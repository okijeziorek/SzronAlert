package pl.oki.frostalert.data.repository

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.AppError
import java.util.*

class CalendarRepository(private val context: Context) {

    fun addFrostRiskEvent(
        date: Long,
        riskLevel: Int,
        minTemp: Double
    ): AppResult<Boolean> {
        return try {
            val calendarId = getDefaultCalendarId() ?: return AppResult.Error(
                AppError.ValidationError("Brak dostępnego kalendarza")
            )
            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            calendar.set(Calendar.HOUR_OF_DAY, 20)
            calendar.set(Calendar.MINUTE, 0)
            val startMillis = calendar.timeInMillis
            calendar.add(Calendar.HOUR_OF_DAY, 12)
            val endMillis = calendar.timeInMillis

            val eventColor = when {
                riskLevel >= 80 -> 0xFFD32F2F.toInt()
                riskLevel >= 60 -> 0xFFFF9800.toInt()
                riskLevel >= 40 -> 0xFFFFEB3B.toInt()
                else -> 0xFF4CAF50.toInt()
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "🧊 Ryzyko szronu ($riskLevel%)")
                put(CalendarContract.Events.DESCRIPTION, "FrostAlert: Prognozowane minimum ${String.format(Locale.US, "%.1f", minTemp)}°C. Ryzyko: $riskLevel%")
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.EVENT_COLOR, eventColor)
            }

            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            AppResult.Success(true)
        } catch (e: SecurityException) {
            AppResult.Error(AppError.LocationPermissionError("Brak uprawnień do kalendarza"))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Błąd zapisu do kalendarza: ${e.message}"))
        }
    }

    private fun getDefaultCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        ) ?: return null

        var calendarId: Long? = null
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val isPrimary = it.getInt(1) == 1
                if (isPrimary) return id
                if (calendarId == null) calendarId = id
            }
        }
        return calendarId
    }
}
