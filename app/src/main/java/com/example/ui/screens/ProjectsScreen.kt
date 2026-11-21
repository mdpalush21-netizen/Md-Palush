package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.export.ReportExporter
import com.example.core.math.LandUnitConverter
import com.example.data.local.SurveyProjectEntity
import com.example.data.local.SurveyRepository
import com.example.model.LandConstants
import com.example.ui.components.AllUnitsResultCard
import com.example.ui.components.NokshaPolygonCanvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    projects: List<SurveyProjectEntity>,
    surveyorFooter: String,
    onSaveProject: (SurveyProjectEntity) -> Unit,
    onDuplicateProject: (SurveyProjectEntity) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onLoadSample: () -> Unit,
    onOpenInCad: (SurveyProjectEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("সব") }
    var editingProject by remember { mutableStateOf<SurveyProjectEntity?>(null) }
    var detailProject by remember { mutableStateOf<SurveyProjectEntity?>(null) }

    val filtered = remember(projects, search, statusFilter) {
        projects.filter { p ->
            val matchStatus = statusFilter == "সব" || p.status == statusFilter
            val q = search.trim().lowercase()
            val matchQ = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.ownerName.lowercase().contains(q) ||
                p.mouza.lowercase().contains(q) ||
                p.dagNumber.lowercase().contains(q)
            matchStatus && matchQ
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProject = SurveyProjectEntity(name = "")
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("নতুন প্রজেক্ট") },
                modifier = Modifier.testTag("add_project_fab"),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "সার্ভে প্রজেক্ট সমূহ (${projects.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("প্রজেক্ট, মালিক, মৌজা বা দাগ খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                val statuses = listOf("সব") + LandConstants.projectStatusesBn
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statuses) { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(st) },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp),
                            )
                            Text("কোনো প্রজেক্ট পাওয়া যায়নি", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "নতুন জমির জরিপ শুরু করতে '+' বাটনে চাপুন অথবা একটি নমুনা সার্ভে লোড করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { editingProject = SurveyProjectEntity(name = "") }) {
                                    Text("নতুন প্রজেক্ট")
                                }
                                OutlinedButton(onClick = onLoadSample) {
                                    Text("নমুনা লোড")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { detailProject = project },
                        onEdit = { editingProject = project },
                        onDuplicate = { onDuplicateProject(project) },
                        onDelete = { onDeleteProject(project.id) },
                        onOpenCad = { onOpenInCad(project) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    editingProject?.let { proj ->
        ProjectEditorDialog(
            initial = proj,
            onDismiss = { editingProject = null },
            onSave = { updated ->
                onSaveProject(updated)
                editingProject = null
                if (detailProject?.id == updated.id) detailProject = updated
            },
        )
    }

    detailProject?.let { proj ->
        ProjectDetailDialog(
            project = proj,
            onDismiss = { detailProject = null },
            onEdit = {
                detailProject = null
                editingProject = proj
            },
            onOpenCad = {
                detailProject = null
                onOpenInCad(proj)
            },
            onExportPdf = {
                val pts = SurveyRepository.decodePoints(proj.verticesJson)
                val file = ReportExporter.exportReportPdf(
                    context = context,
                    title = proj.name,
                    subtitle = "মৌজা: ${proj.mouza.ifBlank { "-" }} | খতিয়ান: ${proj.khatianNumber.ifBlank { "-" }} | দাগ: ${proj.dagNumber.ifBlank { "-" }}",
                    metadataLines = listOf(
                        "মালিক" to proj.ownerName.ifBlank { "-" },
                        "পিতা/স্বামী" to proj.fatherOrHusbandName.ifBlank { "-" },
                        "জেলা/উপজেলা" to "${proj.district} ${proj.upazila}".trim().ifBlank { "-" },
                        "জে.এল. / শ্রেণি" to "${proj.jlNumber.ifBlank { "-" }} / ${proj.landClass}",
                        "রেকর্ড ধরন" to proj.recordType,
                        "অবস্থা" to proj.status,
                    ),
                    areaSqFt = proj.areaSqFt,
                    points = pts,
                    extraLines = listOfNotNull(
                        proj.measurementsSummary.takeIf { it.isNotBlank() },
                        proj.partitionSummary.takeIf { it.isNotBlank() },
                        proj.notes.takeIf { it.isNotBlank() },
                    ),
                    surveyorFooter = surveyorFooter,
                )
                ReportExporter.shareFile(context, file, "application/pdf")
            },
            onPrintPdf = {
                val pts = SurveyRepository.decodePoints(proj.verticesJson)
                val file = ReportExporter.exportReportPdf(
                    context = context,
                    title = proj.name,
                    subtitle = "মৌজা: ${proj.mouza.ifBlank { "-" }} | দাগ: ${proj.dagNumber.ifBlank { "-" }}",
                    metadataLines = listOf(
                        "মালিক" to proj.ownerName.ifBlank { "-" },
                        "খতিয়ান/দাগ" to "${proj.khatianNumber}/${proj.dagNumber}",
                    ),
                    areaSqFt = proj.areaSqFt,
                    points = pts,
                    surveyorFooter = surveyorFooter,
                )
                ReportExporter.printPdf(context, file, proj.name)
            },
            onExportJpg = {
                val pts = SurveyRepository.decodePoints(proj.verticesJson)
                val file = ReportExporter.exportNokshaImage(context, proj.name, points = pts, asJpg = true)
                ReportExporter.shareFile(context, file, "image/jpeg")
            },
        )
    }
}

@Composable
private fun ProjectCard(
    project: SurveyProjectEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenCad: () -> Unit,
) {
    val dateStr = remember(project.updatedAt) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(project.updatedAt))
    }
    val shotok = LandUnitConverter.format(project.areaSqFt / 435.6, 2)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = project.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = "মালিক: ${project.ownerName.ifBlank { "উল্লেখ নেই" }} • মৌজা: ${project.mouza.ifBlank { "-" }} • দাগ: ${project.dagNumber.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$shotok শতাংশ (${LandUnitConverter.format(project.areaSqFt, 1)} ব.ফু)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onOpenCad) {
                    Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CAD")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("এডিট")
                }
                TextButton(onClick = onDuplicate) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("কপি")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectEditorDialog(
    initial: SurveyProjectEntity,
    onDismiss: () -> Unit,
    onSave: (SurveyProjectEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var owner by remember { mutableStateOf(initial.ownerName) }
    var father by remember { mutableStateOf(initial.fatherOrHusbandName) }
    var district by remember { mutableStateOf(initial.district) }
    var upazila by remember { mutableStateOf(initial.upazila) }
    var mouza by remember { mutableStateOf(initial.mouza) }
    var jl by remember { mutableStateOf(initial.jlNumber) }
    var khatian by remember { mutableStateOf(initial.khatianNumber) }
    var dag by remember { mutableStateOf(initial.dagNumber) }
    var landClass by remember { mutableStateOf(initial.landClass) }
    var recordType by remember { mutableStateOf(initial.recordType) }
    var areaShotokStr by remember { mutableStateOf(if (initial.areaSqFt > 0) LandUnitConverter.format(initial.areaSqFt / 435.6, 2) else "") }
    var status by remember { mutableStateOf(initial.status) }
    var notes by remember { mutableStateOf(initial.notes) }
    var photoUris by remember { mutableStateOf(SurveyRepository.decodeStringList(initial.deedPhotoUrisJson)) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { photoUris = photoUris + it.toString() }
        },
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "নতুন সার্ভে প্রজেক্ট" else "প্রজেক্ট সম্পাদনা") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("প্রজেক্টের নাম *") }, singleLine = true)
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("জমির মালিকের নাম") }, singleLine = true)
                OutlinedTextField(value = father, onValueChange = { father = it }, label = { Text("পিতা / স্বামীর নাম") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("উপজেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = jl, onValueChange = { jl = it }, label = { Text("জে.এল. নং") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = khatian, onValueChange = { khatian = it }, label = { Text("খতিয়ান নং") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dag, onValueChange = { dag = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = areaShotokStr,
                    onValueChange = { areaShotokStr = it },
                    label = { Text("জমির পরিমাণ (শতাংশ)") },
                    singleLine = true,
                )
                Text("রেকর্ড ধরন", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LandConstants.recordTypes) { rt ->
                        FilterChip(selected = recordType == rt, onClick = { recordType = rt }, label = { Text(rt) })
                    }
                }
                Text("জমির শ্রেণি", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LandConstants.landClassesBn) { lc ->
                        FilterChip(selected = landClass == lc, onClick = { landClass = lc }, label = { Text(lc) })
                    }
                }
                Text("কাজের অবস্থা", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LandConstants.projectStatusesBn) { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("মন্তব্য / নোট") }, maxLines = 3)

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("দলিল / কাগজপত্রের ছবি যোগ (${photoUris.size})")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val shotok = areaShotokStr.toDoubleOrNull() ?: 0.0
                val sqFt = if (shotok > 0) shotok * 435.6 else initial.areaSqFt
                onSave(
                    initial.copy(
                        name = name.ifBlank { "সার্ভে প্রজেক্ট" },
                        ownerName = owner,
                        fatherOrHusbandName = father,
                        district = district,
                        upazila = upazila,
                        mouza = mouza,
                        jlNumber = jl,
                        khatianNumber = khatian,
                        dagNumber = dag,
                        landClass = landClass,
                        recordType = recordType,
                        areaSqFt = sqFt,
                        status = status,
                        notes = notes,
                        deedPhotoUrisJson = SurveyRepository.encodeStringList(photoUris),
                    )
                )
            }) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
