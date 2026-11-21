package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.math.LandUnitConverter
import com.example.core.math.SurveyMath
import com.example.core.math.SurveyPoint2D
import com.example.ui.components.*
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class CadMode(val labelBn: String) {
    ADD_POINT("পয়েন্ট যোগ"),
    MOVE_POINT("কর্নার সরান"),
}

@Composable
fun CadEditorScreen(
    initialPoints: List<SurveyPoint2D>,
    surveyorFooter: String,
    onPointsChanged: (List<SurveyPoint2D>) -> Unit,
    onBack: () -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, pts: List<SurveyPoint2D>, summary: String) -> Unit,
) {
    val context = LocalContext.current
    var points by remember(initialPoints) { mutableStateOf(initialPoints) }
    var undoStack by remember { mutableStateOf<List<List<SurveyPoint2D>>>(emptyList()) }
    var mode by remember { mutableStateOf(CadMode.ADD_POINT) }
    var snapToGrid by remember { mutableStateOf(true) }
    var showDimensions by remember { mutableStateOf(true) }
    var selectedIdx by remember { mutableIntStateOf(-1) }
    var showSave by remember { mutableStateOf(false) }

    fun pushPoints(newPts: List<SurveyPoint2D>) {
        undoStack = undoStack + listOf(points)
        points = newPts
        onPointsChanged(newPts)
    }

    val areaSqFt = remember(points) { SurveyMath.polygonAreaShoelace(points) }
    val perimFt = remember(points) { SurveyMath.polygonPerimeter(points) }

    Scaffold(
        topBar = {
            SurveyTopBar(
                title = "CAD Drawing Editor",
                subtitle = "ইন্টারেক্টিভ নকশা ও DXF/PDF এক্সপোর্ট",
                onBack = onBack,
                actions = {
                    IconButton(
                        enabled = undoStack.isNotEmpty(),
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                points = undoStack.last()
                                undoStack = undoStack.dropLast(1)
                                onPointsChanged(points)
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Undo, contentDescription = "Undo")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // CAD Toolbar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CadMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m.labelBn) },
                        )
                    }
                    FilterChip(
                        selected = snapToGrid,
                        onClick = { snapToGrid = !snapToGrid },
                        label = { Text("Snap 5'") },
                    )
                    FilterChip(
                        selected = showDimensions,
                        onClick = { showDimensions = !showDimensions },
                        label = { Text("মাপ") },
                    )
                }
            }

            // Interactive CAD Vector Canvas
            item {
                val minX = if (points.isEmpty()) 0.0 else min(0.0, points.minOf { it.x }) - 15.0
                val maxX = if (points.isEmpty()) 120.0 else max(120.0, points.maxOf { it.x }) + 15.0
                val minY = if (points.isEmpty()) 0.0 else min(0.0, points.minOf { it.y }) - 15.0
                val maxY = if (points.isEmpty()) 90.0 else max(90.0, points.maxOf { it.y }) + 15.0
                val spanX = max(20.0, maxX - minX)
                val spanY = max(20.0, maxY - minY)

                val borderColor = MaterialTheme.colorScheme.outline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0B1324))
                        .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                        .pointerInput(mode, snapToGrid, minX, maxX, minY, maxY) {
                            fun toWorld(pos: Offset): Pair<Double, Double> {
                                val pad = 32f
                                val availW = max(20f, size.width - pad * 2)
                                val availH = max(20f, size.height - pad * 2)
                                val scale = min(availW / spanX, availH / spanY)
                                val wx = minX + (pos.x - pad) / scale
                                val wy = maxY - (pos.y - pad) / scale
                                return if (snapToGrid) {
                                    (wx / 5.0).roundToInt() * 5.0 to (wy / 5.0).roundToInt() * 5.0
                                } else wx to wy
                            }

                            if (mode == CadMode.ADD_POINT) {
                                detectTapGestures { offset ->
                                    val (wx, wy) = toWorld(offset)
                                    val nextLabel = "P${points.size + 1}"
                                    pushPoints(points + SurveyPoint2D(wx, wy, nextLabel))
                                }
                            } else {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val (wx, wy) = toWorld(start)
                                        selectedIdx = points.indices.minByOrNull { idx ->
                                            hypot(points[idx].x - wx, points[idx].y - wy)
                                        } ?: -1
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (selectedIdx in points.indices) {
                                            val (wx, wy) = toWorld(change.position)
                                            val updated = points.toMutableList()
                                            updated[selectedIdx] = updated[selectedIdx].copy(x = wx, y = wy)
                                            points = updated
                                            onPointsChanged(updated)
                                        }
                                    },
                                    onDragEnd = { selectedIdx = -1 },
                                )
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pad = 32f
                        val availW = max(20f, size.width - pad * 2)
                        val availH = max(20f, size.height - pad * 2)
                        val scale = min(availW / spanX, availH / spanY)

                        // CAD Blueprint Grid
                        val step = 36f
                        var gx = step
                        while (gx < size.width) {
                            drawLine(Color(0xFF1E293B), Offset(gx, 0f), Offset(gx, size.height), 1f)
                            gx += step
                        }
                        var gy = step
                        while (gy < size.height) {
                            drawLine(Color(0xFF1E293B), Offset(0f, gy), Offset(size.width, gy), 1f)
                            gy += step
                        }

                        fun mapPt(pt: SurveyPoint2D): Offset = Offset(
                            x = (pad + (pt.x - minX) * scale).toFloat(),
                            y = (pad + (maxY - pt.y) * scale).toFloat(),
                        )

                        val mapped = points.map(::mapPt)
                        if (mapped.size >= 2) {
                            val path = Path().apply {
                                moveTo(mapped[0].x, mapped[0].y)
                                for (i in 1 until mapped.size) lineTo(mapped[i].x, mapped[i].y)
                                if (mapped.size >= 3) close()
                            }
                            if (mapped.size >= 3) {
                                drawPath(path, Color(0xFF38BDF8).copy(alpha = 0.16f))
                            }
                            drawPath(path, Color(0xFF38BDF8), style = Stroke(width = 3.5f))
                        }

                        val lblPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#F8FAFC")
                            textSize = 24f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val dimPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#4ADE80")
                            textSize = 22f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        for (i in mapped.indices) {
                            val p = mapped[i]
                            drawCircle(Color(0xFFF59E0B), radius = 8f, center = p)
                            drawContext.canvas.nativeCanvas.drawText(
                                points[i].label.ifBlank { "P${i + 1}" }, p.x, p.y - 12f, lblPaint
                            )
                            if (showDimensions && mapped.size >= 2) {
                                val next = (i + 1) % mapped.size
                                val dist = SurveyMath.distanceBetween(points[i], points[next])
                                drawContext.canvas.nativeCanvas.drawText(
                                    "${LandUnitConverter.format(dist, 1)}'",
                                    (p.x + mapped[next].x) / 2f,
                                    (p.y + mapped[next].y) / 2f - 4f,
                                    dimPaint,
                                )
                            }
                        }
                    }
                }
            }

            // Export DXF, PDF, PNG + Clear
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val dxf = ReportExporter.exportDxf(context, "CAD_Plot", points, closed = true)
                            ReportExporter.shareFile(context, dxf, "application/dxf")
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("export_dxf_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("DXF")
                    }
                    OutlinedButton(
                        onClick = {
                            val img = ReportExporter.exportNokshaImage(context, "CAD ডিজিটাল নকশা", points = points, asJpg = false)
                            ReportExporter.shareFile(context, img, "image/png")
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PNG")
                    }
                    OutlinedButton(
                        onClick = { pushPoints(emptyList()) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            item {
                AllUnitsResultCard(
                    areaSqFt = areaSqFt,
                    perimeterFt = perimFt,
                    onSaveToProject = { showSave = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, "CAD ডিজিটাল নকশা", "${points.size} টি সার্ভে পয়েন্ট",
                            listOf("পয়েন্ট" to "${points.size} টি"), areaSqFt, points, surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
            }
        }
    }

    if (showSave) {
        SaveToProjectDialog(
            defaultTitle = "CAD নকশা প্রজেক্ট",
            onDismiss = { showSave = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(name, owner, mouza, dag, khatian, areaSqFt, points, "CAD প্লট (${points.size} পয়েন্ট)")
                showSave = false
            },
        )
    }
}

@Composable
fun TotalStationImportScreen(
    surveyorFooter: String,
    onBack: () -> Unit,
    onOpenInCad: (List<SurveyPoint2D>) -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, pts: List<SurveyPoint2D>, summary: String) -> Unit,
) {
    val context = LocalContext.current
    var rawText by remember {
        mutableStateOf(
            "1,100.0,100.0,12.4,CORNER\n2,195.5,102.0,12.6,CORNER\n3,190.2,168.4,12.1,CORNER\n4,102.8,164.0,12.3,CORNER"
        )
    }
    var showSave by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val txt = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!txt.isNullOrBlank()) rawText = txt
            }
        },
    )

    val parsedPoints = remember(rawText) { parseTotalStationCsv(rawText) }
    val areaSqFt = remember(parsedPoints) { SurveyMath.polygonAreaShoelace(parsedPoints) }
    val perimFt = remember(parsedPoints) { SurveyMath.polygonPerimeter(parsedPoints) }

    Scaffold(
        topBar = {
            SurveyTopBar(title = "Total Station Import", subtitle = "CSV / TXT Easting-Northing-Elevation", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("CSV / TXT ফাইল ইম্পোর্ট বা পেস্ট করুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("ফরম্যাট: PointNo, Easting (X), Northing (Y), Elevation (Z)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { filePicker.launch(arrayOf("text/csv", "text/plain", "*/*")) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("pick_ts_file_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("CSV/TXT বাছুন")
                            }
                            OutlinedButton(
                                onClick = {
                                    rawText = "P1,500.0,500.0,10.2,BM\nP2,612.4,504.8,10.5,COR\nP3,608.0,584.2,10.3,COR\nP4,540.5,592.0,10.1,COR\nP5,496.2,550.6,10.0,COR"
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("নমুনা ডেটা")
                            }
                        }
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            label = { Text("Total Station Coordinate Data") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (parsedPoints.size >= 2) {
                item { NokshaPolygonCanvas(points = parsedPoints, height = 220.dp) }
            }

            if (parsedPoints.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ইম্পোর্টকৃত পয়েন্টসমূহ (${parsedPoints.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            parsedPoints.take(20).forEach { pt ->
                                Text(
                                    "${pt.label}: E=${LandUnitConverter.format(pt.x, 2)} | N=${LandUnitConverter.format(pt.y, 2)} | RL=${LandUnitConverter.format(pt.elevation, 2)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onOpenInCad(parsedPoints) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("CAD Editor-এ খুলুন")
                        }
                        OutlinedButton(
                            onClick = {
                                val dxf = ReportExporter.exportDxf(context, "TotalStation_Plot", parsedPoints, closed = true)
                                ReportExporter.shareFile(context, dxf, "application/dxf")
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("DXF Export")
                        }
                    }
                }
            }

            item {
                AllUnitsResultCard(
                    areaSqFt = areaSqFt,
                    perimeterFt = perimFt,
                    onSaveToProject = { showSave = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, "Total Station সার্ভে রিপোর্ট", "${parsedPoints.size} টি পয়েন্ট",
                            listOf("মোট পয়েন্ট" to "${parsedPoints.size}"), areaSqFt, parsedPoints, surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
            }
        }
    }

    if (showSave) {
        SaveToProjectDialog(
            defaultTitle = "Total Station প্রজেক্ট",
            onDismiss = { showSave = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(name, owner, mouza, dag, khatian, areaSqFt, parsedPoints, "Total Station (${parsedPoints.size} পয়েন্ট)")
                showSave = false
            },
        )
    }
}

private fun parseTotalStationCsv(raw: String): List<SurveyPoint2D> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val result = mutableListOf<SurveyPoint2D>()
    lines.forEachIndexed { idx, line ->
        val parts = line.split(',', '\t', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size >= 3) {
            // Supports either (PointNo, Easting, Northing, [Elevation]) or (Easting, Northing)
            val p1 = parts[0].toDoubleOrNull()
            val p2 = parts[1].toDoubleOrNull()
            val p3 = parts[2].toDoubleOrNull()
            if (p2 != null && p3 != null) {
                val elev = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                result.add(SurveyPoint2D(x = p2, y = p3, label = parts[0], elevation = elev))
            } else if (p1 != null && p2 != null) {
                result.add(SurveyPoint2D(x = p1, y = p2, label = "P${idx + 1}", elevation = p3 ?: 0.0))
            }
        }
    }
    return result
}
