package pl.oki.frostalert.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import pl.oki.frostalert.data.local.TemperatureRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {

    fun generateSeasonReport(
        context: Context,
        records: List<TemperatureRecord>,
        seasonLabel: String
    ): File? {
        if (records.isEmpty()) return null

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 18f
            isFakeBoldText = true
        }

        var y = 60f

        // Title
        canvas.drawText("FrostAlert — Raport Sezonowy", 40f, y, titlePaint)
        y += 30f
        canvas.drawText(seasonLabel, 40f, y, bodyPaint)
        y += 40f

        // Stats
        val frostNights = records.count { it.hasRisk }
        val avgMin = records.map { it.minTemp }.average()
        val absoluteMin = records.minOf { it.minTemp }
        val totalNights = records.size

        canvas.drawText("Podsumowanie", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Łączna liczba nocy: $totalNights", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Noce z ryzykiem szronu: $frostNights", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText(
            "Średnie minimum: ${String.format(Locale.US, "%.1f°C", avgMin)}",
            40f, y, bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Absolutne minimum: ${String.format(Locale.US, "%.1f°C", absoluteMin)}",
            40f, y, bodyPaint
        )
        y += 40f

        // Table header
        canvas.drawText("Szczegóły rekordów", 40f, y, headerPaint)
        y += 25f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val tableHeaderPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText("Data", 40f, y, tableHeaderPaint)
        canvas.drawText("Min °C", 200f, y, tableHeaderPaint)
        canvas.drawText("Ryzyko", 300f, y, tableHeaderPaint)
        canvas.drawText("Prawdop.", 400f, y, tableHeaderPaint)
        y += 18f

        // Draw line
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(40f, y, 550f, y, linePaint)
        y += 10f

        val recordsToShow = records.sortedByDescending { it.timestamp }.take(40)
        for (record in recordsToShow) {
            if (y > 780f) break // Avoid overflow
            canvas.drawText(dateFormat.format(Date(record.timestamp)), 40f, y, bodyPaint)
            canvas.drawText(
                String.format(Locale.US, "%.1f", record.minTemp),
                200f, y, bodyPaint
            )
            canvas.drawText(if (record.hasRisk) "TAK" else "NIE", 300f, y, bodyPaint)
            canvas.drawText("${record.frostProbability}%", 400f, y, bodyPaint)
            y += 16f
        }

        // Footer
        y = 820f
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 10f }
        canvas.drawText(
            "Wygenerowano przez FrostAlert • ${dateFormat.format(Date())}",
            40f, y, footerPaint
        )

        document.finishPage(page)

        val file = File(context.cacheDir, "frost_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    fun shareReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Udostępnij raport"))
    }
}