private fun ProjectDetailDialog(
    project: SurveyProjectEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOpenCad: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onExportJpg: () -> Unit,
) {
    val pts = remember(project.verticesJson) { SurveyRepository.decodePoints(project.verticesJson) }
    val photos = remember(project.deedPhotoUrisJson) { SurveyRepository.decodeStringList(project.deedPhotoUrisJson) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(project.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (pts.size >= 2) {
                    NokshaPolygonCanvas(points = pts, height = 180.dp)
                }
                AllUnitsResultCard(areaSqFt = project.areaSqFt, perimeterFt = project.perimeterFt)
                if (project.measurementsSummary.isNotBlank()) {
                    Text("পরিমাপ: ${project.measurementsSummary}", style = MaterialTheme.typography.bodySmall)
                }
                if (project.partitionSummary.isNotBlank()) {
                    Text("ভাগ-বণ্টন: ${project.partitionSummary}", style = MaterialTheme.typography.bodySmall)
                }
                if (photos.isNotEmpty()) {
                    Text("সংযুক্ত দলিল ও কাগজপত্র", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photos) { uriStr ->
                            AsyncImage(
                                model = uriStr,
                                contentDescription = "দলিলের ছবি",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) { Text("PDF") }
                    OutlinedButton(onClick = onPrintPdf, modifier = Modifier.weight(1f)) { Text("প্রিন্ট") }
                    OutlinedButton(onClick = onExportJpg, modifier = Modifier.weight(1f)) { Text("JPG") }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenCad) { Text("CAD এ খুলুন") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit) { Text("এডিট") }
                TextButton(onClick = onDismiss) { Text("বন্ধ") }
            }
        },
    )
}
