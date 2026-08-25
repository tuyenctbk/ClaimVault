package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.WarrantyItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyExpiryCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleNextDailyCheck(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAllExpiries(context)
            } finally {
                pendingResult.finish()
                scheduleNextDailyCheck(context)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 9988
        private const val DAILY_CHECK_CHANNEL_ID = "daily_expiry_check_channel"

        fun scheduleNextDailyCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyExpiryCheckReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                // Set checkup time to 9:00 AM tomorrow
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: Exception) {
                // Fail-safe for system/OS background execution limits
            }
        }

        suspend fun checkAllExpiries(context: Context) {
            val db = AppDatabase.getDatabase(context)
            val warranties = db.warrantyDao().getAllWarranties().first()
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            for (item in warranties) {
                if (item.warrantyDurationMonths == -1) continue // Lifetime warranties do not expire

                val daysLeft = ((item.expiryDateEpoch - now) / dayMs).toInt()

                val (title, msg) = when (daysLeft) {
                    30 -> Pair("Warranty Expiring in 30 Days", "${item.title} warranty expires in 1 month. Plan a pre-expiry inspection!")
                    7 -> Pair("⚠️ Warranty Expiring in 7 Days", "${item.title} coverage ends in 1 week. Check for defects or file a claim now.")
                    1 -> Pair("🚨 LAST DAY: Warranty Expires Tomorrow!", "Final chance to file a claim for ${item.title} before coverage ends.")
                    0 -> Pair("🚨 Warranty Expires TODAY!", "${item.title} warranty expires today! File a claim immediately if needed.")
                    else -> continue
                }

                showExpiryAlert(context, title, msg, item.id)
            }
        }

        private fun showExpiryAlert(context: Context, title: String, message: String, itemId: Long) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    DAILY_CHECK_CHANNEL_ID,
                    "Daily Expiry Checkups",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily background scanning to alert you before warranties expire."
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_item_id", itemId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                itemId.toInt() + 100000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, DAILY_CHECK_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(itemId.toInt() + 100000, notification)
        }
    }
}
