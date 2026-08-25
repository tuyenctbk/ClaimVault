package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager task that checks saved warranty records in the Room database
 * and triggers high-priority local notifications 30 days (and 7/1 days) before expiry.
 */
class WarrantyExpiryWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val warranties = db.warrantyDao().getAllWarranties().first()
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            for (item in warranties) {
                if (item.warrantyDurationMonths == -1) continue // Lifetime warranty

                val daysLeft = ((item.expiryDateEpoch - now) / dayMs).toInt()

                val (title, msg) = when {
                    daysLeft in 29..31 && item.reminder30Days -> {
                        Pair("Warranty Expiring in 30 Days", "${item.title} (${item.merchant}) warranty expires in 1 month. Time for a pre-expiry inspection!")
                    }
                    daysLeft in 6..7 && item.reminder7Days -> {
                        Pair("⚠️ Warranty Expiring in 7 Days", "${item.title} coverage ends in 1 week. Check for defects or file a claim now.")
                    }
                    daysLeft in 0..1 && item.reminder1Day -> {
                        Pair("🚨 LAST CHANCE: Warranty Expires Soon!", "Final chance to file a claim for ${item.title} before coverage ends.")
                    }
                    else -> continue
                }

                showExpiryNotification(appContext, title, msg, item.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showExpiryNotification(context: Context, title: String, message: String, itemId: Long) {
        val channelId = "workmanager_expiry_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "30-Day Warranty Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends background WorkManager notifications 30 days before stored warranties expire."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_item_id", itemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (itemId + 300000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((itemId + 300000).toInt(), notification)
    }

    companion object {
        const val WORK_NAME = "warranty_30_day_expiry_check_worker"

        fun enqueuePeriodicExpiryCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WarrantyExpiryWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
