package com.minegov.ai.sync

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
import java.io.File

class ViolationSyncWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

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
            return Result.success()
        }

        return try {

            for (violation in pendingViolations) {

                Log.d(
                    "ViolationSyncWorker",
                    "Syncing violation: ${violation.localId}"
                )

                // ---------------------------------------------------------
                // 1. CREATE VIOLATION
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
                    documentUri = violation.documentUri,
                    ocrText = violation.ocrText,
                    createdAt = violation.createdAt
                )

                val response =
                    ApiClient.apiService.createViolation(request)

                if (!response.isSuccessful) {

                    dao.update(
                        violation.copy(
                            syncStatus = "FAILED",
                            syncAttempts = violation.syncAttempts + 1,
                            updatedAt = System.currentTimeMillis(),
                            lastSyncError = "HTTP ${response.code()}"
                        )
                    )

                    return Result.retry()
                }

                val serverId =
                    response.body()?.id
                        ?: throw Exception(
                            "Server returned no violation ID"
                        )

                Log.d(
                    "ViolationSyncWorker",
                    "Violation created: $serverId"
                )

                // ---------------------------------------------------------
                // 2. VOICE → WHISPER
                // ---------------------------------------------------------

                var voiceError: String? = null

                if (!violation.voiceUri.isNullOrBlank()) {

                    try {

                        Log.d(
                            "ViolationSyncWorker",
                            "Uploading voice for Whisper..."
                        )

                        val voicePath = violation.voiceUri

                        // VoiceNoteScreen stores a real local filesystem path.
                        // Therefore ContentResolver cannot read it.
                        val audioFile = File(voicePath)

                        if (!audioFile.exists()) {
                            throw Exception(
                                "Voice file not found: $voicePath"
                            )
                        }

                        val audioBytes = audioFile.readBytes()

                        if (audioBytes.isEmpty()) {
                            throw Exception(
                                "Voice file is empty"
                            )
                        }

                        Log.d(
                            "ViolationSyncWorker",
                            "Voice file size: ${audioBytes.size} bytes"
                        )

                        val mimeType = "audio/mp4"

                        val body =
                            audioBytes.toRequestBody(
                                mimeType.toMediaType()
                            )

                        val filePart =
                            MultipartBody.Part.createFormData(
                                name = "file",
                                filename =
                                    "voice_${violation.localId}.m4a",
                                body = body
                            )

                        val voiceResponse =
                            ApiClient.apiService
                                .transcribeViolationVoice(
                                    violationId = serverId,
                                    file = filePart
                                )

                        if (voiceResponse.isSuccessful) {

                            val result =
                                voiceResponse.body()

                            Log.d(
                                "ViolationSyncWorker",
                                "WHISPER SUCCESS"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Language: ${result?.language}"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Transcript: ${result?.transcript}"
                            )

                            Log.d(
                                "ViolationSyncWorker",
                                "Probability: ${result?.languageProbability}"
                            )

                        } else {

                            voiceError =
                                "Voice HTTP ${voiceResponse.code()}"

                            Log.e(
                                "ViolationSyncWorker",
                                voiceError
                            )
                        }

                    } catch (e: Exception) {

                        voiceError =
                            e.message
                                ?: "Voice transcription error"

                        Log.e(
                            "ViolationSyncWorker",
                            "WHISPER ERROR: $voiceError",
                            e
                        )
                    }
                }

                // ---------------------------------------------------------
                // 3. DOCUMENT UPLOAD
                // ---------------------------------------------------------

                var documentError: String? = null

                if (!violation.documentUri.isNullOrBlank()) {

                    try {

                        val uri =
                            Uri.parse(violation.documentUri)

                        val resolver =
                            applicationContext.contentResolver

                        val documentBytes =
                            resolver.openInputStream(uri)?.use {
                                it.readBytes()
                            }

                        if (
                            documentBytes == null ||
                            documentBytes.isEmpty()
                        ) {
                            throw Exception(
                                "Could not read document from URI"
                            )
                        }

                        val mimeType =
                            resolver.getType(uri)
                                ?: "application/octet-stream"

                        val body =
                            documentBytes.toRequestBody(
                                mimeType.toMediaType()
                            )

                        val part =
                            MultipartBody.Part.createFormData(
                                name = "file",
                                filename =
                                    "document_${violation.localId}",
                                body = body
                            )

                        val documentResponse =
                            ApiClient.apiService.uploadDocument(
                                violationId = serverId,
                                file = part
                            )

                        if (!documentResponse.isSuccessful) {

                            documentError =
                                "Document HTTP ${documentResponse.code()}"
                        }

                    } catch (e: Exception) {

                        documentError =
                            e.message
                                ?: "Document upload error"

                        Log.e(
                            "ViolationSyncWorker",
                            "DOCUMENT UPLOAD ERROR: $documentError",
                            e
                        )
                    }
                }

                // ---------------------------------------------------------
                // 4. PHOTO → YOLO
                // ---------------------------------------------------------

                var visionError: String? = null

                // IMPORTANT DIAGNOSTIC
                Log.d(
                    "ViolationSyncWorker",
                    "PHOTO URI: ${violation.photoUri}"
                )

                if (!violation.photoUri.isNullOrBlank()) {

                    try {

                        Log.d(
                            "ViolationSyncWorker",
                            "PHOTO URI IS PRESENT - starting YOLO upload"
                        )

                        val uri =
                            Uri.parse(violation.photoUri)

                        val resolver =
                            applicationContext.contentResolver

                        Log.d(
                            "ViolationSyncWorker",
                            "Reading photo from URI: $uri"
                        )

                        val imageBytes =
                            resolver.openInputStream(uri)?.use {
                                it.readBytes()
                            }

                        if (
                            imageBytes == null ||
                            imageBytes.isEmpty()
                        ) {
                            throw Exception(
                                "Could not read photo from URI"
                            )
                        }

                        Log.d(
                            "ViolationSyncWorker",
                            "Photo file size: ${imageBytes.size} bytes"
                        )

                        val mimeType =
                            resolver.getType(uri)
                                ?: "image/jpeg"

                        Log.d(
                            "ViolationSyncWorker",
                            "Photo MIME type: $mimeType"
                        )

                        val body =
                            imageBytes.toRequestBody(
                                mimeType.toMediaType()
                            )

                        val filePart =
                            MultipartBody.Part.createFormData(
                                name = "file",
                                filename =
                                    "evidence_${violation.localId}.jpg",
                                body = body
                            )

                        Log.d(
                            "ViolationSyncWorker",
                            "Uploading photo to Vision..."
                        )

                        val visionResponse =
                            ApiClient.apiService
                                .analyzeViolationVision(
                                    violationId = serverId,
                                    file = filePart
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
                                "YOLO ANALYSIS ERROR: $visionError"
                            )
                        }

                    } catch (e: Exception) {

                        visionError =
                            e.message
                                ?: "Vision analysis error"

                        Log.e(
                            "ViolationSyncWorker",
                            "YOLO ANALYSIS ERROR: $visionError",
                            e
                        )
                    }

                } else {

                    Log.e(
                        "ViolationSyncWorker",
                        "PHOTO URI IS NULL OR BLANK - YOLO SKIPPED"
                    )
                }

                // ---------------------------------------------------------
                // 5. FINAL LOCAL SYNC STATUS
                // ---------------------------------------------------------

                val errors =
                    listOfNotNull(
                        voiceError?.let {
                            "Voice: $it"
                        },
                        documentError?.let {
                            "Document: $it"
                        },
                        visionError?.let {
                            "Vision: $it"
                        }
                    )

                dao.update(
                    violation.copy(
                        syncStatus = "SYNCED",
                        serverId = serverId,
                        updatedAt = System.currentTimeMillis(),
                        lastSyncError =
                            errors.joinToString("; ")
                                .ifBlank { null }
                    )
                )

                Log.d(
                    "ViolationSyncWorker",
                    "SYNC SUCCESS: $serverId"
                )
            }

            Result.success()

        } catch (e: Exception) {

            Log.e(
                "ViolationSyncWorker",
                "SYNC ERROR: ${e.message}",
                e
            )

            for (violation in pendingViolations) {

                dao.update(
                    violation.copy(
                        syncStatus = "FAILED",
                        syncAttempts =
                            violation.syncAttempts + 1,
                        updatedAt =
                            System.currentTimeMillis(),
                        lastSyncError =
                            e.message ?: "Network error"
                    )
                )
            }

            Result.retry()
        }
    }
}