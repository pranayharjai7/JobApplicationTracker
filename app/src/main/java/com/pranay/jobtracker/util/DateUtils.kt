package com.pranay.jobtracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {

    private val shortFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val fullFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    /**
     * Formats an epoch timestamp to a relative string (e.g., "Today", "Yesterday")
     * or a short absolute date if older.
     */
    fun formatRelative(epochMs: Long): String {
        if (epochMs == 0L) return "Recent"
        
        val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            ChronoUnit.DAYS.between(date, today) < 7 -> {
                // Return day of week if within last 7 days
                date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
            }
            else -> date.format(shortFormatter)
        }
    }

    /**
     * Formats an epoch timestamp to "MMM d" (e.g., "Apr 21").
     */
    fun formatShort(epochMs: Long): String {
        if (epochMs == 0L) return "Recent"
        val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(shortFormatter)
    }

    /**
     * Formats an epoch timestamp to "MMM d, yyyy" (e.g., "Apr 21, 2024").
     */
    fun formatFull(epochMs: Long): String {
        if (epochMs == 0L) return "Recent"
        val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(fullFormatter)
    }
}
