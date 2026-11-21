package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.math.LandUnit
import com.example.data.local.UserPreferencesState
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    prefs: UserPreferencesState,
    onSaveProfile: (name: String, title: String, phone: String, email: String, office: String) -> Unit,
    onSetLanguage: (Boolean) -> Unit,
    onSetDefaultUnit: (LandUnit) -> Unit,
    onSetProUnlocked: (Boolean) -> Unit,
    onSetPinLock: (Boolean, String) -> Unit,
    onExportBackupJson: suspend () -> String,
    onImportBackupJson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isBn = prefs.isBengali

    var name by remember(prefs.surveyorName) { mutableStateOf(prefs.surveyorName) }
    var title by remember(prefs.surveyorTitle) { mutableStateOf(prefs.surveyorTitle) }
    var phone by remember(prefs.surveyorPhone) { mutableStateOf(prefs.surveyorPhone) }
    var email by remember(prefs.surveyorEmail) { mutableStateOf(prefs.surveyorEmail) }
    var office by remember(prefs.surveyorOffice) { mutableStateOf(prefs.surveyorOffice) }

    var showPinDialog by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    val json = onExportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    statusNotice = if (isBn) "লোকাল ব্যাকআপ ফাইল সফলভাবে সংরক্ষিত হয়েছে।" else "Backup file exported."
                }
            }
        },
    )

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!text.isNullOrBlank()) {
                        onImportBackupJson(text)
                    }
                }
            }
        },
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = if (isBn) "অ্যাকাউন্ট ও সেটিংস" else "Account & Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        statusNotice?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFF166534),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        // Surveyor Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text(if (isBn) "সার্ভেয়ার প্রোফাইল" else "Surveyor Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (isBn) "PDF রিপোর্টে স্বাক্ষর ও পরিচয় হিসেবে যুক্ত হবে" else "Printed on exported PDF reports", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isBn) "নাম" else "Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isBn) "পদবি / লাইসেন্স" else "Title / License") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(if (isBn) "মোবাইল" else "Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(if (isBn) "ইমেইল" else "Email") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = office, onValueChange = { office = it }, label = { Text(if (isBn) "অফিস / ঠিকানা" else "Office Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = { onSaveProfile(name, title, phone, email, office) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (isBn) "প্রোফাইল সংরক্ষণ করুন" else "Save Profile")
                    }
                }
            }
        }

        // Language & Default Unit
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isBn) "ভাষা ও একক সেটিংস" else "Language & Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = prefs.isBengali,
                            onClick = { onSetLanguage(true) },
                            label = { Text("বাংলা (প্রধান)") },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = !prefs.isBengali,
                            onClick = { onSetLanguage(false) },
                            label = { Text("English") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(if (isBn) "প্রাথমিক জমির একক" else "Default Land Unit", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(LandUnit.SHOTOK, LandUnit.KATHA, LandUnit.BIGHA, LandUnit.ACRE, LandUnit.SQ_FEET)) { unit ->
                            FilterChip(
                                selected = prefs.defaultUnit == unit,
                                onClick = { onSetDefaultUnit(unit) },
                                label = { Text(if (isBn) unit.titleBn else unit.titleEn) },
                            )
                        }
                    }
                }
            }
        }

        // Backup, Restore & Offline Mode
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (isBn) "ব্যাকআপ ও রিস্টোর (অফলাইন মোড)" else "Backup & Restore (Offline Mode)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (isBn) "সব প্রজেক্ট, খতিয়ান ও CS-BS তথ্য আপনার ডিভাইসের Room ডাটাবেজে অফলাইনে সংরক্ষিত থাকে। JSON ফাইল হিসেবে ব্যাকআপ নিন বা রিস্টোর করুন।"
                        else "All Projects, Khatian & CS-BS data are stored offline in local SQLite/Room. Export or restore a JSON backup file anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { backupExportLauncher.launch("howlader_survey_backup_${System.currentTimeMillis()}.json") },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_backup_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBn) "ব্যাকআপ সেভ" else "Export")
                        }
                        OutlinedButton(
                            onClick = { backupImportLauncher.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("import_backup_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBn) "রিস্টোর" else "Restore")
                        }
                    }
                }
            }
        }

        // Security PIN & Subscription Pro Management
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isBn) "অ্যাপ নিরাপত্তা (PIN লক)" else "App Security (PIN Lock)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (isBn) "অ্যাপ খোলার সময় ৪ সংখ্যার পিন চাইবে" else "Require 4-digit PIN on launch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.pinLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) showPinDialog = true else onSetPinLock(false, "")
                            },
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isBn) "Surveyor Pro প্যাকেজ" else "Surveyor Pro Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (isBn) "CAD Editor, Total Station Import ও Map Overlay" else "Unlock CAD, Total Station & Overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.isProUnlocked,
                            onCheckedChange = onSetProUnlocked,
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("৪ সংখ্যার PIN সেট করুন") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("৪ সংখ্যার পিন") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    enabled = pinInput.length == 4,
                    onClick = {
                        onSetPinLock(true, pinInput)
                        showPinDialog = false
                    },
                ) { Text("সেভ করুন") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("বাতিল") } },
        )
    }
}
