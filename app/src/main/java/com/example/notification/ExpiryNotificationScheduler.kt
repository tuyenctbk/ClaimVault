package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.WarrantyItem

object ExpiryNotificationScheduler {

    fun scheduleExpiriesForWarranty(context: Context, item: WarrantyItem) {
        if (item.warrantyDurationMonths == -1) return // Lifetime warranty

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        // Schedule 30 Days Alert
        if (item.reminder30Days) {
            val alertTime30 = item.expiryDateEpoch - (30L * dayMs)
            if (alertTime30 > now) {
                scheduleAlarm(
                    context,
                    alarmManager,
                    item.id.toInt() * 10 + 1,
                    alertTime30,
                    "Warranty Expiring in 30 Days",
                    "${item.title} warranty expires in 1 month. Time for a pre-expiry inspection!",
                    item.id
                )
            }
        }

        // Schedule 7 Days Alert
        if (item.reminder7Days) {
            val alertTime7 = item.expiryDateEpoch - (7L * dayMs)
            if (alertTime7 > now) {
                scheduleAlarm(
                    context,
                    alarmManager,
                    item.id.toInt() * 10 + 2,
                    alertTime7,
                    "⚠️ Warranty Expiring in 7 Days",
                    "${item.title} coverage ends in 1 week. Check for defects or file a claim now.",
                    item.id
                )
            }
        }

        // Schedule 1 Day Alert
        if (item.reminder1Day) {
            val alertTime1 = item.expiryDateEpoch - (1L * dayMs)
            if (alertTime1 > now) {
                scheduleAlarm(
                    context,
                    alarmManager,
                    item.id.toInt() * 10 + 3,
                    alertTime1,
                    "🚨 LAST DAY: Warranty Expires Tomorrow!",
                    "Final chance to file a claim for ${item.title} before coverage ends.",
                    item.id
                )
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerTimeEpoch: Long,
        title: String,
        message: String,
        itemId: Long
    ) {
        val intent = Intent(context, ExpiryNotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("itemId", itemId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeEpoch,
                pendingIntent
            )
        } catch (e: Exception) {
            // Permission or system limitation fallback
        }
    }
}
