package com.example.schedo.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.schedo.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Pengingat"
        val message = intent.getStringExtra("message") ?: "Tugas perlu dikerjakan!"
        val quote = intent.getStringExtra("quote") ?: "Tidak ada quote hari ini"
        val notificationManager = NotificationManagerCompat.from(context)

        val notificationMessage = "$message\n\nQuote hari ini:\n\"$quote\""
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ganti dengan ic_notification jika sudah ada
            .setContentTitle(title)
            .setContentText(notificationMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage)) // Untuk teks panjang
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000)) // Vibrasi untuk perhatian
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Suara default
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Izin notifikasi diperlukan", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            notificationManager.notify(1, notification)
            android.util.Log.d("AlarmReceiver", "Notifikasi dikirim: $title dengan quote: $quote")
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengirim notifikasi: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("AlarmReceiver", "Error: ${e.message}", e)
        }
    }
}