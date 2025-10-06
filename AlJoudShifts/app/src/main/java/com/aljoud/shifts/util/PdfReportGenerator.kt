package com.aljoud.shifts.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.aljoud.shifts.data.dao.ShiftWithNames
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object PdfReportGenerator {

    private val arLocale = Locale("ar")
    private val dayForBranchFmt = DateTimeFormatter.ofPattern("EEEE dd.MM", arLocale)

    // سيتم تحميل الخطوط من assets
    private fun arabicFont(context: Context, bold: Boolean = false): Font {
        val fontName = if (bold) "NotoNaskhArabic-Bold.ttf" else "NotoNaskhArabic-Regular.ttf"
        val base = BaseFont.createFont("assets/fonts/$fontName", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        return Font(base, if (bold) 14f else 12f, if (bold) Font.BOLD else Font.NORMAL, BaseColor.BLACK)
    }

    fun exportMonthlyBranch(
        context: Context,
        branchName: String,
        month: YearMonth,
        data: List<ShiftWithNames>
    ) {
        val fileName = "Branch_${sanitize(branchName)}_${month}.pdf"
        writePdfToDownloads(context, fileName) { os ->

            // نستخدم صفحة أفقية لعرض أفضل
            val doc = Document(PageSize.A4.rotate(), 36f, 36f, 36f, 36f)
            PdfWriter.getInstance(doc, os)
            doc.open()

            val titleFont = arabicFont(context, true)
            val headerFont = arabicFont(context, true)
            val cellFont = arabicFont(context, false)

            val title = Paragraph("تقرير شفتات فرع $branchName – ${month}", titleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 16f
            doc.add(title)

            // الأعمدة: التاريخ | التوقيت | الموظفون
            val table = PdfPTable(floatArrayOf(2f, 2f, 5f)).apply {
                runDirection = PdfWriter.RUN_DIRECTION_RTL // الاتجاه من اليمين لليسار
                widthPercentage = 100f
                spacingBefore = 8f
                spacingAfter = 8f
            }

            table.addCell(headerCell("التاريخ", headerFont))
            table.addCell(headerCell("التوقيت", headerFont))
            table.addCell(headerCell("الموظفون", headerFont))

            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()
            val monthShifts = data.filter { it.date in monthStart..monthEnd }

            val grouped = monthShifts
                .groupBy { Triple(it.date, it.start, it.end) }
                .toSortedMap(compareBy<Triple<LocalDate, java.time.LocalTime, java.time.LocalTime>>({ it.first }, { it.second }))

            if (grouped.isEmpty()) {
                val empty = PdfPCell(Phrase("لا توجد شفتات في هذا الشهر", cellFont)).apply {
                    horizontalAlignment = Element.ALIGN_CENTER
                    verticalAlignment = Element.ALIGN_MIDDLE
                    colspan = 3
                    setPadding(10f)
                }
                table.addCell(empty)
            } else {
                grouped.forEach { (k, list) ->
                    val (date, start, end) = k
                    val names = list.map { it.employeeName }.distinct().joinToString("، ")

                    table.addCell(bodyCell(date.format(dayForBranchFmt), cellFont))
                    table.addCell(bodyCell("${start} – ${end}", cellFont))
                    table.addCell(bodyCell(names, cellFont, alignLeft = true))
                }
            }

            doc.add(table)
            doc.close()
        }
    }

    private fun headerCell(text: String, font: Font): PdfPCell =
        PdfPCell(Phrase(text, font)).apply {
            horizontalAlignment = Element.ALIGN_CENTER
            verticalAlignment = Element.ALIGN_MIDDLE
            backgroundColor = BaseColor(255, 215, 0)
            runDirection = PdfWriter.RUN_DIRECTION_RTL
            setPadding(8f)
        }

    private fun bodyCell(text: String, font: Font, alignLeft: Boolean = false): PdfPCell =
        PdfPCell(Phrase(text, font)).apply {
            horizontalAlignment = if (alignLeft) Element.ALIGN_LEFT else Element.ALIGN_CENTER
            verticalAlignment = Element.ALIGN_MIDDLE
            runDirection = PdfWriter.RUN_DIRECTION_RTL
            setPadding(6f)
        }

    private fun sanitize(s: String): String =
        s.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")

    private inline fun writePdfToDownloads(
        context: Context,
        displayName: String,
        writeBlock: (OutputStream) -> Unit
    ) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AlJoudShifts")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("فشل إنشاء الملف عبر MediaStore")

        resolver.openOutputStream(uri)?.use(writeBlock)
            ?: throw IllegalStateException("فشل فتح OutputStream للـ PDF")
    }
}
