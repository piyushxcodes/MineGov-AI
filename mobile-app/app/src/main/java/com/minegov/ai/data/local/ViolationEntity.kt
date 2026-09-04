package com.minegov.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "violations")
data class ViolationEntity(
    @PrimaryKey
    val localId: String = UUID.randomUUID().toString(),
    val violationType: String,
    val description: String,
    val observedCondition: String,
    val severity: String,
    val latitude: Double?,
    val longitude: Double?,
    val photoUri: String?,
    val videoUri: String?,
    val voiceUri: String?,
    val documentUri: String? = null,
    val ocrText: String? = null,
    val syncStatus: String = "PENDING",
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null
)
