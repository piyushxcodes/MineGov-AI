package com.minegov.ai.network

import com.minegov.ai.network.model.ViolationRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("api/v1/violations")
    suspend fun createViolation(
        @Body request: ViolationRequest
    ): Response<CreateViolationResponse>

    @Multipart
    @POST("api/v1/violations/{violationId}/vision-analyze")
    suspend fun analyzeViolationVision(
        @Path("violationId") violationId: String,
        @Part file: MultipartBody.Part
    ): Response<VisionAnalysisResponse>
}

data class CreateViolationResponse(
    val id: String?,
    val message: String?
)

data class VisionAnalysisResponse(
    val success: Boolean,
    val violationId: String,
    val vision: VisionResult,
    val combinedRisk: CombinedRisk,
    val finding: String?
)

data class VisionResult(
    val riskScore: Int,
    val riskLevel: String,
    val violations: List<VisionViolation>,
    val detections: List<VisionDetection>
)

data class VisionViolation(
    val type: String,
    val confidence: Double
)

data class VisionDetection(
    val `class`: String,
    val confidence: Double,
    val box: List<Double>
)

data class CombinedRisk(
    val score: Int?,
    val level: String?
)