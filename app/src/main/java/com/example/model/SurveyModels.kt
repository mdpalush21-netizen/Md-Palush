package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

enum class ToolGroup {
    MEASUREMENT,
    PARTITION,
    SURVEY_CAD,
}

enum class SurveyToolId(
    val id: String,
    val group: ToolGroup,
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accentColor: Color,
    val accentBg: Color,
    val isProFeature: Boolean = false,
) {
    // 1. জমি পরিমাপের ১৫টি টুলস
    AUTO_NOKSHA(
        "auto_noksha", ToolGroup.MEASUREMENT,
        "অটো নকশা তৈরি", "Auto Noksha",
        "স্বয়ংক্রিয় জমির নকশা ও মাপ", "Auto plot drawing from sides",
        Icons.Rounded.AutoAwesomeMosaic, AccentBlue, AccentBlueBg
    ),
    NOKSHA_MEASURE(
        "noksha_measure", ToolGroup.MEASUREMENT,
        "নকশা পরিমাপ", "Noksha Measure",
        "১৬\" / ৩২\" / ৬৪\" স্কেলে মাপ", "Scale map measurement",
        Icons.Rounded.Straighten, AccentGreen, AccentGreenBg
    ),
    ONLINE_DIGITAL_NOKSHA(
        "online_digital_noksha", ToolGroup.MEASUREMENT,
        "অনলাইন ডিজিটাল নকশা", "Online Digital Map",
        "স্যাটেলাইট ম্যাপে প্লট অঙ্কন", "Draw plots on live map",
        Icons.Rounded.Public, AccentTeal, AccentTealBg
    ),
    GOOGLE_MAP_OVERLAY(
        "google_map_overlay", ToolGroup.MEASUREMENT,
        "গুগল ম্যাপে নকশা স্থাপন", "Map Overlay",
        "নকশা ছবি ম্যাপের ওপর সেট", "Align Mouza map on map",
        Icons.Rounded.Layers, AccentBlue, AccentBlueBg, isProFeature = true
    ),
    PENTAGRAPH(
        "pentagraph", ToolGroup.MEASUREMENT,
        "পেন্টাগ্রাফ নকশা", "Pentagraph Scale",
        "ভিন্ন স্কেলের নকশা তুলনা", "Compare & rescale maps",
        Icons.Rounded.Compare, AccentIndigo, AccentIndigoBg
    ),
    PLOT_PLANNING(
        "plot_planning", ToolGroup.MEASUREMENT,
        "প্লট প্ল্যানিং", "Plot Planning",
        "রাস্তা ও আবাসিক প্লট লেআউট", "Subdivide plots with roads",
        Icons.Rounded.GridView, AccentOrange, AccentOrangeBg
    ),
    SURVEY_KHATIAN(
        "survey_khatian", ToolGroup.MEASUREMENT,
        "সার্ভে খতিয়ান", "Survey Khatian",
        "খতিয়ান, দাগ ও দলিল ফাইল", "Khatian, Dag & deed docs",
        Icons.Rounded.Description, AccentGreen, AccentGreenBg
    ),
    QUAD_MEASURE(
        "quad_measure", ToolGroup.MEASUREMENT,
        "চতুর্ভুজ জমি পরিমাপ", "Quad Measure",
        "গড় ও কর্ণসহ চার বাহু হিসাব", "4-side land measurement",
        Icons.Rounded.CropSquare, AccentGreen, AccentGreenBg
    ),
    TRIANGLE_MEASURE(
        "triangle_measure", ToolGroup.MEASUREMENT,
        "ত্রিভুজ জমি পরিমাপ", "Triangle Measure",
        "হেরন সূত্র ও ৩ বাহু পরিমাপ", "Heron 3-side area",
        Icons.Rounded.ChangeHistory, AccentOrange, AccentOrangeBg
    ),
    MULTI_TRIANGLE(
        "multi_triangle", ToolGroup.MEASUREMENT,
        "একাধিক ত্রিভুজে পরিমাপ", "Multi-Triangle",
        "অনিয়মিত বহুভুজ জমি মাপ", "Irregular multi-triangle plot",
        Icons.Rounded.Polyline, AccentTeal, AccentTealBg
    ),
    EXACT_QUAD(
        "exact_quad", ToolGroup.MEASUREMENT,
        "সঠিক চতুর্ভুজ পরিমাপ", "Exact Quad",
        "কর্ণ দিয়ে নিখুঁত ক্ষেত্রফল", "Exact diagonal quad math",
        Icons.Rounded.SquareFoot, AccentBlue, AccentBlueBg
    ),
    AUTO_CORNER(
        "auto_corner", ToolGroup.MEASUREMENT,
        "অটো কর্নার ক্যালকুলেশন", "Auto Corner",
        "সমকোণ ও কর্ণ যাচাই (৩-৪-৫)", "Corner angles & diagonal",
        Icons.Rounded.Architecture, AccentIndigo, AccentIndigoBg
    ),
    LENGTH_WIDTH(
        "length_width", ToolGroup.MEASUREMENT,
        "দৈর্ঘ্য ও প্রস্থ পরিমাপ", "Length & Width",
        "ফুট, ইঞ্চি, লিঙ্ক, গুনিয়া, হাত", "Feet, Link, Gunia, Meter",
        Icons.Rounded.LinearScale, AccentGreen, AccentGreenBg
    ),
    AREA_CALCULATOR(
        "area_calculator", ToolGroup.MEASUREMENT,
        "জমির ক্ষেত্রফল নির্ণয়", "Land Calculator",
        "শতাংশ, কাঠা, বিঘা, একর রূপান্তর", "All BD unit conversions",
        Icons.Rounded.Calculate, AccentOrange, AccentOrangeBg
    ),
    BOUNDARY_DEMARCATION(
        "boundary_demarcation", ToolGroup.MEASUREMENT,
        "জমির সীমানা নির্ধারণ", "Boundary Pegging",
        "সীমানা খুঁটি ও বিয়ারিং নির্দেশনা", "Corner coordinates & bearings",
        Icons.Rounded.PinDrop, AccentRed, AccentRedBg
    ),

    // ২. জমি ভাগ-বণ্টন মডিউল
    LAND_PARTITION(
        "land_partition", ToolGroup.PARTITION,
        "জমি ভাগ-বণ্টন", "Land Partition",
        "অংশীদার, রাস্তা ও রঙিন নকশাসহ ভাগ", "Partner split, road & PDF",
        Icons.Rounded.PieChart, AccentOrange, AccentOrangeBg
    ),

    // ৩. জরিপ ও ট্র্যাকিং টুলস (১০টি)
    SATELLITE_LINE_MAP(
        "satellite_line_map", ToolGroup.SURVEY_CAD,
        "স্যাটেলাইট লাইন ম্যাপ", "Satellite Line Map",
        "ম্যাপের উপর লাইন ও দূরত্ব মাপ", "Draw lines & measure bearings",
        Icons.Rounded.SatelliteAlt, AccentBlue, AccentBlueBg
    ),
    ANA_GONDA_CALC(
        "ana_gonda_calc", ToolGroup.SURVEY_CAD,
        "আনা-গণ্ডা ক্যালকুলেটর", "Ana-Gonda Calc",
        "আনা-গণ্ডা-কড়া-ক্রান্তি-তিল হিসাব", "Traditional share calculator",
        Icons.Rounded.Balance, AccentGreen, AccentGreenBg
    ),
    TIE_LINE(
        "tie_line", ToolGroup.SURVEY_CAD,
        "টাই লাইন", "Tie Line Survey",
        "বেসলাইন ও অফসেট জরিপ", "Baseline & offset plotting",
        Icons.Rounded.Timeline, AccentTeal, AccentTealBg
    ),
    SLOPE_ELEVATION(
        "slope_elevation", ToolGroup.SURVEY_CAD,
        "ঢাল ও উচ্চতা পরিমাপ", "Slope & Elevation",
        "উচ্চতা, ঢাল % ও সেন্সর টিল্ট", "Slope %, elevation & tilt",
        Icons.Rounded.Terrain, AccentOrange, AccentOrangeBg
    ),
    LAND_SURVEY_TRAVERSE(
        "land_survey_traverse", ToolGroup.SURVEY_CAD,
        "ল্যান্ড সার্ভে (Traverse)", "Field Traverse",
        "বিয়ারিং ও দূরত্ব সমন্বয় নকশা", "Closed traverse & Bowditch",
        Icons.Rounded.Explore, AccentIndigo, AccentIndigoBg
    ),
    CS_BS_TRACKER(
        "cs_bs_tracker", ToolGroup.SURVEY_CAD,
        "CS-BS ট্র্যাকার", "CS-BS Tracker",
        "CS, SA, RS, BS, BRS দাগ তুলনা", "Track CS to BRS records",
        Icons.Rounded.ManageSearch, AccentBlue, AccentBlueBg
    ),
    TOTAL_STATION_IMPORT(
        "total_station_import", ToolGroup.SURVEY_CAD,
        "Total Station Import", "Total Station CSV",
        "CSV/TXT Easting-Northing প্লট", "Import CSV/TXT coordinates",
        Icons.Rounded.UploadFile, AccentRed, AccentRedBg, isProFeature = true
    ),
    GPS_COORDINATES(
        "gps_coordinates", ToolGroup.SURVEY_CAD,
        "GPS Coordinate পরিমাপ", "GPS Measurement",
        "লাইভ Lat/Lon ও একুরেসি", "Live GPS coordinates",
        Icons.Rounded.MyLocation, AccentRed, AccentRedBg
    ),
    BOUNDARY_TRACKING(
        "boundary_tracking", ToolGroup.SURVEY_CAD,
        "Boundary Tracking", "Walk Boundary",
        "হাঁটতে হাঁটতে সীমানা ও ক্ষেত্রফল", "Live GPS walk perimeter",
        Icons.Rounded.DirectionsWalk, AccentTeal, AccentTealBg
    ),
    CAD_EDITOR(
        "cad_editor", ToolGroup.SURVEY_CAD,
        "CAD Drawing Editor", "CAD Editor & DXF",
        "ভেক্টর নকশা ও DXF/PDF এক্সপোর্ট", "Interactive CAD & DXF",
        Icons.Rounded.Draw, AccentIndigo, AccentIndigoBg, isProFeature = true
    );

    companion object {
        fun fromId(id: String): SurveyToolId = entries.firstOrNull { it.id == id } ?: AUTO_NOKSHA
    }
}

object LandConstants {
    val landClassesBn = listOf("নাল", "ভিটি", "বাড়ি", "পুকুর", "ডোবা", "চালা", "বাগান", "বাণিজ্যিক", "রাস্তা")
    val recordTypes = listOf("CS", "SA", "RS", "BS", "BRS")
    val projectStatusesBn = listOf("চলমান", "পরিমাপ সম্পন্ন", "রিপোর্ট তৈরি", "চূড়ান্ত")
}

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val accuracyMeters: Float,
    val timestampMs: Long,
)
