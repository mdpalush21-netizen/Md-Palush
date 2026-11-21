package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.export.ReportExporter
import com.example.core.math.*
import com.example.data.location.LocationTracker
import com.example.model.GpsFix
import com.example.model.SurveyToolId
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.*

private enum class MapLayerMode(val labelBn: String) {
    SATELLITE("স্যাটেলাইট"),
    STREET("ম্যাপ"),
    OFFLINE_GRID("অফলাইন গ্রিড"),
}

@Composable
fun MapAndGpsScreen(
    tool: SurveyToolId,
    surveyorFooter: String,
    onBack: () -> Unit,
    onOpenInCad: (List<SurveyPoint2D>) -> Unit,
    onSaveProject: (title: String, owner: String, mouza: String, dag: String, khatian: String, areaSqFt: Double, pts: List<SurveyPoint2D>, summary: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tracker = remember(context) { LocationTracker(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        },
    )

    // Map center defaults to Dhaka, Bangladesh; updates on GPS fix
    var centerLat by remember { mutableDoubleStateOf(23.8103) }
    var centerLon by remember { mutableDoubleStateOf(90.4125) }
    var zoomLevel by remember { mutableIntStateOf(17) }
    var layerMode by remember {
        mutableStateOf(
            if (tool == SurveyToolId.NOKSHA_MEASURE) MapLayerMode.OFFLINE_GRID else MapLayerMode.SATELLITE
        )
    }

    var currentFix by remember { mutableStateOf<GpsFix?>(null) }
    var isTrackingWalk by remember { mutableStateOf(false) }

    // Stored coordinates (Lat, Lon)
    var gpsCorners by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }

    // Noksha Measure / Map Overlay photo & calibration
    var overlayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var overlayOpacity by remember { mutableFloatStateOf(0.55f) }
    var overlayScale by remember { mutableFloatStateOf(1.0f) }
    var overlayRotation by remember { mutableFloatStateOf(0f) }
    var mouzaScaleIndex by remember { mutableIntStateOf(0) } // 16", 32", 64"

    var tileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    withContext(Dispatchers.Main) { overlayBitmap = bmp }
                }
            }
        },
    )

    // Continuous walk-the-boundary GPS listener
    LaunchedEffect(isTrackingWalk, hasLocationPermission) {
        if (isTrackingWalk && hasLocationPermission) {
            tracker.observeLocationUpdates(2500L).collect { fix ->
                currentFix = fix
                centerLat = fix.latitude
                centerLon = fix.longitude
                val last = gpsCorners.lastOrNull()
                if (last == null || SurveyMath.haversineMeters(last.first, last.second, fix.latitude, fix.longitude) >= 1.5) {
                    gpsCorners = gpsCorners + (fix.latitude to fix.longitude)
                }
            }
        }
    }

    // Fetch single tile center mosaic when online & not in offline grid mode
    LaunchedEffect(centerLat, centerLon, zoomLevel, layerMode) {
        if (layerMode == MapLayerMode.OFFLINE_GRID) {
            tileBitmap = null
            return@LaunchedEffect
        }
        tileBitmap = withContext(Dispatchers.IO) {
            fetchMapTileGrid(centerLat, centerLon, zoomLevel, layerMode == MapLayerMode.SATELLITE)
        }
    }

    val localPoints = remember(gpsCorners) {
        SurveyMath.latLonToLocalFeet(gpsCorners)
    }
    val areaSqFt = remember(localPoints) { SurveyMath.polygonAreaShoelace(localPoints) }
    val perimeterFt = remember(localPoints) {
        SurveyMath.polygonPerimeter(localPoints, closed = tool != SurveyToolId.SATELLITE_LINE_MAP)
    }

    Scaffold(
        topBar = {
            SurveyTopBar(
                title = tool.titleBn,
                subtitle = tool.subtitleBn,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            val uri = Uri.parse("geo:$centerLat,$centerLon?q=$centerLat,$centerLon(Survey+Point)")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            runCatching { context.startActivity(intent) }
                        },
                    ) {
                        Icon(Icons.Rounded.Map, contentDescription = "Google Maps-এ খুলুন")
                    }
                },
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
            item { GpsDisclaimerBanner() }

            // Layer & Zoom Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MapLayerMode.entries.forEach { mode ->
                            FilterChip(
                                selected = layerMode == mode,
                                onClick = { layerMode = mode },
                                label = { Text(mode.labelBn, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { zoomLevel = (zoomLevel - 1).coerceAtLeast(13) }) {
                            Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom Out")
                        }
                        Text("Z$zoomLevel", style = MaterialTheme.typography.labelMedium)
                        IconButton(onClick = { zoomLevel = (zoomLevel + 1).coerceAtMost(19) }) {
                            Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom In")
                        }
                    }
                }
            }

            // Interactive Map / Noksha Tap Canvas
            item {
                val borderColor = MaterialTheme.colorScheme.outline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                        .pointerInput(centerLat, centerLon, zoomLevel) {
                            detectTapGestures { offset ->
                                val (tapLat, tapLon) = screenOffsetToLatLon(
                                    offset.x, offset.y, size.width.toFloat(), size.height.toFloat(),
                                    centerLat, centerLon, zoomLevel,
                                )
                                gpsCorners = gpsCorners + (tapLat to tapLon)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw background tiles or blueprint grid
                        val tb = tileBitmap
                        if (tb != null && layerMode != MapLayerMode.OFFLINE_GRID) {
                            drawImage(
                                image = tb.asImageBitmap(),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            )
                        } else {
                            drawRect(color = Color(0xFFF8FBFF))
                            val step = 40f
                            var gx = step
                            while (gx < size.width) {
                                drawLine(Color(0xFFDCE8F8), Offset(gx, 0f), Offset(gx, size.height), 1f)
                                gx += step
                            }
                            var gy = step
                            while (gy < size.height) {
                                drawLine(Color(0xFFDCE8F8), Offset(0f, gy), Offset(size.width, gy), 1f)
                                gy += step
                            }
                        }

                        // Draw optional Mouza Noksha Overlay Photo
                        overlayBitmap?.let { bmp ->
                            withTransform({
                                translate(size.width / 2f, size.height / 2f)
                                rotate(overlayRotation)
                                scale(overlayScale, overlayScale)
                                translate(-size.width / 2f, -size.height / 2f)
                            }) {
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                    alpha = overlayOpacity,
                                )
                            }
                        }

                        // Draw Polygon / Line vertices
                        val mapped = gpsCorners.map { (lat, lon) ->
                            latLonToScreenOffset(lat, lon, size.width, size.height, centerLat, centerLon, zoomLevel)
                        }
                        if (mapped.size >= 2) {
                            val path = Path().apply {
                                moveTo(mapped[0].x, mapped[0].y)
                                for (i in 1 until mapped.size) lineTo(mapped[i].x, mapped[i].y)
                                if (tool != SurveyToolId.SATELLITE_LINE_MAP && mapped.size >= 3) close()
                            }
                            if (tool != SurveyToolId.SATELLITE_LINE_MAP && mapped.size >= 3) {
                                drawPath(path, color = Color(0xFF22C55E).copy(alpha = 0.28f))
                            }
                            drawPath(path, color = Color(0xFF16A34A), style = Stroke(width = 5f))
                        }

                        val lblPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            textSize = 24f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        mapped.forEachIndexed { idx, pt ->
                            drawCircle(Color.White, radius = 10f, center = pt)
                            drawCircle(Color(0xFFEA580C), radius = 6.5f, center = pt)
                            drawContext.canvas.nativeCanvas.drawText("P${idx + 1}", pt.x, pt.y - 14f, lblPaint)
                            if (idx < mapped.lastIndex || (tool != SurveyToolId.SATELLITE_LINE_MAP && mapped.size >= 3 && idx == mapped.lastIndex)) {
                                val nextIdx = (idx + 1) % mapped.size
                                val nextPt = mapped[nextIdx]
                                val meters = SurveyMath.haversineMeters(
                                    gpsCorners[idx].first, gpsCorners[idx].second,
                                    gpsCorners[nextIdx].first, gpsCorners[nextIdx].second,
                                )
                                val ft = meters * 3.28084
                                drawContext.canvas.nativeCanvas.drawText(
                                    "${LandUnitConverter.format(ft, 1)}'",
                                    (pt.x + nextPt.x) / 2f,
                                    (pt.y + nextPt.y) / 2f - 4f,
                                    lblPaint,
                                )
                            }
                        }
                    }

                    // Hint Pill at top-left
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.62f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                    ) {
                        Text(
                            text = "ম্যাপে ট্যাপ করে কোণা যোগ করুন (${gpsCorners.size} পয়েন্ট)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // GPS Action Controls
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentFix?.let { fix ->
                            Text(
                                text = "বর্তমান GPS: ${LandUnitConverter.format(fix.latitude, 6)}, ${LandUnitConverter.format(fix.longitude, 6)} (±${LandUnitConverter.format(fix.accuracyMeters.toDouble(), 1)}m)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            )
                                        )
                                    } else {
                                        scope.launch {
                                            tracker.getSingleFix()?.let { fix ->
                                                currentFix = fix
                                                centerLat = fix.latitude
                                                centerLon = fix.longitude
                                                gpsCorners = gpsCorners + (fix.latitude to fix.longitude)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("add_gps_point_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("GPS পয়েন্ট নিন")
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            )
                                        )
                                    } else {
                                        isTrackingWalk = !isTrackingWalk
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    if (isTrackingWalk) Icons.Rounded.Stop else Icons.Rounded.DirectionsWalk,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isTrackingWalk) "ট্র্যাকিং বন্ধ" else "Walk ট্র্যাক")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { if (gpsCorners.isNotEmpty()) gpsCorners = gpsCorners.dropLast(1) },
                                modifier = Modifier.weight(1f).height(44.dp),
                            ) {
                                Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Undo")
                            }
                            OutlinedButton(
                                onClick = { gpsCorners = emptyList() },
                                modifier = Modifier.weight(1f).height(44.dp),
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear")
                            }
                            OutlinedButton(
                                onClick = { if (localPoints.isNotEmpty()) onOpenInCad(localPoints) },
                                modifier = Modifier.weight(1f).height(44.dp),
                            ) {
                                Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CAD")
                            }
                        }
                    }
                }
            }

            // Noksha Overlay & Scale Controls (for Google Map Overlay & Noksha Measure)
            if (tool == SurveyToolId.GOOGLE_MAP_OVERLAY || tool == SurveyToolId.NOKSHA_MEASURE) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("নকশা ইমেজ ওভারলে ও স্কেল", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("মৌজা নকশার ছবি আপলোড করুন")
                            }
                            if (tool == SurveyToolId.NOKSHA_MEASURE) {
                                val scales = listOf("১৬\" = ১ মাইল", "৩২\" = ১ মাইল", "৬৪\" = ১ মাইল")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(scales.indices.toList()) { i ->
                                        FilterChip(
                                            selected = mouzaScaleIndex == i,
                                            onClick = { mouzaScaleIndex = i },
                                            label = { Text(scales[i]) },
                                        )
                                    }
                                }
                            }
                            if (overlayBitmap != null) {
                                Text("স্বচ্ছতা (Opacity): ${(overlayOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                                Slider(value = overlayOpacity, onValueChange = { overlayOpacity = it }, valueRange = 0.1f..1f)
                                Text("স্কেল: ${LandUnitConverter.format(overlayScale.toDouble(), 2)}x", style = MaterialTheme.typography.labelMedium)
                                Slider(value = overlayScale, onValueChange = { overlayScale = it }, valueRange = 0.5f..2.5f)
                                Text("ঘূর্ণন (Rotation): ${overlayRotation.toInt()}°", style = MaterialTheme.typography.labelMedium)
                                Slider(value = overlayRotation, onValueChange = { overlayRotation = it }, valueRange = -180f..180f)
                            }
                        }
                    }
                }
            }

            // Recorded Lat/Lon Corner List
            if (gpsCorners.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("সংরক্ষিত কর্নার (Lat / Lon)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            gpsCorners.forEachIndexed { idx, (lat, lon) ->
                                Text(
                                    "P${idx + 1}: ${LandUnitConverter.format(lat, 6)}, ${LandUnitConverter.format(lon, 6)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item {
                AllUnitsResultCard(
                    areaSqFt = areaSqFt,
                    perimeterFt = perimeterFt,
                    onSaveToProject = { showSaveDialog = true },
                    onExportPdf = {
                        val file = ReportExporter.exportReportPdf(
                            context = context,
                            title = tool.titleBn,
                            subtitle = "ম্যাপ ও GPS সীমানা রিপোর্ট",
                            metadataLines = listOf(
                                "কেন্দ্র" to "${LandUnitConverter.format(centerLat, 5)}, ${LandUnitConverter.format(centerLon, 5)}",
                                "কর্নার সংখ্যা" to "${gpsCorners.size} টি",
                            ),
                            areaSqFt = areaSqFt,
                            points = localPoints,
                            extraLines = gpsCorners.mapIndexed { i, (la, lo) -> "P${i + 1}: $la, $lo" },
                            surveyorFooter = surveyorFooter,
                        )
                        ReportExporter.shareFile(context, file, "application/pdf")
                    },
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveToProjectDialog(
            defaultTitle = "${tool.titleBn} জরিপ",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, owner, mouza, dag, khatian ->
                onSaveProject(
                    name, owner, mouza, dag, khatian, areaSqFt, localPoints,
                    "GPS/Map কর্নার: ${gpsCorners.size} টি, পরিসীমা ${LandUnitConverter.format(perimeterFt, 1)} ফুট",
                )
                showSaveDialog = false
            },
        )
    }
}

private val tileHttpClient = OkHttpClient()

private fun latLonToTileDouble(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
    val n = 2.0.pow(zoom)
    val x = (lon + 180.0) / 360.0 * n
    val latRad = Math.toRadians(lat)
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    return x to y
}

private fun tileDoubleToLatLon(x: Double, y: Double, zoom: Int): Pair<Double, Double> {
    val n = 2.0.pow(zoom)
    val lon = x / n * 360.0 - 180.0
    val latRad = atan(sinh(PI * (1.0 - 2.0 * y / n)))
    val lat = Math.toDegrees(latRad)
    return lat to lon
}

private fun latLonToScreenOffset(
    lat: Double,
    lon: Double,
    w: Float,
    h: Float,
    centerLat: Double,
    centerLon: Double,
    zoom: Int,
): Offset {
    val (cx, cy) = latLonToTileDouble(centerLat, centerLon, zoom)
    val (px, py) = latLonToTileDouble(lat, lon, zoom)
    val tilePx = min(w, h) / 2f
    return Offset(
        x = w / 2f + ((px - cx) * tilePx).toFloat(),
        y = h / 2f + ((py - cy) * tilePx).toFloat(),
    )
}

private fun screenOffsetToLatLon(
    sx: Float,
    sy: Float,
    w: Float,
    h: Float,
    centerLat: Double,
    centerLon: Double,
    zoom: Int,
): Pair<Double, Double> {
    val (cx, cy) = latLonToTileDouble(centerLat, centerLon, zoom)
    val tilePx = min(w, h) / 2f
    val tx = cx + (sx - w / 2f) / tilePx
    val ty = cy + (sy - h / 2f) / tilePx
    return tileDoubleToLatLon(tx, ty, zoom)
}

private fun fetchMapTileGrid(lat: Double, lon: Double, zoom: Int, satellite: Boolean): Bitmap? {
    return runCatching {
        val (txd, tyd) = latLonToTileDouble(lat, lon, zoom)
        val cx = floor(txd).toInt()
        val cy = floor(tyd).toInt()
        val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val fracX = ((txd - cx) * 256.0).toFloat()
        val fracY = ((tyd - cy) * 256.0).toFloat()

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = cx + dx
                val ty = cy + dy
                val url = if (satellite) {
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$zoom/$ty/$tx"
                } else {
                    "https://tile.openstreetmap.org/$zoom/$tx/$ty.png"
                }
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "HowladerDigitalSurvey/1.0")
                    .build()
                tileHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.bytes()?.let { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                                val drawX = 256f + (dx * 256f) - fracX
                                val drawY = 256f + (dy * 256f) - fracY
                                canvas.drawBitmap(bmp, drawX, drawY, null)
                                bmp.recycle()
                            }
                        }
                    }
                }
            }
        }
        out
    }.getOrNull()
}
