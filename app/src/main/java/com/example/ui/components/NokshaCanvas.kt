package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.math.LandUnitConverter
import com.example.core.math.PartitionPlanResult
import com.example.core.math.SurveyMath
import com.example.core.math.SurveyPoint2D
import kotlin.math.max
import kotlin.math.min

@Composable
fun NokshaPolygonCanvas(
    points: List<SurveyPoint2D>,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    closed: Boolean = true,
    showDimensions: Boolean = true,
    showGrid: Boolean = true,
    fillColor: Color = Color(0xFF155EEF).copy(alpha = 0.14f),
    strokeColor: Color = Color(0xFF155EEF),
    backgroundColor: Color = Color(0xFFF8FBFF),
) {
    val borderColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showGrid) {
                val step = 36f
                var gx = step
                while (gx < size.width) {
                    drawLine(
                        color = Color(0xFFE2ECF9),
                        start = Offset(gx, 0f),
                        end = Offset(gx, size.height),
                        strokeWidth = 1f,
                    )
                    gx += step
                }
                var gy = step
                while (gy < size.height) {
                    drawLine(
                        color = Color(0xFFE2ECF9),
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1f,
                    )
                    gy += step
                }
            }

            // North Compass Badge in top-right
            val compassCenter = Offset(size.width - 30f, 30f)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 18f,
                center = compassCenter,
            )
            drawCircle(
                color = Color(0xFF94A3B8),
                radius = 18f,
                center = compassCenter,
                style = Stroke(width = 1.5f),
            )
            drawContext.canvas.nativeCanvas.apply {
                val compassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#DC2626")
                    textSize = 22f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                drawText("N↑", compassCenter.x, compassCenter.y + 8f, compassPaint)
            }

            if (points.isEmpty()) return@Canvas

            val minX = points.minOf { it.x }
            val maxX = points.maxOf { it.x }
            val minY = points.minOf { it.y }
            val maxY = points.maxOf { it.y }
            val spanX = max(1.0, maxX - minX)
            val spanY = max(1.0, maxY - minY)
            val pad = 42f
            val availW = max(20f, size.width - pad * 2)
            val availH = max(20f, size.height - pad * 2)
            val scale = min(availW / spanX, availH / spanY)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val midX = (minX + maxX) / 2.0
            val midY = (minY + maxY) / 2.0

            fun mapPt(pt: SurveyPoint2D): Offset = Offset(
                x = (cx + (pt.x - midX) * scale).toFloat(),
                y = (cy - (pt.y - midY) * scale).toFloat(),
            )

            val mapped = points.map(::mapPt)
            if (mapped.size >= 2) {
                val path = Path().apply {
                    moveTo(mapped[0].x, mapped[0].y)
                    for (i in 1 until mapped.size) {
                        lineTo(mapped[i].x, mapped[i].y)
                    }
                    if (closed && mapped.size >= 3) close()
                }
                if (closed && mapped.size >= 3) {
                    drawPath(path = path, color = fillColor)
                }
                drawPath(path = path, color = strokeColor, style = Stroke(width = 4.5f))
            }

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 25f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1D4ED8")
                textSize = 23f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val dimBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(220, 255, 255, 255)
            }

            for (i in mapped.indices) {
                val a = mapped[i]
                drawCircle(color = Color.White, radius = 9f, center = a)
                drawCircle(color = Color(0xFFDC2626), radius = 6f, center = a)
                val label = points[i].label.ifBlank { "P${i + 1}" }
                drawContext.canvas.nativeCanvas.drawText(label, a.x, a.y - 14f, labelPaint)

                if (showDimensions && mapped.size >= 2 && (i < mapped.lastIndex || (closed && mapped.size >= 3))) {
                    val nextIdx = (i + 1) % mapped.size
                    val b = mapped[nextIdx]
                    val distFt = SurveyMath.distanceBetween(points[i], points[nextIdx])
                    val mx = (a.x + b.x) / 2f
                    val my = (a.y + b.y) / 2f
                    val text = "${LandUnitConverter.format(distFt, 1)}'"
                    val tw = dimPaint.measureText(text)
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        mx - tw / 2f - 8f, my - 22f, mx + tw / 2f + 8f, my + 8f, 8f, 8f, dimBgPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(text, mx, my, dimPaint)
                }
            }
        }
    }
}

@Composable
fun PartitionNokshaCanvas(
    plan: PartitionPlanResult,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 32f
            val availW = max(20f, size.width - pad * 2)
            val availH = max(20f, size.height - pad * 2)
            val scale = min(availW / max(1.0, plan.totalLengthFt), availH / max(1.0, plan.totalWidthFt)).toFloat()
            val ox = (size.width - plan.totalLengthFt.toFloat() * scale) / 2f
            val oy = (size.height - plan.totalWidthFt.toFloat() * scale) / 2f

            plan.plots.forEach { plot ->
                val l = ox + plot.x.toFloat() * scale
                val t = oy + plot.y.toFloat() * scale
                val w = plot.widthFt.toFloat() * scale
                val h = plot.heightFt.toFloat() * scale

                drawRect(
                    color = plot.color.copy(alpha = 0.24f),
                    topLeft = Offset(l, t),
                    size = Size(w, h),
                )
                drawRect(
                    color = plot.color,
                    topLeft = Offset(l, t),
                    size = Size(w, h),
                    style = Stroke(width = 3f),
                )

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#0F172A")
                    textSize = if (w < 110f || h < 70f) 20f else 25f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = plot.color.toArgb()
                    textSize = if (w < 110f || h < 70f) 18f else 22f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val cx = l + w / 2f
                val cy = t + h / 2f
                val shotok = LandUnitConverter.format(plot.areaSqFt / 435.6, 2)
                drawContext.canvas.nativeCanvas.drawText(plot.name.take(14), cx, cy - 6f, textPaint)
                drawContext.canvas.nativeCanvas.drawText("$shotok শতক", cx, cy + 20f, subPaint)
            }

            plan.roadRect?.let { road ->
                val l = ox + road.x.toFloat() * scale
                val t = oy + road.y.toFloat() * scale
                val w = road.widthFt.toFloat() * scale
                val h = road.heightFt.toFloat() * scale
                drawRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(l, t),
                    size = Size(w, h),
                )
                drawRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(l, t),
                    size = Size(w, h),
                    style = Stroke(width = 2f),
                )
                if (w > h) {
                    drawLine(
                        color = Color.White,
                        start = Offset(l + 8f, t + h / 2f),
                        end = Offset(l + w - 8f, t + h / 2f),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                    )
                } else {
                    drawLine(
                        color = Color.White,
                        start = Offset(l + w / 2f, t + 8f),
                        end = Offset(l + w / 2f, t + h - 8f),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                    )
                }
                val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#1E293B")
                    textSize = 21f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText("রাস্তা", l + w / 2f, t + h / 2f + 7f, roadPaint)
            }
        }
    }
}
