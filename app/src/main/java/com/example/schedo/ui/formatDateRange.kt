package com.example.schedo.ui

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.*

fun formatDateRange(startDate: String?, endDate: String?): String {
    if (startDate == null || endDate == null) {
        return "Start date: N/A\nEnd date: N/A"
    }

    return try {
        val dateFormat = when {
            startDate.contains("T") && startDate.contains(".") ->
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            startDate.contains("T") && !startDate.contains(".") ->
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            startDate.contains(" ") ->
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            else ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }

        if (startDate.endsWith("Z")) {
            dateFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        val startDateTime = dateFormat.parse(startDate)
        val endDateTime = dateFormat.parse(endDate)

        val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

        val formattedStart = outputFormat.format(startDateTime)
        val formattedEnd = outputFormat.format(endDateTime)

        "Start Date : $formattedStart\nEnd Date   : $formattedEnd"
    } catch (e: Exception) {
        Log.e("DateFormat", "Format failed: ${e.message}")
        "Start date: ${startDate.replace("T", " ").substringBefore(".")}\n" +
                "End date: ${endDate.replace("T", " ").substringBefore(".")}"
    }
}
