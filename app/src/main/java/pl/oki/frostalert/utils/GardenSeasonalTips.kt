package pl.oki.frostalert.utils

import android.content.Context
import pl.oki.frostalert.R
import java.text.DateFormatSymbols
import java.util.Calendar

object GardenSeasonalTips {

    data class MonthlyTip(
        val emoji: String,
        val tip: String
    )

    data class MonthData(
        val tips: List<MonthlyTip>,
        val monthName: String
    )

    /**
     * Get localized tips for the given month.
     * @param context Android context for string resource access
     * @param month Calendar month (0-11), defaults to current month
     */
    fun getTipsForMonth(context: Context, month: Int = Calendar.getInstance().get(Calendar.MONTH)): List<MonthlyTip> {
        return when (month) {
            Calendar.JANUARY -> listOf(
                MonthlyTip("❄️", context.getString(R.string.tip_january_1)),
                MonthlyTip("📋", context.getString(R.string.tip_january_2)),
                MonthlyTip("✂️", context.getString(R.string.tip_january_3))
            )
            Calendar.FEBRUARY -> listOf(
                MonthlyTip("🌱", context.getString(R.string.tip_february_1)),
                MonthlyTip("✂️", context.getString(R.string.tip_february_2)),
                MonthlyTip("🐛", context.getString(R.string.tip_february_3))
            )
            Calendar.MARCH -> listOf(
                MonthlyTip("🌷", context.getString(R.string.tip_march_1)),
                MonthlyTip("🌱", context.getString(R.string.tip_march_2)),
                MonthlyTip("💧", context.getString(R.string.tip_march_3))
            )
            Calendar.APRIL -> listOf(
                MonthlyTip("⚠️", context.getString(R.string.tip_april_1)),
                MonthlyTip("🍅", context.getString(R.string.tip_april_2)),
                MonthlyTip("🌿", context.getString(R.string.tip_april_3))
            )
            Calendar.MAY -> listOf(
                MonthlyTip("🌡️", context.getString(R.string.tip_may_1)),
                MonthlyTip("💧", context.getString(R.string.tip_may_2)),
                MonthlyTip("🌹", context.getString(R.string.tip_may_3))
            )
            Calendar.JUNE -> listOf(
                MonthlyTip("☀️", context.getString(R.string.tip_june_1)),
                MonthlyTip("💧", context.getString(R.string.tip_june_2)),
                MonthlyTip("🍓", context.getString(R.string.tip_june_3))
            )
            Calendar.JULY -> listOf(
                MonthlyTip("🌡️", context.getString(R.string.tip_july_1)),
                MonthlyTip("💧", context.getString(R.string.tip_july_2)),
                MonthlyTip("🌿", context.getString(R.string.tip_july_3))
            )
            Calendar.AUGUST -> listOf(
                MonthlyTip("🥦", context.getString(R.string.tip_august_1)),
                MonthlyTip("🍎", context.getString(R.string.tip_august_2)),
                MonthlyTip("💧", context.getString(R.string.tip_august_3))
            )
            Calendar.SEPTEMBER -> listOf(
                MonthlyTip("🌾", context.getString(R.string.tip_september_1)),
                MonthlyTip("🧅", context.getString(R.string.tip_september_2)),
                MonthlyTip("❄️", context.getString(R.string.tip_september_3))
            )
            Calendar.OCTOBER -> listOf(
                MonthlyTip("🌹", context.getString(R.string.tip_october_1)),
                MonthlyTip("🥕", context.getString(R.string.tip_october_2)),
                MonthlyTip("🍂", context.getString(R.string.tip_october_3))
            )
            Calendar.NOVEMBER -> listOf(
                MonthlyTip("❄️", context.getString(R.string.tip_november_1)),
                MonthlyTip("🪴", context.getString(R.string.tip_november_2)),
                MonthlyTip("📋", context.getString(R.string.tip_november_3))
            )
            Calendar.DECEMBER -> listOf(
                MonthlyTip("🌟", context.getString(R.string.tip_december_1)),
                MonthlyTip("📚", context.getString(R.string.tip_december_2)),
                MonthlyTip("🐦", context.getString(R.string.tip_december_3))
            )
            else -> emptyList()
        }
    }

    /**
     * Returns tips and the locale-aware month name in a single call,
     * using one Calendar instance to avoid clock-skew around midnight.
     */
    fun getCurrentMonthData(context: Context): MonthData {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val tipsList = getTipsForMonth(context, month)
        val monthName = DateFormatSymbols.getInstance().months[month]
            .replaceFirstChar { it.uppercaseChar() }
        return MonthData(tips = tipsList, monthName = monthName)
    }
}

