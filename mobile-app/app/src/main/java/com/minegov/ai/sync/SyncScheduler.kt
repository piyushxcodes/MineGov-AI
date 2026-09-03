package com.minegov.ai.sync

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object SyncScheduler {

    private const val SYNC_WORK_NAME = "violation_sync"

    fun scheduleSync(context: Context) {

        Log.d(
            "SyncScheduler",
            "Scheduling violation sync"
        )

        val syncRequest =
            OneTimeWorkRequestBuilder<ViolationSyncWorker>()
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

        Log.d(
            "SyncScheduler",
            "Violation sync work enqueued"
        )
    }
}