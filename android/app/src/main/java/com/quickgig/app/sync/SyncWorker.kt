package com.quickgig.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quickgig.app.QuickGigApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * User Defined Feature 2 (offline mode with sync), the "sync automatically
 * when reconnected" half. WorkManager's own NetworkType.CONNECTED
 * constraint is what actually detects reconnection; this worker just
 * drains whatever is in the draft-gig and message outbox once it's
 * allowed to run.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as QuickGigApplication

        val drafts = app.gigRepository.observePendingDrafts().first()
        var allOk = true
        for (draft in drafts) {
            val ok = app.gigRepository.trySyncDraft(draft)
            if (!ok) allOk = false
        }

        val outboxMessages = app.database.messageDao().pendingOutbox()
        for (message in outboxMessages) {
            val ok = app.messageRepository.trySyncOutbox(message)
            if (ok) app.database.messageDao().removeFromOutbox(message) else allOk = false
        }

        return if (allOk) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "quickgig_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
