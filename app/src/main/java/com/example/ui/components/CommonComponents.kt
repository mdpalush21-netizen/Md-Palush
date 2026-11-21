package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.math.LandUnitConverter
import com.example.model.SurveyToolId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "ফিরে যান",
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
fun ToolCard(
    tool: SurveyToolId,
    isBengali: Boolean,
    isPinned: Boolean,
    isProUnlocked: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = tool.isProFeature && !isProUnlocked
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tool.accentBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = if (isBengali) tool.titleBn else tool.titleEn,
                        tint = tool.accentColor,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tool.isProFeature) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (locked) Color(0xFFFFEDD5) else Color(0xFFDCFCE7),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = if (locked) Icons.Rounded.Lock else Icons.Rounded.WorkspacePremium,
                                    contentDescription = "PRO",
                                    tint = if (locked) Color(0xFFEA580C) else Color(0xFF16A34A),
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (locked) Color(0xFFEA580C) else Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("pin_button_${tool.id}"),
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Rounded.PushPin else Icons.Rounded.OutlinedFlag,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isBengali) tool.titleBn else tool.titleEn,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isBengali) tool.subtitleBn else tool.subtitleEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AllUnitsResultCard(
    areaSqFt: Double,
    perimeterFt: Double? = null,
    title: String = "ক্ষেত্রফলের পূর্ণাঙ্গ হিসাব",
    onSaveToProject: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val summary = remember(areaSqFt) { LandUnitConverter.summarizeFromSqFeet(areaSqFt) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${LandUnitConverter.format(summary.shotok, 2)} শতাংশ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            if (perimeterFt != null && perimeterFt > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "মোট পরিসীমা (Perimeter): ${LandUnitConverter.format(perimeterFt, 2)} ফুট (${LandUnitConverter.format(perimeterFt * 0.3048, 2)} মিটার)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val entries = listOf(
                "শতাংশ / ডেসিমেল" to LandUnitConverter.format(summary.shotok, 3),
                "কাঠা (৭২০ ব.ফু)" to LandUnitConverter.format(summary.katha, 3),
                "বিঘা (২০ কাঠা)" to LandUnitConverter.format(summary.bigha, 4),
                "একর (১০০ শতক)" to LandUnitConverter.format(summary.acre, 4),
                "বর্গফুট (Sq.Ft)" to LandUnitConverter.format(summary.sqFeet, 2),
                "বর্গমিটার (Sq.M)" to LandUnitConverter.format(summary.sqMeter, 2),
                "বর্গগজ (Sq.Yd)" to LandUnitConverter.format(summary.sqYard, 2),
                "হেক্টর (Hectare)" to LandUnitConverter.format(summary.hectare, 5),
            )

            entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { (label, value) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }

            if (onSaveToProject != null || onExportPdf != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onSaveToProject != null) {
                        Button(
                            onClick = onSaveToProject,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_to_project_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রজেক্টে সেভ")
                        }
                    }
                    if (onExportPdf != null) {
                        OutlinedButton(
                            onClick = onExportPdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_pdf_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF রিপোর্ট")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GpsDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEDD5)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = "GPS সতর্কতা",
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = "গুরুত্বপূর্ণ নির্দেশনা: মোবাইল GPS দিয়ে সাধারণ অবস্থান ও প্রাথমিক পরিমাপ করা যায়। আইনগত বা চূড়ান্ত সীমানা নির্ধারণের জন্য অনুমোদিত Total Station বা DGPS যন্ত্র ব্যবহার করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A3412),
            )
        }
    }
}

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String = "ফুট",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        suffix = if (suffix.isNotBlank()) { { Text(suffix) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SaveToProjectDialog(
    defaultTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, owner: String, mouza: String, dag: String, khatian: String) -> Unit,
) {
    var name by remember { mutableStateOf(defaultTitle) }
    var owner by remember { mutableStateOf("") }
    var mouza by remember { mutableStateOf("") }
    var dag by remember { mutableStateOf("") }
    var khatian by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রজেক্ট হিসেবে সংরক্ষণ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("প্রজেক্টের নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("জমির মালিকের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mouza,
                    onValueChange = { mouza = it },
                    label = { Text("মৌজা / ঠিকানা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = khatian,
                        onValueChange = { khatian = it },
                        label = { Text("খতিয়ান নং") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = dag,
                        onValueChange = { dag = it },
                        label = { Text("দাগ নং") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, owner, mouza, dag, khatian) },
                modifier = Modifier.testTag("confirm_save_project"),
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        },
    )
}
