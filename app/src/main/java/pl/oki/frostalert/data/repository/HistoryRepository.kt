package pl.oki.frostalert.data.repository

import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.utils.AppError
import pl.oki.frostalert.utils.AppResult
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class MonthlyStat(
    val monthNumber: Int,
    val monthName: String,
    val riskDays: Int,
    val averageMinTemp: Double
)

class HistoryRepository @Inject constructor(private val dao: TemperatureDao) {

    suspend fun getSeasonStats(): AppResult<List<MonthlyStat>> {
        return try {
            val allRecords = dao.getAllRecords()
            if (allRecords.isEmpty()) return AppResult.Success(emptyList())

            val stats = allRecords
                .groupBy { 
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.MONTH)
                }
                .map { (month, records) ->
                    val monthName = Calendar.getInstance().apply { set(Calendar.MONTH, month) }
                        .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Nieznany"
                    val riskDays = records.count { it.hasRisk }
                    val avgTemp = records.map { it.minTemp }.average()
                    MonthlyStat(month, monthName, riskDays, avgTemp)
                }
                .sortedBy { 
                    val cal = Calendar.getInstance()
                    val currentMonth = cal.get(Calendar.MONTH)
                    // Sortowanie od jesieni do wiosny (sezon zimowy)
                    (it.monthNumber - currentMonth + 12) % 12 
                }
            AppResult.Success(stats)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Błąd odczytu statystyk: ${e.message}", e))
        }
    }

    suspend fun getAbsoluteMinTemp(): AppResult<Double?> {
        return try {
            AppResult.Success(dao.getAbsoluteMinTemp())
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Błąd odczytu temperatury minimalnej", e))
        }
    }

    suspend fun getRecordsForDateOneYearAgo(): AppResult<List<TemperatureRecord>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -1)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 2) // ±1 day window
            val end = calendar.timeInMillis
            AppResult.Success(dao.getRecordsBetween(start, end))
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError("Błąd odczytu danych historycznych: ${e.message}", e))
        }
    }
}
