package com.example.bmupds

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

const val PREFS_NAME = "bmu_salary_prefs"
const val KEY_EVENT_IDS = "bmu_event_ids"
const val KEY_SUBMITTED_MONTH = "bmu_submitted_month"

object CalendarReminderHelper {

    fun findWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        ) ?: return null

        cursor.use {
            var defaultCalendarId: Long? = null
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val accessLevel = it.getInt(1)
                if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    return id
                }
                if (defaultCalendarId == null) {
                    defaultCalendarId = id
                }
            }
            return defaultCalendarId
        }
    }

    fun removeAllRemindersForMonth(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val eventIdsStr = prefs.getString(KEY_EVENT_IDS, null) ?: return
        val ids = eventIdsStr.split(",").mapNotNull { it.toLongOrNull() }

        for (id in ids) {
            try {
                val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon().appendPath(id.toString()).build()
                context.contentResolver.delete(deleteUri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val now = Calendar.getInstance()
        val currentMonthKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH)}"
        prefs.edit()
            .remove(KEY_EVENT_IDS)
            .putString(KEY_SUBMITTED_MONTH, currentMonthKey)
            .apply()
    }

    fun submitAndScheduleNext(context: Context) {
        // 1. Delete all current month's calendar events
        removeAllRemindersForMonth(context)

        // 2. Schedule reminders for next month starting from the 2nd
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val calendarId = findWritableCalendarId(context) ?: return
        val eventIds = mutableListOf<Long>()

        for (day in 2..28 step 2) {
            val target = Calendar.getInstance(TimeZone.getDefault()).apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            try {
                val cv = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, "💼 BMU Salary Bill Reminder")
                    put(CalendarContract.Events.DESCRIPTION, "Submit your monthly salary bill via the BMU PDS portal.")
                    put(CalendarContract.Events.DTSTART, target.timeInMillis)
                    put(CalendarContract.Events.DTEND, target.timeInMillis + 30 * 60 * 1000L)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }
                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, cv) ?: continue
                val eventId = uri.lastPathSegment?.toLongOrNull() ?: continue

                val reminder = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    put(CalendarContract.Reminders.MINUTES, 10)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
                eventIds.add(eventId)
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Store next month's event IDs and clear submitted flag so next month works normally
        val next = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
        prefs.edit()
            .putString(KEY_EVENT_IDS, eventIds.joinToString(","))
            .remove(KEY_SUBMITTED_MONTH)
            .apply()
    }

    fun addSalaryReminders(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = Calendar.getInstance()
        val currentMonthKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH)}"
        if (prefs.getString(KEY_SUBMITTED_MONTH, "") == currentMonthKey) return
        if (!prefs.getString(KEY_EVENT_IDS, "").isNullOrEmpty()) return
        val calendarId = findWritableCalendarId(context) ?: return
        val eventIds = mutableListOf<Long>()
        for (day in 2..28 step 2) {
            val target = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) continue
            try {
                val cv = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, "💼 BMU Salary Bill Reminder")
                    put(CalendarContract.Events.DESCRIPTION, "Submit your monthly salary bill via the BMU PDS portal.")
                    put(CalendarContract.Events.DTSTART, target.timeInMillis)
                    put(CalendarContract.Events.DTEND, target.timeInMillis + 30 * 60 * 1000L)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }
                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, cv) ?: continue
                val eventId = uri.lastPathSegment?.toLongOrNull() ?: continue
                val reminder = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    put(CalendarContract.Reminders.MINUTES, 10)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
                eventIds.add(eventId)
            } catch (e: Exception) { e.printStackTrace() }
        }
        prefs.edit().putString(KEY_EVENT_IDS, eventIds.joinToString(",")).apply()
    }

    fun muteForMonth(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getString(KEY_EVENT_IDS, "") ?: ""
        ids.split(",").filter { it.isNotBlank() }.forEach { idStr ->
            try {
                val eventId = idStr.toLong()
                val deleteUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI, eventId)
                context.contentResolver.delete(deleteUri, null, null)
                context.contentResolver.delete(CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events._ID} = ?", arrayOf(eventId.toString()))
            } catch (e: Exception) { e.printStackTrace() }
        }
        val now = Calendar.getInstance()
        val currentMonthKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH)}"
        prefs.edit().remove(KEY_EVENT_IDS).putString(KEY_SUBMITTED_MONTH, currentMonthKey).apply()
    }

    fun unmuteForMonth(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SUBMITTED_MONTH).remove(KEY_EVENT_IDS).apply()
        addSalaryReminders(context)
    }

    fun isMutedForMonth(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = Calendar.getInstance()
        val currentMonthKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH)}"
        return prefs.getString(KEY_SUBMITTED_MONTH, "") == currentMonthKey
    }

    fun resetIfNewMonth(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val submittedMonth = prefs.getString(KEY_SUBMITTED_MONTH, "") ?: ""
        if (submittedMonth.isEmpty()) return
        val now = Calendar.getInstance()
        val currentMonthKey = "${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH)}"
        if (submittedMonth != currentMonthKey) {
            prefs.edit().remove(KEY_SUBMITTED_MONTH).remove(KEY_EVENT_IDS).apply()
        }
    }
}
