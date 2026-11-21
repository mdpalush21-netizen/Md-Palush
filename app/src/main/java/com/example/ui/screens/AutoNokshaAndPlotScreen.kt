package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.math.*
import com.example.model.SurveyToolId
import com.example.ui.components.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Composable
fun AutoNokshaAndPlotScreen(
    tool: SurveyToolId,
    surveyorFooter: String,
    onBack: () -> Unit,
    onOpenInCad: (List<SurveyPoint2D>) -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, pts: List<SurveyPoint2D>, summary: String) -> Unit,
) {
    val context = LocalContext.current
    var showSave by remember { mutableStateOf(false) }
    var activeArea by remember { mutableDoubleStateOf(0.0) }
    var activePts by remember { mutableStateOf<List<SurveyPoint2D>>(emptyList()) }
    var activeSummary by remember { mutableStateOf("") }

    Scaffold(
        topBar = { SurveyTopBar(title = tool.titleBn, subtitle = tool.subtitleBn, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (tool) {
                SurveyToolId.AUTO_NOKSHA -> AutoNokshaBody(
                    onUpdate = { area, pts, sum -> activeArea = area; activePts = pts; activeSummary = sum },
                    onSave = { showSave = true },
                    onOpenCad = { onOpenInCad(activePts) },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, "অটো ডিজিটাল নকশা", activeSummary,
                            listOf("পরিমাপ" to activeSummary), activeArea, activePts, surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                    onExportJpg = {
                        val file = ReportExporter.exportNokshaImage(context, "অটো ডিজিটাল নকশা", points = activePts, asJpg = true)
                        ReportExporter.shareFile(context, file, "image/jpeg")
                    },
                )
                SurveyToolId.PLOT_PLANNING -> PlotPlanningBody(
                    onSave = { area, sum -> activeArea = area; activeSummary = sum; showSave = true },
                    onExportPdf = { area, sum ->
                        val file = ReportExporter.exportReportPdf(
                            context, "আবাসিক প্লট প্ল্যানিং লেআউট", sum,
                            listOf("লেআউট বিবরণ" to sum), area, emptyList(), surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.PENTAGRAPH -> PentagraphBody()
                else -> Unit
            }
        }
    }

    if (showSave) {
        SaveToProjectDialog(
            defaultTitle = tool.titleBn,
            onDismiss = { showSave = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(name, owner, mouza, dag, khatian, activeArea, activePts, activeSummary)
                showSave = false
            },
        )
    }
}

@Composable
private fun AutoNokshaBody(
    onUpdate: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSave: () -> Unit,
    onOpenCad: () -> Unit,
    onExportPdf: () -> Unit,
    onExportJpg: () -> Unit,
) {
    var north by remember { mutableStateOf("86") }
    var south by remember { mutableStateOf("94") }
    var east by remember { mutableStateOf("62") }
    var west by remember { mutableStateOf("58") }
    var diag by remember { mutableStateOf("108") }

    val res = remember(north, south, east, west, diag) {
        SurveyMath.quadrilateralExact(
            north.toDoubleOrNull() ?: 0.0,
            south.toDoubleOrNull() ?: 0.0,
            east.toDoubleOrNull() ?: 0.0,
            west.toDoubleOrNull() ?: 0.0,
            diag.toDoubleOrNull(),
        )
    }
    LaunchedEffect(res) {
        onUpdate(res.exactAreaSqFt, res.vertices, "উ:$north' দ:$south' পূ:$east' প:$west' কর্ণ:$diag'")
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (res.isValid) {
                NokshaPolygonCanvas(points = res.vertices, height = 230.dp)
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("চার বাহু ও কর্ণ দিয়ে অটো নকশা তৈরি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(north, { north = it }, "উত্তর", "ফুট", Modifier.weight(1f))
                        NumberInputField(south, { south = it }, "দক্ষিণ", "ফুট", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(east, { east = it }, "পূর্ব", "ফুট", Modifier.weight(1f))
                        NumberInputField(west, { west = it }, "পশ্চিম", "ফুট", Modifier.weight(1f))
                    }
                    NumberInputField(diag, { diag = it }, "কর্ণ (SW-NE)", "ফুট")
                    res.errorReason?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (res.isValid) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = onOpenCad, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CAD এডিট")
                            }
                            OutlinedButton(onClick = onExportJpg, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("JPG সেভ")
                            }
                        }
                    }
                }
            }
        }
        item {
            AllUnitsResultCard(
                areaSqFt = res.exactAreaSqFt,
                perimeterFt = res.perimeterFt,
                onSaveToProject = onSave,
                onExportPdf = onExportPdf,
            )
        }
    }
}

