package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.export.ReportExporter
import com.example.core.math.*
import com.example.ui.components.*
import com.example.ui.theme.PartitionPalette

private data class PartnerUiState(
    val id: String,
    val name: String,
    val shareStr: String,
    val anaStr: String = "4",
    val gondaStr: String = "0",
    val colorIndex: Int,
)

@Composable
fun PartitionScreen(
    surveyorFooter: String,
    onBack: () -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, summary: String, partitionText: String) -> Unit,
) {
    val context = LocalContext.current
    var lengthStr by remember { mutableStateOf("120") }
    var widthStr by remember { mutableStateOf("80") }
    var khatianNo by remember { mutableStateOf("") }
    var dagNo by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PartitionMode.EQUAL) }
    var roadPlacement by remember { mutableStateOf(RoadPlacement.ONE_SIDE) }
    var roadWidthStr by remember { mutableStateOf("12") }
    var cutAlongLength by remember { mutableStateOf(true) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var partners by remember {
        mutableStateOf(
            listOf(
                PartnerUiState("p1", "অংশীদার ১", "50", "8", "0", 0),
                PartnerUiState("p2", "অংশীদার ২", "30", "5", "0", 1),
                PartnerUiState("p3", "অংশীদার ৩", "20", "3", "0", 2),
            )
        )
    }

    val plan = remember(lengthStr, widthStr, partners, mode, roadWidthStr, roadPlacement, cutAlongLength) {
        SurveyMath.partitionLand(
            totalLengthFt = lengthStr.toDoubleOrNull() ?: 100.0,
            totalWidthFt = widthStr.toDoubleOrNull() ?: 60.0,
            partners = partners.map { p ->
                PartnerInput(
                    id = p.id,
                    name = p.name.ifBlank { "অংশীদার" },
                    shareValue = p.shareStr.toDoubleOrNull() ?: 1.0,
                    anaGonda = AnaGondaShare(
                        ana = p.anaStr.toIntOrNull() ?: 0,
                        gonda = p.gondaStr.toIntOrNull() ?: 0,
                    ),
                    color = PartitionPalette[p.colorIndex % PartitionPalette.size],
                )
            },
            mode = mode,
            roadWidthFt = roadWidthStr.toDoubleOrNull() ?: 0.0,
            roadPlacement = roadPlacement,
            cutAlongLength = cutAlongLength,
        )
    }

    val partitionSummaryText = remember(plan) {
        plan.plots.joinToString(" | ") { plot ->
            "${plot.name}: ${LandUnitConverter.format(plot.areaSqFt / 435.6, 2)} শতক"
        }
    }

    Scaffold(
        topBar = {
            SurveyTopBar(
                title = "জমি ভাগ-বণ্টন",
                subtitle = "অংশীদার, রাস্তা ও রঙিন নকশা",
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Live Color-Coded Partition Noksha
            item {
                PartitionNokshaCanvas(plan = plan, height = 240.dp)
            }

            // Summary Banner
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "মোট জমি: ${LandUnitConverter.format(plan.grossAreaSqFt / 435.6, 2)} শতাংশ | নিট বণ্টনযোগ্য: ${LandUnitConverter.format(plan.netAreaSqFt / 435.6, 2)} শতাংশ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (plan.roadAreaSqFt > 0) {
                            Text(
                                text = "রাস্তায় ব্যবহৃত জমি: ${LandUnitConverter.format(plan.roadAreaSqFt / 435.6, 2)} শতাংশ (${LandUnitConverter.format(plan.roadAreaSqFt, 1)} ব.ফু)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        plan.warningMessage?.let { warn ->
                            Text(
                                text = warn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Land Dimensions & Road Configuration
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("জমির মাপ, খতিয়ান ও রাস্তা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField(lengthStr, { lengthStr = it }, "মোট দৈর্ঘ্য", "ফুট", Modifier.weight(1f))
                            NumberInputField(widthStr, { widthStr = it }, "মোট প্রস্থ", "ফুট", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = khatianNo, onValueChange = { khatianNo = it },
                                label = { Text("খতিয়ান নং") }, singleLine = true, modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = dagNo, onValueChange = { dagNo = it },
                                label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f),
                            )
                        }

                        Text("ভাগের ধরন", style = MaterialTheme.typography.labelLarge)
                        val modes = listOf(
                            PartitionMode.EQUAL to "সমান ভাগ",
                            PartitionMode.PERCENTAGE to "শতাংশ %",
                            PartitionMode.RATIO to "অনুপাত",
                            PartitionMode.CUSTOM_SHOTOK to "নির্দিষ্ট শতক",
                            PartitionMode.ANA_GONDA to "আনা-গণ্ডা",
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(modes) { (m, lbl) ->
                                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(lbl) })
                            }
                        }

                        Text("রাস্তার অবস্থান ও কাটার দিক", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = roadPlacement == RoadPlacement.NONE,
                                    onClick = { roadPlacement = RoadPlacement.NONE },
                                    label = { Text("রাস্তা ছাড়া") },
                                )
                            }
                            item {
                                FilterChip(
                                    selected = roadPlacement == RoadPlacement.ONE_SIDE,
                                    onClick = { roadPlacement = RoadPlacement.ONE_SIDE },
                                    label = { Text("এক পাশে রাস্তা") },
                                )
                            }
                            item {
                                FilterChip(
                                    selected = roadPlacement == RoadPlacement.CENTER_BOTH_SIDES,
                                    onClick = { roadPlacement = RoadPlacement.CENTER_BOTH_SIDES },
                                    label = { Text("রাস্তার দুই পাশে ভাগ") },
                                )
                            }
                        }
                        if (roadPlacement != RoadPlacement.NONE) {
                            NumberInputField(roadWidthStr, { roadWidthStr = it }, "রাস্তার প্রস্থ", "ফুট")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = cutAlongLength, onClick = { cutAlongLength = true }, label = { Text("দৈর্ঘ্য বরাবর ভাগ") })
                            FilterChip(selected = !cutAlongLength, onClick = { cutAlongLength = false }, label = { Text("প্রস্থ বরাবর ভাগ") })
                        }
                    }
                }
            }

            // Partners Input List
            itemsIndexed(partners, key = { _, p -> p.id }) { idx, partner ->
                val color = PartitionPalette[partner.colorIndex % PartitionPalette.size]
                val plotRes = plan.plots.firstOrNull { it.partnerId == partner.id }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            OutlinedTextField(
                                value = partner.name,
                                onValueChange = { v -> partners = partners.map { if (it.id == partner.id) it.copy(name = v) else it } },
                                label = { Text("অংশীদার #${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (partners.size > 1) {
                                IconButton(onClick = { partners = partners.filterNot { it.id == partner.id } }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        when (mode) {
                            PartitionMode.EQUAL -> Unit
                            PartitionMode.PERCENTAGE -> NumberInputField(
                                partner.shareStr,
                                { v -> partners = partners.map { if (it.id == partner.id) it.copy(shareStr = v) else it } },
                                "অংশ (%)", "%",
                            )
                            PartitionMode.RATIO -> NumberInputField(
                                partner.shareStr,
                                { v -> partners = partners.map { if (it.id == partner.id) it.copy(shareStr = v) else it } },
                                "অনুপাত সংখ্যা", "অংশ",
                            )
                            PartitionMode.CUSTOM_SHOTOK -> NumberInputField(
                                partner.shareStr,
                                { v -> partners = partners.map { if (it.id == partner.id) it.copy(shareStr = v) else it } },
                                "জমির পরিমাণ", "শতাংশ",
                            )
                            PartitionMode.ANA_GONDA -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberInputField(
                                    partner.anaStr,
                                    { v -> partners = partners.map { if (it.id == partner.id) it.copy(anaStr = v) else it } },
                                    "আনা", "", Modifier.weight(1f),
                                )
                                NumberInputField(
                                    partner.gondaStr,
                                    { v -> partners = partners.map { if (it.id == partner.id) it.copy(gondaStr = v) else it } },
                                    "গণ্ডা", "", Modifier.weight(1f),
                                )
                            }
                        }

                        plotRes?.let { plot ->
                            Text(
                                text = "প্রাপ্য (${plot.sideLabel}): ${LandUnitConverter.format(plot.areaSqFt / 435.6, 3)} শতাংশ | ${LandUnitConverter.format(plot.areaSqFt / 720.0, 3)} কাঠা (${LandUnitConverter.format(plot.widthFt, 1)}' × ${LandUnitConverter.format(plot.heightFt, 1)}')",
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val nextIdx = partners.size
                        partners = partners + PartnerUiState(
                            id = "p_${System.currentTimeMillis()}",
                            name = "অংশীদার ${nextIdx + 1}",
                            shareStr = "25",
                            colorIndex = nextIdx,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_partner_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("নতুন অংশীদার যোগ করুন")
                }
            }

            // Export & Save Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("save_partition_project"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("প্রজেক্টে সেভ")
                        }
                        Button(
                            onClick = {
                                val file = ReportExporter.exportReportPdf(
                                    context = context,
                                    title = "জমি ভাগ-বণ্টন রিপোর্ট",
                                    subtitle = "খতিয়ান: ${khatianNo.ifBlank { "-" }} | দাগ: ${dagNo.ifBlank { "-" }}",
                                    metadataLines = listOf(
                                        "মোট মাপ" to "${lengthStr}' × ${widthStr}'",
                                        "রাস্তা" to "${roadWidthStr}' (${roadPlacement.name})",
                                        "খতিয়ান নং" to khatianNo.ifBlank { "-" },
                                        "দাগ নং" to dagNo.ifBlank { "-" },
                                    ),
                                    areaSqFt = plan.netAreaSqFt,
                                    partitionPlan = plan,
                                    surveyorFooter = surveyorFooter,
                                )
                                ReportExporter.shareFile(context, file, "application/pdf")
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_partition_pdf"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF রিপোর্ট")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val file = ReportExporter.exportNokshaImage(context, "জমি ভাগ-বণ্টন নকশা", partitionPlan = plan, asJpg = true)
                                ReportExporter.shareFile(context, file, "image/jpeg")
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("JPG নকশা")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = ReportExporter.exportReportPdf(
                                    context = context,
                                    title = "জমি ভাগ-বণ্টন রিপোর্ট",
                                    subtitle = "খতিয়ান: ${khatianNo.ifBlank { "-" }} | দাগ: ${dagNo.ifBlank { "-" }}",
                                    metadataLines = listOf("মোট মাপ" to "${lengthStr}' × ${widthStr}'"),
                                    areaSqFt = plan.netAreaSqFt,
                                    partitionPlan = plan,
                                    surveyorFooter = surveyorFooter,
                                )
                                ReportExporter.printPdf(context, file, "LandPartitionReport")
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("প্রিন্ট (A4)")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showSaveDialog) {
        SaveToProjectDialog(
            defaultTitle = "ভাগ-বণ্টন প্রজেক্ট (দাগ ${dagNo.ifBlank { "নতুন" }})",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(
                    name, owner, mouza, dag.ifBlank { dagNo }, khatian.ifBlank { khatianNo },
                    plan.netAreaSqFt,
                    "মোট ${lengthStr}' × ${widthStr}' | রাস্তা ${roadWidthStr}'",
                    partitionSummaryText,
                )
                showSaveDialog = false
            },
        )
    }
}
