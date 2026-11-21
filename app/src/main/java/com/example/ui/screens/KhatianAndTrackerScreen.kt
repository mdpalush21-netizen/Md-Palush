package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.math.LandUnitConverter
import com.example.data.local.CsBsRecordEntity
import com.example.data.local.KhatianRecordEntity
import com.example.data.local.SurveyRepository
import com.example.model.LandConstants
import com.example.ui.components.SurveyTopBar

@Composable
fun KhatianScreen(
    khatians: List<KhatianRecordEntity>,
    onSaveKhatian: (KhatianRecordEntity) -> Unit,
    onDeleteKhatian: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<KhatianRecordEntity?>(null) }

    val filtered = remember(khatians, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) khatians
        else khatians.filter {
            it.ownerName.lowercase().contains(q) ||
                it.mouza.lowercase().contains(q) ||
                it.khatianNumber.lowercase().contains(q) ||
                it.dagNumber.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            SurveyTopBar(title = "সার্ভে খতিয়ান ও দাগ", subtitle = "CS, SA, RS, BS, BRS ডিজিটাল ফাইল", onBack = onBack)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = KhatianRecordEntity(ownerName = "") },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("নতুন খতিয়ান") },
                modifier = Modifier.testTag("add_khatian_fab"),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("মালিক, মৌজা, খতিয়ান বা দাগ নম্বর খুঁজুন...") },
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

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Text("কোনো খতিয়ান সংরক্ষিত নেই", style = MaterialTheme.typography.titleMedium)
                            Text("মালিকের নাম, মৌজা, জে.এল., খতিয়ান, দাগ ও দলিলের ছবি সংরক্ষণ করতে '+' চাপুন।", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { rec ->
                    val photos = remember(rec.photoUrisJson) { SurveyRepository.decodeStringList(rec.photoUrisJson) }
                    Surface(
                        onClick = { editing = rec },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(rec.ownerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(rec.recordType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("পিতা/স্বামী: ${rec.fatherOrHusbandName.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                            Text("জেলা: ${rec.district} • উপজেলা: ${rec.upazila} • মৌজা: ${rec.mouza} (JL: ${rec.jlNumber})", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "খতিয়ান: ${rec.khatianNumber} | দাগ: ${rec.dagNumber} | শ্রেণি: ${rec.landClass} | পরিমাণ: ${LandUnitConverter.format(rec.areaShotok, 2)} শতাংশ",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (photos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(photos) { uri ->
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "দলিল ছবি",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                        )
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = rec }) { Text("এডিট") }
                                TextButton(
                                    onClick = { onDeleteKhatian(rec.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) { Text("ডিলিট") }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    editing?.let { rec ->
        KhatianEditorDialog(
            initial = rec,
            onDismiss = { editing = null },
            onSave = {
                onSaveKhatian(it)
                editing = null
            },
        )
    }
}

@Composable
private fun KhatianEditorDialog(
    initial: KhatianRecordEntity,
    onDismiss: () -> Unit,
    onSave: (KhatianRecordEntity) -> Unit,
) {
    var owner by remember { mutableStateOf(initial.ownerName) }
    var father by remember { mutableStateOf(initial.fatherOrHusbandName) }
    var district by remember { mutableStateOf(initial.district) }
    var upazila by remember { mutableStateOf(initial.upazila) }
    var mouza by remember { mutableStateOf(initial.mouza) }
    var jl by remember { mutableStateOf(initial.jlNumber) }
    var khatian by remember { mutableStateOf(initial.khatianNumber) }
    var dag by remember { mutableStateOf(initial.dagNumber) }
    var areaStr by remember { mutableStateOf(if (initial.areaShotok > 0) initial.areaShotok.toString() else "") }
    var landClass by remember { mutableStateOf(initial.landClass) }
    var recordType by remember { mutableStateOf(initial.recordType) }
    var notes by remember { mutableStateOf(initial.notes) }
    var photoUris by remember { mutableStateOf(SurveyRepository.decodeStringList(initial.photoUrisJson)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { photoUris = photoUris + it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "নতুন খতিয়ান ফাইল" else "খতিয়ান সম্পাদনা") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("মালিকের নাম *") }, singleLine = true)
                OutlinedTextField(value = father, onValueChange = { father = it }, label = { Text("পিতা / স্বামীর নাম") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("উপজেলা") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজা") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = jl, onValueChange = { jl = it }, label = { Text("জে.এল.") }, singleLine = true, modifier = Modifier.weight(0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = khatian, onValueChange = { khatian = it }, label = { Text("খতিয়ান নং") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dag, onValueChange = { dag = it }, label = { Text("দাগ নং") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = areaStr, onValueChange = { areaStr = it }, label = { Text("জমির পরিমাণ (শতাংশ)") }, singleLine = true)
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
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("দলিল ও মন্তব্য") }, maxLines = 2)
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("দলিল / পর্চার ছবি (${photoUris.size})")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    initial.copy(
                        ownerName = owner.ifBlank { "অজানা মালিক" },
                        fatherOrHusbandName = father,
                        district = district,
                        upazila = upazila,
                        mouza = mouza,
                        jlNumber = jl,
                        khatianNumber = khatian,
                        dagNumber = dag,
                        areaShotok = areaStr.toDoubleOrNull() ?: 0.0,
                        landClass = landClass,
                        recordType = recordType,
                        photoUrisJson = SurveyRepository.encodeStringList(photoUris),
                        notes = notes,
                    )
                )
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
fun CsBsTrackerScreen(
    records: List<CsBsRecordEntity>,
    onSave: (CsBsRecordEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var editing by remember { mutableStateOf<CsBsRecordEntity?>(null) }

    Scaffold(
        topBar = { SurveyTopBar(title = "CS-SA-RS-BS-BRS ট্র্যাকার", subtitle = "খতিয়ান ও দাগ রূপান্তর ইতিহাস", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = CsBsRecordEntity(mouza = "") },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("নতুন রেকর্ড") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (records.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("কোনো CS-BS ট্র্যাকিং রেকর্ড নেই", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("একই জমির CS, SA, RS, BS ও BRS দাগ নম্বর এবং জমির পরিমাণ পরিবর্তন ট্র্যাক করতে '+' চাপুন।", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(records, key = { it.id }) { r ->
                    Surface(
                        onClick = { editing = r },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("মৌজা: ${r.mouza}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("দাগ: CS(${r.csDag}) → SA(${r.saDag}) → RS(${r.rsDag}) → BS(${r.bsDag}) → BRS(${r.brsDag})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("পরিমাণ (শতাংশ): CS ${r.csAreaShotok} | RS ${r.rsAreaShotok} | BS ${r.bsAreaShotok}", style = MaterialTheme.typography.bodySmall)
                            if (r.ownerTrail.isNotBlank()) Text("মালিকানা ধারা: ${r.ownerTrail}", style = MaterialTheme.typography.bodySmall)
                            if (r.mutationNote.isNotBlank()) Text("মন্তব্য: ${r.mutationNote}", style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = r }) { Text("এডিট") }
                                TextButton(onClick = { onDelete(r.id) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("ডিলিট") }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { r ->
        var mouza by remember { mutableStateOf(r.mouza) }
        var cs by remember { mutableStateOf(r.csDag) }
        var sa by remember { mutableStateOf(r.saDag) }
        var rs by remember { mutableStateOf(r.rsDag) }
        var bs by remember { mutableStateOf(r.bsDag) }
        var brs by remember { mutableStateOf(r.brsDag) }
        var csArea by remember { mutableStateOf(if (r.csAreaShotok > 0) r.csAreaShotok.toString() else "") }
        var rsArea by remember { mutableStateOf(if (r.rsAreaShotok > 0) r.rsAreaShotok.toString() else "") }
        var bsArea by remember { mutableStateOf(if (r.bsAreaShotok > 0) r.bsAreaShotok.toString() else "") }
        var owners by remember { mutableStateOf(r.ownerTrail) }
        var note by remember { mutableStateOf(r.mutationNote) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("CS-BS দাগ রূপান্তর রেকর্ড") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজার নাম *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = cs, onValueChange = { cs = it }, label = { Text("CS দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = sa, onValueChange = { sa = it }, label = { Text("SA দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = rs, onValueChange = { rs = it }, label = { Text("RS দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = bs, onValueChange = { bs = it }, label = { Text("BS দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = brs, onValueChange = { brs = it }, label = { Text("BRS দাগ") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = csArea, onValueChange = { csArea = it }, label = { Text("CS শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = rsArea, onValueChange = { rsArea = it }, label = { Text("RS শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = bsArea, onValueChange = { bsArea = it }, label = { Text("BS শতক") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = owners, onValueChange = { owners = it }, label = { Text("মালিকানা ধারা") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("মন্তব্য / খারিজনামা") }, maxLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSave(
                        r.copy(
                            mouza = mouza.ifBlank { "অজানা মৌজা" },
                            csDag = cs, saDag = sa, rsDag = rs, bsDag = bs, brsDag = brs,
                            csAreaShotok = csArea.toDoubleOrNull() ?: 0.0,
                            rsAreaShotok = rsArea.toDoubleOrNull() ?: 0.0,
                            bsAreaShotok = bsArea.toDoubleOrNull() ?: 0.0,
                            ownerTrail = owners,
                            mutationNote = note,
                        )
                    )
                    editing = null
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("বাতিল") } },
        )
    }
}
