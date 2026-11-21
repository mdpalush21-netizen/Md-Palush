package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserPreferencesState
import com.example.model.SurveyToolId
import com.example.model.ToolGroup
import com.example.ui.components.ToolCard

@Composable
fun HomeScreen(
    prefs: UserPreferencesState,
    projectCount: Int,
    onOpenTool: (SurveyToolId) -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenProjects: () -> Unit,
    onUnlockProPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBn = prefs.isBengali
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<ToolGroup?>(null) }
    var proPromptTool by remember { mutableStateOf<SurveyToolId?>(null) }

    val allTools = remember { SurveyToolId.entries.toList() }
    val pinnedTools = remember(prefs.pinnedToolIds) {
        allTools.filter { it.id in prefs.pinnedToolIds }
    }
    val filteredTools = remember(searchQuery, selectedGroup) {
        allTools.filter { tool ->
            val matchesGroup = selectedGroup == null || tool.group == selectedGroup
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                tool.titleBn.lowercase().contains(q) ||
                tool.titleEn.lowercase().contains(q) ||
                tool.subtitleBn.lowercase().contains(q) ||
                tool.subtitleEn.lowercase().contains(q)
            matchesGroup && matchesQuery
        }
    }

    fun handleToolTap(tool: SurveyToolId) {
        if (tool.isProFeature && !prefs.isProUnlocked) {
            proPromptTool = tool
        } else {
            onOpenTool(tool)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Hero Banner Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_survey_banner),
                                contentDescription = "ডিজিটাল সার্ভে ব্যানার",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF0A2A75).copy(alpha = 0.72f),
                                                Color(0xFF155EEF).copy(alpha = 0.86f),
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBn) "হাওলাদার ডিজিটাল সার্ভে" else "Howlader Digital Survey",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = if (isBn) "জমি পরিমাপ, নকশা, খতিয়ান ও ভাগ-বণ্টন" else "Land Measurement, CAD, Khatian & Partition",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFDBEAFE),
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.OfflinePin,
                                                contentDescription = "Offline Ready",
                                                tint = Color(0xFF86EFAC),
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = if (isBn) "অফলাইন রেডি" else "Offline Ready",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    FilledTonalButton(
                                        onClick = onOpenProjects,
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("hero_projects_button"),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF155EEF),
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isBn) "PROJECTS ($projectCount)" else "PROJECTS ($projectCount)", fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = { handleToolTap(SurveyToolId.LAND_PARTITION) },
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("hero_partition_button"),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFFFEDD5),
                                            contentColor = Color(0xFF9A3412),
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                    ) {
                                        Icon(Icons.Rounded.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isBn) "জমি ভাগ-বণ্টন" else "Partition", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(if (isBn) "টুল খুঁজুন (যেমন: অটো নকশা, খতিয়ান, GPS, CAD)..." else "Search tools (e.g. Noksha, Khatian, GPS, CAD)...")
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_field"),
                )
            }

            // Pinned Tools Section
            if (pinnedTools.isNotEmpty() && searchQuery.isBlank() && selectedGroup == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (isBn) "পিন করা প্রয়োজনীয় টুলস" else "Pinned Quick Tools",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(pinnedTools, key = { "pin_${it.id}" }) { tool ->
                                Surface(
                                    onClick = { handleToolTap(tool) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.width(156.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(tool.accentBg),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(tool.icon, contentDescription = null, tint = tool.accentColor, modifier = Modifier.size(20.dp))
                                        }
                                        Text(
                                            text = if (isBn) tool.titleBn else tool.titleEn,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                val groups = listOf(
                    null to (if (isBn) "সব টুল (${allTools.size})" else "All (${allTools.size})"),
                    ToolGroup.MEASUREMENT to (if (isBn) "জমি পরিমাপ (১৫)" else "Measurement (15)"),
                    ToolGroup.PARTITION to (if (isBn) "ভাগ-বণ্টন" else "Partition"),
                    ToolGroup.SURVEY_CAD to (if (isBn) "জরিপ ও CAD (১০)" else "Survey & CAD (10)"),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups) { (grp, label) ->
                        FilterChip(
                            selected = selectedGroup == grp,
                            onClick = { selectedGroup = grp },
                            label = { Text(label) },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }

            // Adaptive Responsive Grid of Tool Cards
            val rows = filteredTools.chunked(columns)
            items(rows) { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowTools.forEach { tool ->
                        ToolCard(
                            tool = tool,
                            isBengali = isBn,
                            isPinned = tool.id in prefs.pinnedToolIds,
                            isProUnlocked = prefs.isProUnlocked,
                            onClick = { handleToolTap(tool) },
                            onTogglePin = { onTogglePin(tool.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowTools.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    proPromptTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { proPromptTool = null },
            icon = {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(32.dp))
            },
            title = {
                Text(if (isBn) "${tool.titleBn} (Pro ফিচার)" else "${tool.titleEn} (Pro Feature)")
            },
            text = {
                Text(
                    if (isBn) "এই অ্যাডভান্সড টুলটি প্রিমিয়াম সার্ভেয়ার প্যাকেজের অন্তর্ভুক্ত। এখনই ফ্রি ট্রায়াল/প্রিভিউ আনলক করে টুলটি সম্পূর্ণ ব্যবহার করুন।"
                    else "This advanced tool is part of Surveyor Pro. Unlock Pro Preview now to use all features."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUnlockProPreview()
                        val target = tool
                        proPromptTool = null
                        onOpenTool(target)
                    },
                    modifier = Modifier.testTag("unlock_pro_preview_button"),
                ) {
                    Text(if (isBn) "Pro প্রিভিউ আনলক ও ওপেন" else "Unlock & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { proPromptTool = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            },
        )
    }
}
