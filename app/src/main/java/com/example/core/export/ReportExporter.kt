package com.example.core.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.math.LandUnitConverter
import com.example.core.math.PartitionPlanResult
import com.example.core.math.SurveyMath
import com.example.core.math.SurveyPoint2D
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ReportExporter {

    private fun exportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun bengaliTypeface(context: Context): Typeface {
        return ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT
    }

    fun exportReportPdf(
        context: Context,
        title: String,
        subtitle: String,
        metadataLines: List<Pair<String, String>>,
        areaSqFt: Double,
        points: List<SurveyPoint2D> = emptyList(),
        partitionPlan: PartitionPlanResult? = null,
        extraLines: List<String> = emptyList(),
        surveyorFooter: String = "হাওলাদার ডিজিটাল সার্ভে",
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val tf = bengaliTypeface(context)

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Header Banner
        val headerPaint = Paint().apply { color = Color.parseColor("#155EEF") }
        canvas.drawRect(0f, 0f, 595f, 76f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(tf, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DBEAFE")
            textSize = 11f
            typeface = tf
        }
        canvas.drawText(title, 28f, 34f, titlePaint)
        canvas.drawText(subtitle, 28f, 54f, subPaint)

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        subPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(dateStr, 567f, 54f, subPaint)
        subPaint.textAlign = Paint.Align.LEFT

        var cursorY = 98f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 10.5f
            typeface = Typeface.create(tf, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = tf
        }
        val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Metadata Grid
        if (metadataLines.isNotEmpty()) {
            val rowHeight = 20f
            val boxH = ((metadataLines.size + 1) / 2) * rowHeight + 14f
            canvas.drawRoundRect(28f, cursorY, 567f, cursorY + boxH, 8f, 8f, boxBorder)
            metadataLines.forEachIndexed { idx, (k, v) ->
                val colX = if (idx % 2 == 0) 40f else 305f
                val rowY = cursorY + 18f + (idx / 2) * rowHeight
                canvas.drawText("$k: ", colX, rowY, labelPaint)
                val keyW = labelPaint.measureText("$k: ")
                canvas.drawText(v.take(32), colX + keyW, rowY, valuePaint)
            }
            cursorY += boxH + 14f
        }

        // Noksha / Partition Diagram Area
        val diagramHeight = 240f
        val diagramBg = Paint().apply { color = Color.parseColor("#F8FAFC") }
        canvas.drawRoundRect(28f, cursorY, 567f, cursorY + diagramHeight, 10f, 10f, diagramBg)
        canvas.drawRoundRect(28f, cursorY, 567f, cursorY + diagramHeight, 10f, 10f, boxBorder)

        if (partitionPlan != null && partitionPlan.plots.isNotEmpty()) {
            drawPartitionOnCanvas(canvas, partitionPlan, 44f, cursorY + 16f, 507f, diagramHeight - 32f, tf)
        } else if (points.size >= 2) {
            drawPolygonOnCanvas(canvas, points, 44f, cursorY + 16f, 507f, diagramHeight - 32f, tf)
        } else {
            valuePaint.textAlign = Paint.Align.CENTER
            canvas.drawText("জমির নকশা প্রিভিউ", 297f, cursorY + diagramHeight / 2f, valuePaint)
            valuePaint.textAlign = Paint.Align.LEFT
        }
        cursorY += diagramHeight + 16f

        // Area Conversion Table
        val summary = LandUnitConverter.summarizeFromSqFeet(areaSqFt)
        val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#155EEF")
            textSize = 12f
            typeface = Typeface.create(tf, Typeface.BOLD)
        }
        canvas.drawText("জমির পরিমাপ ও একক বিবরণ (Area Summary)", 28f, cursorY, sectionTitle)
        cursorY += 10f

        val unitRows = listOf(
            "শতাংশ / ডেসিমেল" to "${LandUnitConverter.format(summary.shotok, 3)} শতাংশ",
            "কাঠা (৭২০ বর্গফুট)" to "${LandUnitConverter.format(summary.katha, 3)} কাঠা",
            "বিঘা (২০ কাঠা)" to "${LandUnitConverter.format(summary.bigha, 4)} বিঘা",
            "একর (১০০ শতাংশ)" to "${LandUnitConverter.format(summary.acre, 4)} একর",
            "বর্গফুট (Sq.Ft)" to "${LandUnitConverter.format(summary.sqFeet, 2)} বর্গফুট",
            "বর্গমিটার (Sq.M)" to "${LandUnitConverter.format(summary.sqMeter, 2)} বর্গমিটার",
            "বর্গগজ (Sq.Yd)" to "${LandUnitConverter.format(summary.sqYard, 2)} বর্গগজ",
            "হেক্টর (Hectare)" to "${LandUnitConverter.format(summary.hectare, 5)} হেক্টর",
        )
        val tableH = ((unitRows.size + 1) / 2) * 19f + 12f
        canvas.drawRoundRect(28f, cursorY, 567f, cursorY + tableH, 8f, 8f, boxBorder)
        unitRows.forEachIndexed { idx, (k, v) ->
            val colX = if (idx % 2 == 0) 40f else 305f
            val rowY = cursorY + 17f + (idx / 2) * 19f
            canvas.drawText("$k: $v", colX, rowY, valuePaint)
        }
        cursorY += tableH + 14f

        // Partition Partner Table or Extra Details
        if (partitionPlan != null && partitionPlan.plots.isNotEmpty()) {
            canvas.drawText("অংশীদারদের প্রাপ্য জমি (Partner Allocation)", 28f, cursorY, sectionTitle)
            cursorY += 14f
            partitionPlan.plots.take(10).forEach { plot ->
                val pShotok = LandUnitConverter.format(plot.areaSqFt / 435.6, 3)
                val pKatha = LandUnitConverter.format(plot.areaSqFt / 720.0, 3)
                val line = "• ${plot.name} (${plot.sideLabel}): $pShotok শতাংশ | $pKatha কাঠা (${LandUnitConverter.format(plot.widthFt, 1)}' × ${LandUnitConverter.format(plot.heightFt, 1)}')"
                canvas.drawText(line, 34f, cursorY, valuePaint)
                cursorY += 16f
            }
        } else if (extraLines.isNotEmpty()) {
            canvas.drawText("অতিরিক্ত তথ্য (Survey Details)", 28f, cursorY, sectionTitle)
            cursorY += 14f
            extraLines.take(10).forEach { line ->
                if (cursorY < 765f) {
                    canvas.drawText(line.take(86), 34f, cursorY, valuePaint)
                    cursorY += 16f
                }
            }
        }

        // Legal GPS Disclaimer + Footer
        val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B45309")
            textSize = 9f
            typeface = tf
        }
        canvas.drawText(
            "সতর্কতা: মোবাইল GPS দ্বারা প্রাপ্ত পরিমাপ প্রাথমিক ধারণা দেয়। আইনগত সীমানার জন্য অনুমোদিত Total Station/DGPS ব্যবহার করুন।",
            28f, 792f, warnPaint
        )
        canvas.drawLine(28f, 804f, 567f, 804f, boxBorder)
        canvas.drawText("প্রস্তুতকারী: $surveyorFooter", 28f, 822f, labelPaint)
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("স্বাক্ষর / সিল: ___________________", 567f, 822f, labelPaint)

        doc.finishPage(page)
        val safeName = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "survey" }
        val file = File(exportsDir(context), "${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun exportNokshaImage(
        context: Context,
        title: String,
        points: List<SurveyPoint2D> = emptyList(),
        partitionPlan: PartitionPlanResult? = null,
        asJpg: Boolean = true,
    ): File {
        val width = 1600
        val height = 1100
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val tf = bengaliTypeface(context)

        canvas.drawColor(Color.parseColor("#F8FBFF"))
        val gridPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        for (gx in 0..width step 80) canvas.drawLine(gx.toFloat(), 0f, gx.toFloat(), height.toFloat(), gridPaint)
        for (gy in 0..height step 80) canvas.drawLine(0f, gy.toFloat(), width.toFloat(), gy.toFloat(), gridPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 40f
            typeface = Typeface.create(tf, Typeface.BOLD)
        }
        canvas.drawText(title, 48f, 68f, titlePaint)

        if (partitionPlan != null && partitionPlan.plots.isNotEmpty()) {
            drawPartitionOnCanvas(canvas, partitionPlan, 80f, 110f, width - 160f, height - 180f, tf, scaleFont = 2.1f)
        } else if (points.size >= 2) {
            drawPolygonOnCanvas(canvas, points, 80f, 110f, width - 160f, height - 180f, tf, scaleFont = 2.1f)
        }

        val ext = if (asJpg) "jpg" else "png"
        val file = File(exportsDir(context), "noksha_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { out ->
            val fmt = if (asJpg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            bmp.compress(fmt, 94, out)
        }
        bmp.recycle()
        return file
    }

    fun exportDxf(
        context: Context,
        title: String,
        points: List<SurveyPoint2D>,
        closed: Boolean = true,
    ): File {
        val sb = StringBuilder()
        sb.appendLine("0\nSECTION\n2\nHEADER\n9\n\$INSUNITS\n70\n2\n0\nENDSEC")
        sb.appendLine("0\nSECTION\n2\nENTITIES")
        if (points.size >= 2) {
            sb.appendLine("0\nLWPOLYLINE\n8\nSURVEY_PLOT\n90\n${points.size}\n70\n${if (closed) 1 else 0}")
            points.forEach { pt ->
                sb.appendLine("10\n${pt.x}\n20\n${pt.y}")
            }
            points.forEachIndexed { idx, pt ->
                val lbl = pt.label.ifBlank { "P${idx + 1}" }
                sb.appendLine("0\nTEXT\n8\nLABELS\n10\n${pt.x}\n20\n${pt.y}\n40\n2.5\n1\n$lbl")
            }
        }
        sb.appendLine("0\nENDSEC\n0\nEOF")
        val safeName = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "cad_plot" }
        val file = File(exportsDir(context), "${safeName}_${System.currentTimeMillis()}.dxf")
        file.writeText(sb.toString())
        return file
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String = "HowladerSurveyReport") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    fun saveToDownloads(context: Context, file: File, mimeType: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/HowladerSurvey")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val saved = saveToDownloads(context, file, mimeType)
        if (saved) {
            Toast.makeText(
                context,
                "ডাউনলোড হয়েছে: Downloads/HowladerSurvey/${file.name}",
                Toast.LENGTH_LONG,
            ).show()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "শেয়ার / সেভ করুন"))
    }

    private fun drawPolygonOnCanvas(
        canvas: Canvas,
        points: List<SurveyPoint2D>,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        tf: Typeface,
        scaleFont: Float = 1f,
    ) {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val spanX = max(1.0, maxX - minX)
        val spanY = max(1.0, maxY - minY)
        val scale = min(w / spanX, h / spanY) * 0.84
        val cx = left + w / 2f
        val cy = top + h / 2f
        val midX = (minX + maxX) / 2.0
        val midY = (minY + maxY) / 2.0

        fun sx(p: SurveyPoint2D) = (cx + (p.x - midX) * scale).toFloat()
        fun sy(p: SurveyPoint2D) = (cy - (p.y - midY) * scale).toFloat()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 21, 94, 239)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#155EEF")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * scaleFont
        }
        val path = Path()
        points.forEachIndexed { idx, pt ->
            val x = sx(pt)
            val y = sy(pt)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (points.size >= 3) path.close()
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f * scaleFont
            typeface = Typeface.create(tf, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DC2626") }

        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val ax = sx(a)
            val ay = sy(a)
            canvas.drawCircle(ax, ay, 4f * scaleFont, dotPaint)
            canvas.drawText(a.label.ifBlank { "P${i + 1}" }, ax, ay - 7f * scaleFont, textPaint)

            if (points.size >= 2 && (i < points.lastIndex || points.size >= 3)) {
                val bx = sx(b)
                val by = sy(b)
                val dist = SurveyMath.distanceBetween(a, b)
                canvas.drawText("${LandUnitConverter.format(dist, 1)}'", (ax + bx) / 2f, (ay + by) / 2f - 4f, textPaint)
            }
        }
    }

    private fun drawPartitionOnCanvas(
        canvas: Canvas,
        plan: PartitionPlanResult,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        tf: Typeface,
        scaleFont: Float = 1f,
    ) {
        val scale = min(w / max(1.0, plan.totalLengthFt), h / max(1.0, plan.totalWidthFt)) * 0.88
        val ox = left + (w - (plan.totalLengthFt * scale).toFloat()) / 2f
        val oy = top + (h - (plan.totalWidthFt * scale).toFloat()) / 2f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f * scaleFont
            typeface = Typeface.create(tf, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scaleFont
        }

        plan.plots.forEach { plot ->
            val l = ox + (plot.x * scale).toFloat()
            val t = oy + (plot.y * scale).toFloat()
            val r = l + (plot.widthFt * scale).toFloat()
            val b = t + (plot.heightFt * scale).toFloat()
            val c = plot.color.toArgb()
            val fill = Paint().apply {
                color = Color.argb(65, Color.red(c), Color.green(c), Color.blue(c))
                style = Paint.Style.FILL
            }
            canvas.drawRect(l, t, r, b, fill)
            canvas.drawRect(l, t, r, b, borderPaint)
            val shotok = LandUnitConverter.format(plot.areaSqFt / 435.6, 2)
            canvas.drawText(plot.name, (l + r) / 2f, (t + b) / 2f - 4f * scaleFont, textPaint)
            canvas.drawText("$shotok শতক", (l + r) / 2f, (t + b) / 2f + 10f * scaleFont, textPaint)
        }

        plan.roadRect?.let { road ->
            val l = ox + (road.x * scale).toFloat()
            val t = oy + (road.y * scale).toFloat()
            val r = l + (road.widthFt * scale).toFloat()
            val b = t + (road.heightFt * scale).toFloat()
            val roadPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.FILL
            }
            canvas.drawRect(l, t, r, b, roadPaint)
            canvas.drawRect(l, t, r, b, borderPaint)
            canvas.drawText("রাস্তা", (l + r) / 2f, (t + b) / 2f + 4f * scaleFont, textPaint)
        }
    }
}
