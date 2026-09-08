package com.aigallery.app.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val detailDateFormat = SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

    fun formatGroupTitle(timestamp: Long): String {
        if (timestamp <= 0L) return "Earlier"

        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        // Check if Today
        if (targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "TODAY"
        }

        // Check if Yesterday
        val yesterdayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        if (targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "YESTERDAY"
        }

        // Otherwise return Month & Year in uppercase
        return monthYearFormat.format(Date(timestamp)).uppercase(Locale.getDefault())
    }

    fun formatDetailDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return detailDateFormat.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return fullDateFormat.format(Date(timestamp))
    }
}
