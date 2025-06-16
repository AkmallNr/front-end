package com.example.schedo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.schedo.R

class ScheduleReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME") ?: "Jadwal"
        val channelId = "schedule_reminder_channel"
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Ganti dengan icon notifikasi Anda
            .setContentTitle("Pengingat Jadwal")
            .setContentText("Waktu untuk $scheduleName!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }
    }
}