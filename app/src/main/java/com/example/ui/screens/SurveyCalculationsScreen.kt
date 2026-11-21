package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.math.*
import com.example.model.SurveyToolId
import com.example.ui.components.*
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

@Composable
fun SurveyCalculationsScreen(
    tool: SurveyToolId,
    surveyorFooter: String,
    onBack: () -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, pts: List<SurveyPoint2D>, summary: String) -> Unit,
) {
    val context = LocalContext.current
    var showSave by remember { mutableStateOf(false) }
    var areaSqFt by remember { mutableDoubleStateOf(0.0) }
    var points by remember { mutableStateOf<List<SurveyPoint2D>>(emptyList()) }
    var summary by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SurveyTopBar(title = tool.titleBn, subtitle = tool.subtitleBn, onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tool) {
                SurveyToolId.TIE_LINE -> TieLineBody(
                    onResult = { a, pts, s -> areaSqFt = a; points = pts; summary = s },
                    onSave = { showSave = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "টাই লাইন সার্ভে",
                            listOf("বিবরণ" to summary), areaSqFt, points, surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.SLOPE_ELEVATION -> SlopeElevationBody()
                SurveyToolId.LAND_SURVEY_TRAVERSE -> TraverseBody(
                    onResult = { a, pts, s -> areaSqFt = a; points = pts; summary = s },
                    onSave = { showSave = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "ট্রাভার্স জরিপ ও সমন্বয়",
                            listOf("বিবরণ" to summary), areaSqFt, points, surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.BOUNDARY_DEMARCATION -> BoundaryDemarcationBody(
                    onResult = { a, pts, s -> areaSqFt = a; points = pts; summary = s },
                    onSave = { showSave = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "সীমানা নির্ধারণ ও বিয়ারিং",
                            listOf("বিবরণ" to summary), areaSqFt, points, surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                else -> Unit
            }
        }
    }

    if (showSave) {
        SaveToProjectDialog(
            defaultTitle = tool.titleBn,
            onDismiss = { showSave = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(name, owner, mouza, dag, khatian, areaSqFt, points, summary)
                showSave = false
            },
        )
    }
}

private data class TieRowUi(val id: Int, val chain: String, val left: String, val right: String)

@Composable
private fun TieLineBody(
    onResult: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var rows by remember {
        mutableStateOf(
            listOf(
                TieRowUi(1, "0", "0", "0"),
                TieRowUi(2, "30", "22", "18"),
                TieRowUi(3, "65", "28", "24"),
                TieRowUi(4, "100", "0", "0"),
            )
        )
    }
    val (area, pts) = remember(rows) {
        SurveyMath.tieLineArea(
            rows.map {
                TieOffsetRow(
                    it.chain.toDoubleOrNull() ?: 0.0,
                    it.left.toDoubleOrNull() ?: 0.0,
                    it.right.toDoubleOrNull() ?: 0.0,
                )
            }
        )
    }
    LaunchedEffect(area, pts) {
        onResult(area, pts, "বেসলাইন অফসেট: ${rows.size} টি স্টেশন")
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (pts.size >= 3) {
            item { NokshaPolygonCanvas(points = pts, height = 200.dp) }
        }
        items(rows, key = { it.id }) { row ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberInputField(row.chain, { v -> rows = rows.map { if (it.id == row.id) it.copy(chain = v) else it } }, "চেইনেজ", "ফু", Modifier.weight(1f))
                    NumberInputField(row.left, { v -> rows = rows.map { if (it.id == row.id) it.copy(left = v) else it } }, "বাম", "ফু", Modifier.weight(1f))
                    NumberInputField(row.right, { v -> rows = rows.map { if (it.id == row.id) it.copy(right = v) else it } }, "ডান", "ফু", Modifier.weight(1f))
                    if (rows.size > 2) {
                        IconButton(onClick = { rows = rows.filterNot { it.id == row.id } }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val next = (rows.maxOfOrNull { it.id } ?: 0) + 1
                    val lastChain = rows.lastOrNull()?.chain?.toDoubleOrNull() ?: 0.0
                    rows = rows + TieRowUi(next, "${(lastChain + 25).toInt()}", "20", "15")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("নতুন অফসেট স্টেশন যোগ করুন")
            }
        }
        item {
            AllUnitsResultCard(areaSqFt = area, onSaveToProject = onSave, onExportPdf = onExportPdf)
        }
    }
}

@Composable
private fun SlopeElevationBody() {
    val context = LocalContext.current
    var horizDist by remember { mutableStateOf("100") }
    var elevDiff by remember { mutableStateOf("8.5") }
    var livePitchDeg by remember { mutableDoubleStateOf(0.0) }
    var useLiveSensor by remember { mutableStateOf(false) }

    DisposableEffect(useLiveSensor) {
        val mgr = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = mgr?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val pitchRad = atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))
                livePitchDeg = kotlin.math.abs(Math.toDegrees(pitchRad))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (useLiveSensor && accel != null) {
            mgr.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { mgr?.unregisterListener(listener) }
    }

    val h = horizDist.toDoubleOrNull() ?: 0.0
    val e = elevDiff.toDoubleOrNull() ?: 0.0
    val slopePct = if (h > 0) (e / h) * 100.0 else 0.0
    val angleDeg = if (h > 0) Math.toDegrees(atan(e / h)) else 0.0
    val slopeDist = sqrt(h * h + e * e)
    val horizFromSlope = h * cos(Math.toRadians(angleDeg))

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ঢাল ও উচ্চতা পরিমাপ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    NumberInputField(horizDist, { horizDist = it }, "অনুভূমিক দূরত্ব (Horizontal)", "ফুট")
                    NumberInputField(elevDiff, { elevDiff = it }, "উচ্চতার পার্থক্য (Rise/Fall)", "ফুট")

                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ঢাল (Slope): ${LandUnitConverter.format(slopePct, 2)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("কোণ (Angle): ${LandUnitConverter.format(angleDeg, 2)}°", style = MaterialTheme.typography.bodyMedium)
                            Text("ঢালু দূরত্ব (Slope Dist): ${LandUnitConverter.format(slopeDist, 2)} ফুট", style = MaterialTheme.typography.bodyMedium)
                            Text("সংশোধিত অনুভূমিক দূরত্ব: ${LandUnitConverter.format(horizFromSlope, 2)} ফুট", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("লাইভ ডিভাইস টিল্ট সেন্সর (Clinometer)", style = MaterialTheme.typography.labelLarge)
                            Text("লাইভ কোণ: ${LandUnitConverter.format(livePitchDeg, 1)}°", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = useLiveSensor, onCheckedChange = { useLiveSensor = it })
                    }
                }
            }
        }
    }
}

private data class TravLegUi(val id: Int, val station: String, val bearing: String, val dist: String)

@Composable
private fun TraverseBody(
    onResult: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var legs by remember {
        mutableStateOf(
            listOf(
                TravLegUi(1, "S1", "90", "85"),
                TravLegUi(2, "S2", "5", "60"),
                TravLegUi(3, "S3", "270", "82"),
                TravLegUi(4, "S4", "183", "60"),
            )
        )
    }

    val res = remember(legs) {
        SurveyMath.computeTraverse(
            legs.map {
                TraverseLegInput(
                    it.station,
                    it.bearing.toDoubleOrNull() ?: 0.0,
                    it.dist.toDoubleOrNull() ?: 0.0,
                )
            }
        )
    }

    LaunchedEffect(res) {
        onResult(res.areaSqFt, res.adjustedPoints, "ট্রাভার্স (${legs.size} স্টেশন), ভুল=${LandUnitConverter.format(res.closingErrorFt, 2)}'")
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (res.adjustedPoints.size >= 3) {
            item { NokshaPolygonCanvas(points = res.adjustedPoints, height = 210.dp) }
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Closing Error: ${LandUnitConverter.format(res.closingErrorFt, 3)} ফুট (${res.precisionRatio})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "মোট দূরত্ব: ${LandUnitConverter.format(res.totalDistanceFt, 2)} ফুট | Bowditch সমন্বয়কৃত",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        items(legs, key = { it.id }) { leg ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = leg.station,
                        onValueChange = { v -> legs = legs.map { if (it.id == leg.id) it.copy(station = v) else it } },
                        label = { Text("স্টেশন") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                    )
                    NumberInputField(leg.bearing, { v -> legs = legs.map { if (it.id == leg.id) it.copy(bearing = v) else it } }, "বিয়ারিং°", "°", Modifier.weight(1f))
                    NumberInputField(leg.dist, { v -> legs = legs.map { if (it.id == leg.id) it.copy(dist = v) else it } }, "দূরত্ব", "ফু", Modifier.weight(1f))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val next = (legs.maxOfOrNull { it.id } ?: 0) + 1
                    legs = legs + TravLegUi(next, "S$next", "0", "40")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("স্টেশন যোগ করুন")
            }
        }
        item {
            AllUnitsResultCard(
                areaSqFt = res.areaSqFt,
                perimeterFt = res.totalDistanceFt,
                onSaveToProject = onSave,
                onExportPdf = onExportPdf,
            )
        }
    }
}

private data class BoundaryPtUi(val id: Int, val label: String, val x: String, val y: String)

@Composable
private fun BoundaryDemarcationBody(
    onResult: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var corners by remember {
        mutableStateOf(
            listOf(
                BoundaryPtUi(1, "C1", "0", "0"),
                BoundaryPtUi(2, "C2", "90", "0"),
                BoundaryPtUi(3, "C3", "86", "60"),
                BoundaryPtUi(4, "C4", "4", "58"),
            )
        )
    }
    val pts = remember(corners) {
        corners.map { SurveyPoint2D(it.x.toDoubleOrNull() ?: 0.0, it.y.toDoubleOrNull() ?: 0.0, it.label) }
    }
    val area = remember(pts) { SurveyMath.polygonAreaShoelace(pts) }
    val perim = remember(pts) { SurveyMath.polygonPerimeter(pts) }

    LaunchedEffect(area, pts) {
        onResult(area, pts, "সীমানা খুঁটি: ${pts.size} টি")
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { GpsDisclaimerBanner() }
        item { NokshaPolygonCanvas(points = pts, height = 210.dp) }
        items(corners, key = { it.id }) { c ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = c.label,
                        onValueChange = { v -> corners = corners.map { if (it.id == c.id) it.copy(label = v) else it } },
                        label = { Text("খুঁটি") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                    )
                    NumberInputField(c.x, { v -> corners = corners.map { if (it.id == c.id) it.copy(x = v) else it } }, "পূর্ব (X)", "ফু", Modifier.weight(1f))
                    NumberInputField(c.y, { v -> corners = corners.map { if (it.id == c.id) it.copy(y = v) else it } }, "উত্তর (Y)", "ফু", Modifier.weight(1f))
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("সীমানা বাহু ও বিয়ারিং নির্দেশনা", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    for (i in pts.indices) {
                        val a = pts[i]
                        val b = pts[(i + 1) % pts.size]
                        val dist = SurveyMath.distanceBetween(a, b)
                        val brg = SurveyMath.bearingDegrees(a, b)
                        Text(
                            "${a.label} → ${b.label}: দূরত্ব ${LandUnitConverter.format(dist, 2)} ফুট | বিয়ারিং ${LandUnitConverter.format(brg, 1)}°",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            AllUnitsResultCard(areaSqFt = area, perimeterFt = perim, onSaveToProject = onSave, onExportPdf = onExportPdf)
        }
    }
}