@Composable
private fun PlotPlanningBody(
    onSave: (Double, String) -> Unit,
    onExportPdf: (Double, String) -> Unit,
) {
    var totalLen by remember { mutableStateOf("200") }
    var totalWid by remember { mutableStateOf("120") }
    var roadW by remember { mutableStateOf("16") }
    var plotW by remember { mutableStateOf("40") }
    var doubleSide by remember { mutableStateOf(true) }

    val L = (totalLen.toDoubleOrNull() ?: 0.0).coerceAtLeast(10.0)
    val W = (totalWid.toDoubleOrNull() ?: 0.0).coerceAtLeast(10.0)
    val R = (roadW.toDoubleOrNull() ?: 0.0).coerceIn(0.0, W * 0.4)
    val PW = (plotW.toDoubleOrNull() ?: 20.0).coerceAtLeast(10.0)

    val cols = max(1, floor(L / PW).toInt())
    val rows = if (doubleSide) 2 else 1
    val plotCount = cols * rows
    val grossArea = L * W
    val roadArea = L * R
    val netArea = (grossArea - roadArea).coerceAtLeast(0.0)
    val singlePlotSqFt = if (plotCount > 0) netArea / plotCount else 0.0
    val roadPct = if (grossArea > 0) (roadArea / grossArea) * 100.0 else 0.0
    val summary = "মোট প্লট: $plotCount টি | প্রতি প্লট: ${LandUnitConverter.format(singlePlotSqFt / 720.0, 2)} কাঠা | রাস্তা: ${LandUnitConverter.format(roadPct, 1)}%"

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF8FBFF))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pad = 24f
                    val scale = min((size.width - pad * 2) / L, (size.height - pad * 2) / W).toFloat()
                    val ox = (size.width - L.toFloat() * scale) / 2f
                    val oy = (size.height - W.toFloat() * scale) / 2f
                    val netH = (W - R).toFloat()
                    val rowH = if (doubleSide) netH / 2f else netH
                    val colW = L.toFloat() / cols

                    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#0F172A")
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    // Top row plots
                    for (c in 0 until cols) {
                        val px = ox + c * colW * scale
                        val py = oy
                        drawRect(Color(0xFFDBEAFE), Offset(px, py), Size(colW * scale, rowH * scale))
                        drawRect(Color(0xFF1D4ED8), Offset(px, py), Size(colW * scale, rowH * scale), style = Stroke(2f))
                        drawContext.canvas.nativeCanvas.drawText("P${c + 1}", px + (colW * scale) / 2f, py + (rowH * scale) / 2f + 7f, textPaint)
                    }
                    // Road
                    val roadY = oy + rowH * scale
                    drawRect(Color(0xFFCBD5E1), Offset(ox, roadY), Size(L.toFloat() * scale, R.toFloat() * scale))
                    drawContext.canvas.nativeCanvas.drawText("রাস্তা (${R.toInt()}')", ox + (L.toFloat() * scale) / 2f, roadY + (R.toFloat() * scale) / 2f + 7f, textPaint)

                    // Bottom row if doubleSide
                    if (doubleSide) {
                        val botY = roadY + R.toFloat() * scale
                        for (c in 0 until cols) {
                            val px = ox + c * colW * scale
                            drawRect(Color(0xFFDCFCE7), Offset(px, botY), Size(colW * scale, rowH * scale))
                            drawRect(Color(0xFF16A34A), Offset(px, botY), Size(colW * scale, rowH * scale), style = Stroke(2f))
                            drawContext.canvas.nativeCanvas.drawText("P${cols + c + 1}", px + (colW * scale) / 2f, botY + (rowH * scale) / 2f + 7f, textPaint)
                        }
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(totalLen, { totalLen = it }, "মোট দৈর্ঘ্য", "ফুট", Modifier.weight(1f))
                        NumberInputField(totalWid, { totalWid = it }, "মোট প্রস্থ", "ফুট", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(roadW, { roadW = it }, "রাস্তার প্রস্থ", "ফুট", Modifier.weight(1f))
                        NumberInputField(plotW, { plotW = it }, "প্রতি প্লটের মুখ", "ফুট", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = doubleSide, onClick = { doubleSide = true }, label = { Text("রাস্তার দুই পাশে প্লট") })
                        FilterChip(selected = !doubleSide, onClick = { doubleSide = false }, label = { Text("এক পাশে প্লট") })
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(summary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("প্রতি প্লট শতাংশ: ${LandUnitConverter.format(singlePlotSqFt / 435.6, 2)} শতক", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onSave(netArea, summary) }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("প্রজেক্টে সেভ")
                        }
                        OutlinedButton(onClick = { onExportPdf(netArea, summary) }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("PDF লেআউট")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PentagraphBody() {
    val scales = listOf(
        "১৬\" = ১ মাইল (১:৩৯৬০)" to 3960.0,
        "৩২\" = ১ মাইল (১:১৯৮০)" to 1980.0,
        "৬৪\" = ১ মাইল (১:৯৯০)" to 990.0,
        "৮০\" = ১ মাইল (১:৭৯২)" to 792.0,
    )
    var sourceIdx by remember { mutableIntStateOf(0) }
    var targetIdx by remember { mutableIntStateOf(1) }
    var mapInches by remember { mutableStateOf("2.5") }

    val srcScale = scales[sourceIdx].second
    val tgtScale = scales[targetIdx].second
    val ratio = srcScale / tgtScale
    val inches = mapInches.toDoubleOrNull() ?: 0.0
    val groundFeet = (inches * (srcScale / 12.0))
    val targetInches = inches * ratio

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("পেন্টাগ্রাফ নকশা স্কেল তুলনা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("মূল নকশার স্কেল (CS/RS):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scales.indices.toList()) { i ->
                            FilterChip(selected = sourceIdx == i, onClick = { sourceIdx = i }, label = { Text(scales[i].first) })
                        }
                    }
                    Text("তুলনা/রূপান্তরিত স্কেল (BS):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scales.indices.toList()) { i ->
                            FilterChip(selected = targetIdx == i, onClick = { targetIdx = i }, label = { Text(scales[i].first) })
                        }
                    }
                    NumberInputField(mapInches, { mapInches = it }, "নকশায় মাপা দৈর্ঘ্য", "ইঞ্চি")
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("স্কেল গুণিতক (Scale Factor): ${LandUnitConverter.format(ratio, 3)}x", fontWeight = FontWeight.Bold)
                            Text("জমির বাস্তব দৈর্ঘ্য: ${LandUnitConverter.format(groundFeet, 2)} ফুট (${LandUnitConverter.format(groundFeet / 0.66, 1)} লিঙ্ক)")
                            Text("টার্গেট নকশায় মাপ হবে: ${LandUnitConverter.format(targetInches, 3)} ইঞ্চি")
                        }
                    }
                }
            }
        }
    }
}
