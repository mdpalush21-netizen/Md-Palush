package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.math.*
import com.example.model.SurveyToolId
import com.example.ui.components.*

@Composable
fun CalculatorsScreen(
    tool: SurveyToolId,
    surveyorFooter: String,
    onBack: () -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, pts: List<SurveyPoint2D>, summary: String) -> Unit,
) {
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var activeAreaSqFt by remember { mutableDoubleStateOf(0.0) }
    var activePoints by remember { mutableStateOf<List<SurveyPoint2D>>(emptyList()) }
    var activeSummary by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SurveyTopBar(
                title = tool.titleBn,
                subtitle = tool.subtitleBn,
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tool) {
                SurveyToolId.AREA_CALCULATOR -> AreaCalculatorBody(
                    onAreaComputed = { area, summary ->
                        activeAreaSqFt = area
                        activeSummary = summary
                    },
                    onSaveClick = { showSaveDialog = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "একক রূপান্তর ও ক্ষেত্রফল",
                            listOf("বিবরণ" to activeSummary), activeAreaSqFt, emptyList(),
                            surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.LENGTH_WIDTH -> LengthWidthBody(
                    onAreaComputed = { area, pts, summary ->
                        activeAreaSqFt = area
                        activePoints = pts
                        activeSummary = summary
                    },
                    onSaveClick = { showSaveDialog = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "দৈর্ঘ্য ও প্রস্থ পরিমাপ",
                            listOf("হিসাব" to activeSummary), activeAreaSqFt, activePoints,
                            surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.QUAD_MEASURE, SurveyToolId.EXACT_QUAD -> QuadrilateralBody(
                    isExactDefault = tool == SurveyToolId.EXACT_QUAD,
                    onResult = { area, pts, summary ->
                        activeAreaSqFt = area
                        activePoints = pts
                        activeSummary = summary
                    },
                    onSaveClick = { showSaveDialog = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "চতুর্ভুজ জমি পরিমাপ",
                            listOf("বাহুসমূহ" to activeSummary), activeAreaSqFt, activePoints,
                            surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.TRIANGLE_MEASURE -> TriangleBody(
                    onResult = { area, pts, summary ->
                        activeAreaSqFt = area
                        activePoints = pts
                        activeSummary = summary
                    },
                    onSaveClick = { showSaveDialog = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "ত্রিভুজ জমি পরিমাপ (হেরন সূত্র)",
                            listOf("বাহু" to activeSummary), activeAreaSqFt, activePoints,
                            surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.MULTI_TRIANGLE -> MultiTriangleBody(
                    onResult = { area, summary ->
                        activeAreaSqFt = area
                        activeSummary = summary
                    },
                    onSaveClick = { showSaveDialog = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context, tool.titleBn, "একাধিক ত্রিভুজ পরিমাপ",
                            listOf("বিবরণ" to activeSummary), activeAreaSqFt, emptyList(),
                            surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
                SurveyToolId.AUTO_CORNER -> AutoCornerBody()
                SurveyToolId.ANA_GONDA_CALC -> AnaGondaBody()
                else -> Unit
            }
        }
    }

    if (showSaveDialog) {
        SaveToProjectDialog(
            defaultTitle = "${tool.titleBn} প্রজেক্ট",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(name, owner, mouza, dag, khatian, activeAreaSqFt, activePoints, activeSummary)
                showSaveDialog = false
            },
        )
    }
}

@Composable
private fun AreaCalculatorBody(
    onAreaComputed: (Double, String) -> Unit,
    onSaveClick: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var inputStr by remember { mutableStateOf("10") }
    var selectedUnit by remember { mutableStateOf(LandUnit.SHOTOK) }
    val valNum = inputStr.toDoubleOrNull() ?: 0.0
    val sqFt = LandUnitConverter.toSqFeet(valNum, selectedUnit)

    LaunchedEffect(sqFt, selectedUnit, inputStr) {
        onAreaComputed(sqFt, "$inputStr ${selectedUnit.titleBn}")
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("একক নির্বাচন ও মান লিখুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    NumberInputField(
                        value = inputStr,
                        onValueChange = { inputStr = it },
                        label = "জমির পরিমাণ",
                        suffix = selectedUnit.shortBn,
                        modifier = Modifier.testTag("area_input_field"),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LandUnit.entries) { unit ->
                            FilterChip(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit },
                                label = { Text(unit.titleBn) },
                            )
                        }
                    }
                }
            }
        }

        item {
            AllUnitsResultCard(
                areaSqFt = sqFt,
                onSaveToProject = onSaveClick,
                onExportPdf = onExportPdf,
            )
        }
    }
}

@Composable
private fun LengthWidthBody(
    onAreaComputed: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSaveClick: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var lenVal by remember { mutableStateOf("80") }
    var lenInch by remember { mutableStateOf("0") }
    var widVal by remember { mutableStateOf("54") }
    var widInch by remember { mutableStateOf("6") }
    var unit by remember { mutableStateOf(LinearUnit.FEET) }

    val lenFt = if (unit == LinearUnit.FEET) {
        LandUnitConverter.feetInchToFeet(lenVal.toDoubleOrNull() ?: 0.0, lenInch.toDoubleOrNull() ?: 0.0)
    } else {
        LandUnitConverter.toFeet(lenVal.toDoubleOrNull() ?: 0.0, unit)
    }
    val widFt = if (unit == LinearUnit.FEET) {
        LandUnitConverter.feetInchToFeet(widVal.toDoubleOrNull() ?: 0.0, widInch.toDoubleOrNull() ?: 0.0)
    } else {
        LandUnitConverter.toFeet(widVal.toDoubleOrNull() ?: 0.0, unit)
    }

    val areaSqFt = lenFt * widFt
    val pts = remember(lenFt, widFt) {
        if (lenFt > 0 && widFt > 0) {
            listOf(
                SurveyPoint2D(0.0, 0.0, "P1"),
                SurveyPoint2D(lenFt, 0.0, "P2"),
                SurveyPoint2D(lenFt, widFt, "P3"),
                SurveyPoint2D(0.0, widFt, "P4"),
            )
        } else emptyList()
    }

    LaunchedEffect(areaSqFt, pts) {
        onAreaComputed(
            areaSqFt,
            pts,
            "দৈর্ঘ্য ${LandUnitConverter.format(lenFt, 2)} ফুট × প্রস্থ ${LandUnitConverter.format(widFt, 2)} ফুট",
        )
    }

    LazyColumn(
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
                    Text("মাপের একক নির্বাচন করুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LinearUnit.entries.filter { it != LinearUnit.INCH }) { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.titleBn) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(lenVal, { lenVal = it }, "দৈর্ঘ্য", unit.titleBn, Modifier.weight(1f))
                        if (unit == LinearUnit.FEET) {
                            NumberInputField(lenInch, { lenInch = it }, "ইঞ্চি", "ইঞ্চি", Modifier.weight(0.7f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(widVal, { widVal = it }, "প্রস্থ", unit.titleBn, Modifier.weight(1f))
                        if (unit == LinearUnit.FEET) {
                            NumberInputField(widInch, { widInch = it }, "ইঞ্চি", "ইঞ্চি", Modifier.weight(0.7f))
                        }
                    }
                    Text(
                        text = "ফুটে রূপান্তর: ${LandUnitConverter.format(lenFt, 2)}' × ${LandUnitConverter.format(widFt, 2)}' | মিটারে: ${LandUnitConverter.format(lenFt * 0.3048, 2)}m × ${LandUnitConverter.format(widFt * 0.3048, 2)}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (pts.isNotEmpty()) {
            item { NokshaPolygonCanvas(points = pts, height = 190.dp) }
        }

        item {
            AllUnitsResultCard(
                areaSqFt = areaSqFt,
                perimeterFt = (lenFt + widFt) * 2.0,
                onSaveToProject = onSaveClick,
                onExportPdf = onExportPdf,
            )
        }
    }
}

@Composable
private fun QuadrilateralBody(
    isExactDefault: Boolean,
    onResult: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSaveClick: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var north by remember { mutableStateOf("84") }
    var south by remember { mutableStateOf("92") }
    var east by remember { mutableStateOf("60") }
    var west by remember { mutableStateOf("56") }
    var diag by remember { mutableStateOf(if (isExactDefault) "105" else "") }
    var useExact by remember { mutableStateOf(isExactDefault) }

    val n = north.toDoubleOrNull() ?: 0.0
    val s = south.toDoubleOrNull() ?: 0.0
    val e = east.toDoubleOrNull() ?: 0.0
    val w = west.toDoubleOrNull() ?: 0.0
    val d = diag.toDoubleOrNull()

    val res = remember(n, s, e, w, d, useExact) {
        SurveyMath.quadrilateralExact(n, s, e, w, if (useExact) d else null)
    }
    val displayArea = if (useExact && res.isValid) res.exactAreaSqFt else res.averageMethodAreaSqFt

    LaunchedEffect(displayArea, res) {
        onResult(displayArea, res.vertices, "উ:$north' দ:$south' পূ:$east' প:$west' কর্ণ:${diag.ifBlank { "অটো" }}'")
    }

    LazyColumn(
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !useExact,
                            onClick = { useExact = false },
                            label = { Text("গড় পদ্ধতি (আমিন)") },
                        )
                        FilterChip(
                            selected = useExact,
                            onClick = { useExact = true },
                            label = { Text("নিখুঁত কর্ণ পদ্ধতি") },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberInputField(north, { north = it }, "উত্তর বাহু", "ফুট", Modifier.weight(1f))
                        NumberInputField(south, { south = it }, "দক্ষিণ বাহু", "ফুট", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberInputField(east, { east = it }, "পূর্ব বাহু", "ফুট", Modifier.weight(1f))
                        NumberInputField(west, { west = it }, "পশ্চিম বাহু", "ফুট", Modifier.weight(1f))
                    }
                    if (useExact) {
                        NumberInputField(diag, { diag = it }, "কর্ণ (SW থেকে NE কোণা)", "ফুট")
                    }
                    res.errorReason?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (res.isValid) {
                        Text(
                            text = "গড় ও নিখুঁত হিসাবের পার্থক্য: ${LandUnitConverter.format(res.differenceSqFt, 2)} বর্গফুট (${LandUnitConverter.format(res.differenceSqFt / 435.6, 3)} শতাংশ)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (res.isValid) {
            item { NokshaPolygonCanvas(points = res.vertices, height = 210.dp) }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("কোণ: NW ${LandUnitConverter.format(res.angleNW, 1)}°", style = MaterialTheme.typography.labelMedium)
                        Text("NE ${LandUnitConverter.format(res.angleNE, 1)}°", style = MaterialTheme.typography.labelMedium)
                        Text("SE ${LandUnitConverter.format(res.angleSE, 1)}°", style = MaterialTheme.typography.labelMedium)
                        Text("SW ${LandUnitConverter.format(res.angleSW, 1)}°", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            AllUnitsResultCard(
                areaSqFt = displayArea,
                perimeterFt = res.perimeterFt,
                onSaveToProject = onSaveClick,
                onExportPdf = onExportPdf,
            )
        }
    }
}

@Composable
private fun TriangleBody(
    onResult: (Double, List<SurveyPoint2D>, String) -> Unit,
    onSaveClick: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var aStr by remember { mutableStateOf("60") }
    var bStr by remember { mutableStateOf("50") }
    var cStr by remember { mutableStateOf("45") }

    val res = remember(aStr, bStr, cStr) {
        SurveyMath.heronTriangleArea(
            aStr.toDoubleOrNull() ?: 0.0,
            bStr.toDoubleOrNull() ?: 0.0,
            cStr.toDoubleOrNull() ?: 0.0,
        )
    }

    LaunchedEffect(res) {
        onResult(res.areaSqFt, res.vertices, "বাহু: $aStr', $bStr', $cStr'")
    }

    LazyColumn(
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
                    Text("ত্রিভুজের তিনটি বাহু (ফুট)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(aStr, { aStr = it }, "বাহু A", "ফুট", Modifier.weight(1f))
                        NumberInputField(bStr, { bStr = it }, "বাহু B", "ফুট", Modifier.weight(1f))
                        NumberInputField(cStr, { cStr = it }, "বাহু C", "ফুট", Modifier.weight(1f))
                    }
                    res.errorReason?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (res.isValid) {
                        Text(
                            "কোণ: A=${LandUnitConverter.format(res.angleADeg, 1)}°, B=${LandUnitConverter.format(res.angleBDeg, 1)}°, C=${LandUnitConverter.format(res.angleCDeg, 1)}° | উচ্চতা=${LandUnitConverter.format(res.altitudeOnA, 2)}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (res.isValid) {
            item { NokshaPolygonCanvas(points = res.vertices, height = 210.dp) }
        }
        item {
            AllUnitsResultCard(
                areaSqFt = res.areaSqFt,
                perimeterFt = res.perimeterFt,
                onSaveToProject = onSaveClick,
                onExportPdf = onExportPdf,
            )
        }
    }
}

private data class TriInput(val id: Int, val a: String, val b: String, val c: String)

@Composable
private fun MultiTriangleBody(
    onResult: (Double, String) -> Unit,
    onSaveClick: () -> Unit,
    onExportPdf: () -> Unit,
) {
    var triangles by remember {
        mutableStateOf(
            listOf(
                TriInput(1, "60", "50", "45"),
                TriInput(2, "50", "48", "40"),
            )
        )
    }

    val results = remember(triangles) {
        triangles.map { t ->
            t to SurveyMath.heronTriangleArea(
                t.a.toDoubleOrNull() ?: 0.0,
                t.b.toDoubleOrNull() ?: 0.0,
                t.c.toDoubleOrNull() ?: 0.0,
            )
        }
    }
    val totalArea = results.sumOf { it.second.areaSqFt }

    LaunchedEffect(totalArea, triangles) {
        onResult(totalArea, "${triangles.size} টি ত্রিভুজের সমষ্টি")
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(results, key = { it.first.id }) { (tri, res) ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("ত্রিভুজ #${tri.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${LandUnitConverter.format(res.areaSqFt / 435.6, 3)} শতক",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (triangles.size > 1) {
                                IconButton(onClick = { triangles = triangles.filterNot { it.id == tri.id } }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            tri.a,
                            { v -> triangles = triangles.map { if (it.id == tri.id) it.copy(a = v) else it } },
                            "বাহু A", "ফুট", Modifier.weight(1f),
                        )
                        NumberInputField(
                            tri.b,
                            { v -> triangles = triangles.map { if (it.id == tri.id) it.copy(b = v) else it } },
                            "বাহু B", "ফুট", Modifier.weight(1f),
                        )
                        NumberInputField(
                            tri.c,
                            { v -> triangles = triangles.map { if (it.id == tri.id) it.copy(c = v) else it } },
                            "বাহু C", "ফুট", Modifier.weight(1f),
                        )
                    }
                    res.errorReason?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val nextId = (triangles.maxOfOrNull { it.id } ?: 0) + 1
                    triangles = triangles + TriInput(nextId, "40", "35", "30")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("আরও ত্রিভুজ যোগ করুন")
            }
        }

        item {
            AllUnitsResultCard(
                areaSqFt = totalArea,
                title = "সব ত্রিভুজের মোট ক্ষেত্রফল",
                onSaveToProject = onSaveClick,
                onExportPdf = onExportPdf,
            )
        }
    }
}

@Composable
private fun AutoCornerBody() {
    var sideA by remember { mutableStateOf("40") }
    var sideB by remember { mutableStateOf("30") }
    var angleStr by remember { mutableStateOf("90") }

    val a = sideA.toDoubleOrNull() ?: 0.0
    val b = sideB.toDoubleOrNull() ?: 0.0
    val ang = (angleStr.toDoubleOrNull() ?: 90.0).coerceIn(1.0, 179.0)
    val diag = SurveyMath.diagonalForAngle(a, b, ang)
    val rightAngleDiag = SurveyMath.diagonalForAngle(a, b, 90.0)

    LazyColumn(
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
                    Text("কর্ণ ও সমকোণ (৩-৪-৫) ক্যালকুলেশন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(sideA, { sideA = it }, "প্রথম বাহু", "ফুট", Modifier.weight(1f))
                        NumberInputField(sideB, { sideB = it }, "দ্বিতীয় বাহু", "ফুট", Modifier.weight(1f))
                    }
                    NumberInputField(angleStr, { angleStr = it }, "কোণ (ডিগ্রি)", "°")
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "প্রয়োজনীয় কর্ণ ($ang° কোণে): ${LandUnitConverter.format(diag, 3)} ফুট",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "৯০° নিখুঁত সমকোণের জন্য কর্ণ: ${LandUnitConverter.format(rightAngleDiag, 3)} ফুট",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "মাঠ পর্যায়ে ৩-৪-৫ গুণিয়া চেক: ${(a * 0.3).toInt()}-${(a * 0.4).toInt()}-${(a * 0.5).toInt()} বা ১২-১৬-২০ ফুটে কর্ণ ২০ ফুট হবে।",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HeirRow(val id: Int, val name: String, val ana: String, val gonda: String, val kora: String, val kranti: String, val til: String)

@Composable
private fun AnaGondaBody() {
    var totalShotok by remember { mutableStateOf("100") }
    var rows by remember {
        mutableStateOf(
            listOf(
                HeirRow(1, "অংশীদার ১", "8", "0", "0", "0", "0"),
                HeirRow(2, "অংশীদার ২", "5", "10", "2", "0", "0"),
                HeirRow(3, "অংশীদার ৩", "2", "9", "2", "0", "0"),
            )
        )
    }

    val totalSqFt = LandUnitConverter.toSqFeet(totalShotok.toDoubleOrNull() ?: 0.0, LandUnit.SHOTOK)
    val shares = rows.map { r ->
        r to AnaGondaShare(
            ana = r.ana.toIntOrNull() ?: 0,
            gonda = r.gonda.toIntOrNull() ?: 0,
            kora = r.kora.toIntOrNull() ?: 0,
            kranti = r.kranti.toIntOrNull() ?: 0,
            til = r.til.toIntOrNull() ?: 0,
        )
    }
    val totalFrac = shares.sumOf { it.second.fractionOfWhole }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(totalShotok, { totalShotok = it }, "মোট জমির পরিমাণ", "শতাংশ")
                    Text(
                        text = "মোট হিস্যা: ${AnaGondaShare.fromFraction(totalFrac.coerceAtMost(1.0)).formatBn()} (${LandUnitConverter.format(totalFrac * 100, 2)}%)",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (kotlin.math.abs(totalFrac - 1.0) < 0.001) Color(0xFF16A34A) else Color(0xFFEA580C),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        items(shares, key = { it.first.id }) { (row, share) ->
            val mySqFt = totalSqFt * share.fractionOfWhole
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = row.name,
                            onValueChange = { v -> rows = rows.map { if (it.id == row.id) it.copy(name = v) else it } },
                            label = { Text("অংশীদার") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (rows.size > 1) {
                            IconButton(onClick = { rows = rows.filterNot { it.id == row.id } }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberInputField(row.ana, { v -> rows = rows.map { if (it.id == row.id) it.copy(ana = v) else it } }, "আনা", "", Modifier.weight(1f))
                        NumberInputField(row.gonda, { v -> rows = rows.map { if (it.id == row.id) it.copy(gonda = v) else it } }, "গণ্ডা", "", Modifier.weight(1f))
                        NumberInputField(row.kora, { v -> rows = rows.map { if (it.id == row.id) it.copy(kora = v) else it } }, "কড়া", "", Modifier.weight(1f))
                        NumberInputField(row.kranti, { v -> rows = rows.map { if (it.id == row.id) it.copy(kranti = v) else it } }, "ক্রান্তি", "", Modifier.weight(1f))
                        NumberInputField(row.til, { v -> rows = rows.map { if (it.id == row.id) it.copy(til = v) else it } }, "তিল", "", Modifier.weight(1f))
                    }
                    Text(
                        text = "প্রাপ্য: ${LandUnitConverter.format(mySqFt / 435.6, 3)} শতাংশ | ${LandUnitConverter.format(mySqFt / 720.0, 3)} কাঠা (${LandUnitConverter.format(share.percentage, 2)}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val next = (rows.maxOfOrNull { it.id } ?: 0) + 1
                    rows = rows + HeirRow(next, "অংশীদার $next", "0", "0", "0", "0", "0")
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("অংশীদার যোগ করুন")
            }
        }
    }
}
