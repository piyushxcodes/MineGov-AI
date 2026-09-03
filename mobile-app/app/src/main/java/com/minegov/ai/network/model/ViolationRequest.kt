package com.minegov.ai.network.model

data class ViolationRequest(
    val localId: String,

    val violationType: String,

    val description: String,

    val observedCondition: String,

    val severity: String,

    val latitude: Double?,

    val longitude: Double?,

    val photoUri: String?,

    val videoUri: String?,

    val voiceUri: String?,

    val createdAt: Long
)