package pl.oki.frostalert.data.repository

import pl.oki.frostalert.data.local.TemperatureDao
import java.util.Calendar
import java.util.Locale

data class MonthlyStat(
    val monthName: String,
    val riskDays: Int,
    val averageMinTemp: Double
)

class HistoryRepository(private val dao: TemperatureDao) {

    suspend fun getSeasonStats(): List<MonthlyStat> {
        val allRecords = dao.getAllRecords()
        if (allRecords.isEmpty()) return emptyList()

        return allRecords
            .groupBy { 
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.MONTH)
            }
            .map { (month, records) ->
                val monthName = Calendar.getInstance().apply { set(Calendar.MONTH, month) }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Błąd"
                val riskDays = records.count { it.hasRisk }
                val avgTemp = records.map { it.minTemp }.average()
                MonthlyStat(monthName, riskDays, avgTemp)
            }
            .sortedBy { 
                val cal = Calendar.getInstance()
                val month = cal.get(Calendar.MONTH)
                (it.monthName.let { name -> 
                    val monthMap = (0..11).associateBy { cal.apply { set(Calendar.MONTH, it) }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) }
                    monthMap[name]
                } ?: month) - month + (if (month < 6) 12 else 0) % 12
            }
    }

    suspend fun getAbsoluteMinTemp(): Double? {
        return dao.getAbsoluteMinTemp()
    }
}
