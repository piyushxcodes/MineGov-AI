package com.minegov.ai.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minegov.ai.data.local.AppDatabase
import com.minegov.ai.network.ApiClient
import com.minegov.ai.network.model.ViolationRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ViolationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        Log.d("ViolationSyncWorker", "WORKER STARTED")

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.violationDao()

        val pendingViolations = dao.getPendingViolations()

        Log.d(
            "ViolationSyncWorker",
            "Pending violations: ${pendingViolations.size}"
        )

        if (pendingViolations.isEmpty()) {
            Log.d("ViolationSyncWorker", "Nothing to sync")
            return Result.success()
        }

        return try {

            for (violation in pendingViolations) {

                Log.d(
                    "ViolationSyncWorker",
                    "Syncing violation: ${violation.localId}"
                )

                // ---------------------------------------------------------
                // STEP 1: SEND VIOLATION METADATA
                // ---------------------------------------------------------

                val request = ViolationRequest(
                    localId = violation.localId,
                    violationType = violation.violationType,
                    description = violation.description,
                    observedCondition = violation.observedCondition,
                    severity = violation.severity,
                    latitude = violation.latitude,
                    longitude = violation.longitude,
                    photoUri = violation.photoUri,
                    videoUri = violation.videoUri,
                    voiceUri = violation.voiceUri,
                    createdAt = violation.createdAt
                )

                val response =
                    ApiClient.apiService.createViolation(request)

                Log.d(
                    "ViolationSyncWorker",
                    "Server response: ${response.code()}"
                )

                if (!response.isSuccessful) {

                    val failedViolation = violation.copy(
                        syncStatus = "FAILED",
                        syncAttempts = violation.syncAttempts + 1,
                        updatedAt = System.currentTimeMillis(),
                        lastSyncError = "HTTP ${response.code()}"
                    )

                    dao.update(failedViolation)

                    Log.e(
                        "ViolationSyncWorker",
                        "SYNC FAILED: HTTP ${response.code()}"
                    )

                    return Result.retry()
                }

                // ---------------------------------------------------------
                // STEP 2: GET SERVER VIOLATION ID
                // ---------------------------------------------------------

                val serverId = response.body()?.id

                if (serverId.isNullOrBlank()) {

                    Log.e(
                        "ViolationSyncWorker",
                        "Server returned no violation ID"
                    )

                    val failedViolation = violation.copy(
                        syncStatus = "FAILED",
                        syncAttempts = violation.syncAttempts + 1,
                        updatedAt = System.currentTimeMillis(),
                        lastSyncError = "Server returned no violation ID"
                    )

                    dao.update(failedViolation)

                    return Result.retry()
                }

                Log.d(
                    "ViolationSyncWorker",
                    "SERVER VIOLATION ID: $serverId"
                )

                // ---------------------------------------------------------
                // STEP 3: UPLOAD PHOTO TO AI VISION
                // ---------------------------------------------------------

                var visionError: String? = null

                if (!violation.photoUri.isNullOrBlank()) {

                    try {

                        Log.d(
                            "ViolationSyncWorker",
                            "Preparing photo for YOLO: ${violation.photoUri}"
                        )

                        val uri = Uri.parse(violation.photoUri)

                        val resolver = applicationContext.contentResolver

                        val imageBytes = resolver
                            .openInputStream(uri)
                            ?.use { inputStream ->
                                inputStream.readBytes()
                            }

                        if (imageBytes == null || imageBytes.isEmpty()) {

                            throw Exception("Could not read photo from URI")
                        }

                        val mimeType =
                            resolver.getType(uri)
                                ?: "image/jpeg"

                        val requestBody =
                            imageBytes.toRequestBody(
                                mimeType.toMediaType()
                            )

                        val filePart =
                            MultipartBody.Part.createFormData(
                                name = "file",
                                filename = "evidence_${violation.localId}.jpg",
                                body = requestBody
                            )

                        Log.d(
                            "ViolationSyncWorker",
                            "Uploading photo to YOLO..."
                        )

                        val visionResponse =
                            ApiClient.apiService.analyzeViolationVision(
                                violationId = serverId,
                                file = filePart
                            )

                        Log.d(
                            "ViolationSyncWorker",
                            "YOLO response: ${visionResponse.code()}"
                        )

                        if (visionResponse.isSuccessful) {

                            val visionResult =
                                visionResponse.body()

                            Log.d(
                                "ViolationSyncWorker",
                                "YOLO ANALYSIS SUCCESS"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Vision risk: ${visionResult?.vision?.riskLevel}"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Vision score: ${visionResult?.vision?.riskScore}"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Combined risk: ${visionResult?.combinedRisk?.level}"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Finding: ${visionResult?.finding}"
                            )

                        } else {

                            visionError =
                                "Vision HTTP ${visionResponse.code()}"

                            Log.e(
                                "ViolationSyncWorker",
                                "YOLO ANALYSIS FAILED: HTTP ${visionResponse.code()}"
                            )
                        }

                    } catch (e: Exception) {

                        visionError =
                            e.message ?: "Vision analysis error"

                        Log.e(
                            "ViolationSyncWorker",
                            "YOLO ANALYSIS ERROR: ${e.message}",
                            e
                        )
                    }

                } else {

                    Log.d(
                        "ViolationSyncWorker",
                        "No photo attached. Skipping YOLO."
                    )
                }

                // ---------------------------------------------------------
                // STEP 4: MARK LOCAL VIOLATION AS SYNCED
                // ---------------------------------------------------------

                val syncedViolation = violation.copy(
                    syncStatus = "SYNCED",
                    serverId = serverId,
                    updatedAt = System.currentTimeMillis(),
                    lastSyncError = visionError
                )

                dao.update(syncedViolation)

                Log.d(
                    "ViolationSyncWorker",
                    "SYNC SUCCESS: $serverId"
                )

                if (visionError != null) {

                    Log.w(
                        "ViolationSyncWorker",
                        "Violation synced, but YOLO failed: $visionError"
                    )

                } else {

                    Log.d(
                        "ViolationSyncWorker",
                        "Violation + YOLO synced successfully"
                    )
                }
            }

            Log.d(
                "ViolationSyncWorker",
                "WORKER FINISHED SUCCESSFULLY"
            )

            Result.success()

        } catch (e: Exception) {

            Log.e(
                "ViolationSyncWorker",
                "SYNC ERROR: ${e.message}",
                e
            )

            for (violation in pendingViolations) {

                val failedViolation = violation.copy(
                    syncStatus = "FAILED",
                    syncAttempts = violation.syncAttempts + 1,
                    updatedAt = System.currentTimeMillis(),
                    lastSyncError = e.message ?: "Network error"
                )

                dao.update(failedViolation)
            }

            Result.retry()
        }
    }
}