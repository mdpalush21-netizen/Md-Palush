package com.example.core.math

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PartitionPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SurveyPoint2D(
    val x: Double, // Easting or X in feet
    val y: Double, // Northing or Y in feet
    val label: String = "",
    val elevation: Double = 0.0,
    val lat: Double? = null,
    val lon: Double? = null,
)

data class TriangleResult(
    val isValid: Boolean,
    val areaSqFt: Double,
    val perimeterFt: Double,
    val semiPerimeterFt: Double,
    val angleADeg: Double,
    val angleBDeg: Double,
    val angleCDeg: Double,
    val altitudeOnA: Double,
    val vertices: List<SurveyPoint2D>,
    val errorReason: String? = null,
)

data class QuadResult(
    val isValid: Boolean,
    val exactAreaSqFt: Double,
    val averageMethodAreaSqFt: Double,
    val differenceSqFt: Double,
    val perimeterFt: Double,
    val angleNW: Double,
    val angleNE: Double,
    val angleSE: Double,
    val angleSW: Double,
    val vertices: List<SurveyPoint2D>,
    val computedDiagonalFt: Double,
    val errorReason: String? = null,
)

data class TieOffsetRow(
    val chainageFt: Double,
    val leftOffsetFt: Double,
    val rightOffsetFt: Double,
)

data class TraverseLegInput(
    val station: String,
    val bearingDeg: Double,
    val distanceFt: Double,
)

data class TraverseResult(
    val points: List<SurveyPoint2D>,
    val adjustedPoints: List<SurveyPoint2D>,
    val totalDistanceFt: Double,
    val closingErrorFt: Double,
    val precisionRatio: String,
    val areaSqFt: Double,
)

enum class PartitionMode {
    EQUAL,
    PERCENTAGE,
    RATIO,
    CUSTOM_SHOTOK,
    ANA_GONDA,
}

enum class RoadPlacement {
    NONE,
    ONE_SIDE,
    CENTER_BOTH_SIDES,
}

data class PartnerInput(
    val id: String,
    val name: String,
    val shareValue: Double, // % or ratio or shotok depending on mode
    val anaGonda: AnaGondaShare = AnaGondaShare(),
    val color: Color = PartitionPalette[0],
)

data class PartitionPlotResult(
    val partnerId: String,
    val name: String,
    val color: Color,
    val areaSqFt: Double,
    val percentageOfNet: Double,
    val x: Double,
    val y: Double,
    val widthFt: Double,
    val heightFt: Double,
    val sideLabel: String,
)

data class RoadRect(
    val x: Double,
    val y: Double,
    val widthFt: Double,
    val heightFt: Double,
)

data class PartitionPlanResult(
    val totalLengthFt: Double,
    val totalWidthFt: Double,
    val grossAreaSqFt: Double,
    val roadAreaSqFt: Double,
    val netAreaSqFt: Double,
    val plots: List<PartitionPlotResult>,
    val roadRect: RoadRect?,
    val warningMessage: String? = null,
)

object SurveyMath {

    fun heronTriangleArea(a: Double, b: Double, c: Double): TriangleResult {
        if (a <= 0 || b <= 0 || c <= 0) {
            return TriangleResult(
                isValid = false, areaSqFt = 0.0, perimeterFt = 0.0, semiPerimeterFt = 0.0,
                angleADeg = 0.0, angleBDeg = 0.0, angleCDeg = 0.0, altitudeOnA = 0.0,
                vertices = emptyList(), errorReason = "সব বাহুর মান ০ থেকে বড় হতে হবে।"
            )
        }
        if (a + b <= c || a + c <= b || b + c <= a) {
            return TriangleResult(
                isValid = false, areaSqFt = 0.0, perimeterFt = 0.0, semiPerimeterFt = 0.0,
                angleADeg = 0.0, angleBDeg = 0.0, angleCDeg = 0.0, altitudeOnA = 0.0,
                vertices = emptyList(), errorReason = "যেকোনো দুটি বাহুর যোগফল তৃতীয় বাহুর চেয়ে বড় হতে হবে।"
            )
        }
        val s = (a + b + c) / 2.0
        val area = sqrt((s * (s - a) * (s - b) * (s - c)).coerceAtLeast(0.0))
        val cosA = ((b * b + c * c - a * a) / (2 * b * c)).coerceIn(-1.0, 1.0)
        val cosB = ((a * a + c * c - b * b) / (2 * a * c)).coerceIn(-1.0, 1.0)
        val angA = Math.toDegrees(acos(cosA))
        val angB = Math.toDegrees(acos(cosB))
        val angC = (180.0 - angA - angB).coerceAtLeast(0.0)

        // Place base 'a' on X axis from (0,0) to (a,0), vertex P3 using law of cosines at P1
        val cosP1 = ((a * a + c * c - b * b) / (2 * a * c)).coerceIn(-1.0, 1.0)
        val x3 = c * cosP1
        val y3 = sqrt((c * c - x3 * x3).coerceAtLeast(0.0))

        val vertices = listOf(
            SurveyPoint2D(0.0, 0.0, "A"),
            SurveyPoint2D(a, 0.0, "B"),
            SurveyPoint2D(x3, y3, "C"),
        )
        return TriangleResult(
            isValid = true,
            areaSqFt = area,
            perimeterFt = a + b + c,
            semiPerimeterFt = s,
            angleADeg = angA,
            angleBDeg = angB,
            angleCDeg = angC,
            altitudeOnA = (2.0 * area) / a,
            vertices = vertices,
        )
    }

    fun quadrilateralAverageArea(north: Double, south: Double, east: Double, west: Double): Double {
        if (north <= 0 || south <= 0 || east <= 0 || west <= 0) return 0.0
        return ((north + south) / 2.0) * ((east + west) / 2.0)
    }

    /**
     * Computes exact area and 2D coordinates for a quadrilateral given:
     * North (top), South (bottom), East (right), West (left), and Diagonal SW->NE (or estimated orthogonal diagonal if null).
     */
    fun quadrilateralExact(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        diagonalSwToNe: Double? = null,
    ): QuadResult {
        if (north <= 0 || south <= 0 || east <= 0 || west <= 0) {
            return QuadResult(
                isValid = false, exactAreaSqFt = 0.0, averageMethodAreaSqFt = 0.0,
                differenceSqFt = 0.0, perimeterFt = 0.0,
                angleNW = 0.0, angleNE = 0.0, angleSE = 0.0, angleSW = 0.0,
                vertices = emptyList(), computedDiagonalFt = 0.0,
                errorReason = "চারটি বাহুর মান ০ থেকে বড় দিন।"
            )
        }
        val avgArea = quadrilateralAverageArea(north, south, east, west)

        // If user didn't supply diagonal, compute best-fit diagonal assuming balanced angles
        val minDiag = max(abs(south - east), abs(north - west)) + 0.01
        val maxDiag = min(south + east, north + west) - 0.01
        if (minDiag >= maxDiag) {
            return QuadResult(
                isValid = false, exactAreaSqFt = 0.0, averageMethodAreaSqFt = avgArea,
                differenceSqFt = 0.0, perimeterFt = north + south + east + west,
                angleNW = 0.0, angleNE = 0.0, angleSE = 0.0, angleSW = 0.0,
                vertices = emptyList(), computedDiagonalFt = 0.0,
                errorReason = "এই বাহুগুলো দিয়ে আবদ্ধ চতুর্ভুজ তৈরি সম্ভব নয়।"
            )
        }

        val diag = diagonalSwToNe ?: run {
            val est = sqrt(((north + south) / 2.0).pow(2) + ((east + west) / 2.0).pow(2))
            est.coerceIn(minDiag, maxDiag)
        }

        if (diag <= abs(south - east) || diag >= south + east || diag <= abs(north - west) || diag >= north + west) {
            return QuadResult(
                isValid = false, exactAreaSqFt = 0.0, averageMethodAreaSqFt = avgArea,
                differenceSqFt = 0.0, perimeterFt = north + south + east + west,
                angleNW = 0.0, angleNE = 0.0, angleSE = 0.0, angleSW = 0.0,
                vertices = emptyList(), computedDiagonalFt = diag,
                errorReason = "কর্ণের মান ${LandUnitConverter.format(minDiag)} থেকে ${LandUnitConverter.format(maxDiag)} ফুটের মধ্যে হতে হবে।"
            )
        }

        // Triangle 1: SW (0,0) -> SE (south, 0) -> NE (xNE, yNE) using sides south, east, diag
        val tri1 = heronTriangleArea(south, east, diag)
        // Triangle 2: SW (0,0) -> NE -> NW using sides diag, north, west
        val tri2 = heronTriangleArea(diag, north, west)

        if (!tri1.isValid || !tri2.isValid) {
            return QuadResult(
                isValid = false, exactAreaSqFt = 0.0, averageMethodAreaSqFt = avgArea,
                differenceSqFt = 0.0, perimeterFt = north + south + east + west,
                angleNW = 0.0, angleNE = 0.0, angleSE = 0.0, angleSW = 0.0,
                vertices = emptyList(), computedDiagonalFt = diag,
                errorReason = "কর্ণ ও বাহু দিয়ে সঠিক ত্রিভুজ গঠিত হয়নি।"
            )
        }

        // Build 2D coordinates:
        // SW = (0, 0), SE = (south, 0)
        // NE is at distance `diag` from SW and `east` from SE (in +Y half-plane)
        val xNE = (south * south + diag * diag - east * east) / (2.0 * south)
        val yNE = sqrt((diag * diag - xNE * xNE).coerceAtLeast(0.0))

        // NW is at distance `west` from SW and `north` from NE, placed on opposite side of SW-NE diagonal
        val ux = xNE / diag
        val uy = yNE / diag
        // Perpendicular left of SW->NE is (-uy, ux)
        val proj = (west * west + diag * diag - north * north) / (2.0 * diag)
        val h = sqrt((west * west - proj * proj).coerceAtLeast(0.0))
        val xNW = proj * ux - h * uy
        val yNW = proj * uy + h * ux

        val vertices = listOf(
            SurveyPoint2D(0.0, 0.0, "SW"),
            SurveyPoint2D(south, 0.0, "SE"),
            SurveyPoint2D(xNE, yNE, "NE"),
            SurveyPoint2D(xNW, yNW, "NW"),
        )

        val exactArea = tri1.areaSqFt + tri2.areaSqFt
        return QuadResult(
            isValid = true,
            exactAreaSqFt = exactArea,
            averageMethodAreaSqFt = avgArea,
            differenceSqFt = abs(avgArea - exactArea),
            perimeterFt = north + south + east + west,
            angleSW = tri1.angleBDeg + tri2.angleCDeg,
            angleSE = tri1.angleCDeg,
            angleNE = tri1.angleADeg + tri2.angleBDeg,
            angleNW = tri2.angleADeg,
            vertices = vertices,
            computedDiagonalFt = diag,
        )
    }

    fun diagonalForAngle(sideA: Double, sideB: Double, angleDeg: Double): Double {
        val rad = Math.toRadians(angleDeg)
        return sqrt((sideA * sideA + sideB * sideB - 2.0 * sideA * sideB * cos(rad)).coerceAtLeast(0.0))
    }

    fun polygonAreaShoelace(points: List<SurveyPoint2D>): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            sum += (points[i].x * points[j].y) - (points[j].x * points[i].y)
        }
        return abs(sum) / 2.0
    }

    fun polygonPerimeter(points: List<SurveyPoint2D>, closed: Boolean = true): Double {
        if (points.size < 2) return 0.0
        var perim = 0.0
        val limit = if (closed) points.size else points.size - 1
        for (i in 0 until limit) {
            val j = (i + 1) % points.size
            perim += distanceBetween(points[i], points[j])
        }
        return perim
    }

    fun distanceBetween(a: SurveyPoint2D, b: SurveyPoint2D): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    fun bearingDegrees(a: SurveyPoint2D, b: SurveyPoint2D): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val deg = Math.toDegrees(atan2(dx, dy))
        return (deg + 360.0) % 360.0
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    fun latLonToLocalFeet(coords: List<Pair<Double, Double>>): List<SurveyPoint2D> {
        if (coords.isEmpty()) return emptyList()
        val refLat = coords.map { it.first }.average()
        val refLon = coords.map { it.second }.average()
        val metersPerDegLat = 111132.92 - 559.82 * cos(2 * Math.toRadians(refLat))
        val metersPerDegLon = 111412.84 * cos(Math.toRadians(refLat))
        val metersToFeet = 3.280839895
        return coords.mapIndexed { idx, (lat, lon) ->
            val x = (lon - refLon) * metersPerDegLon * metersToFeet
            val y = (lat - refLat) * metersPerDegLat * metersToFeet
            SurveyPoint2D(x = x, y = y, label = "P${idx + 1}", lat = lat, lon = lon)
        }
    }

    fun tieLineArea(rows: List<TieOffsetRow>): Pair<Double, List<SurveyPoint2D>> {
        if (rows.size < 2) return 0.0 to emptyList()
        val sorted = rows.sortedBy { it.chainageFt }
        var area = 0.0
        for (i in 0 until sorted.size - 1) {
            val dx = sorted[i + 1].chainageFt - sorted[i].chainageFt
            val w1 = sorted[i].leftOffsetFt + sorted[i].rightOffsetFt
            val w2 = sorted[i + 1].leftOffsetFt + sorted[i + 1].rightOffsetFt
            area += dx * ((w1 + w2) / 2.0)
        }
        val top = sorted.mapIndexed { idx, r -> SurveyPoint2D(r.chainageFt, r.leftOffsetFt, "L${idx + 1}") }
        val bottom = sorted.reversed().mapIndexed { idx, r -> SurveyPoint2D(r.chainageFt, -r.rightOffsetFt, "R${sorted.size - idx}") }
        return area to (top + bottom)
    }

    fun computeTraverse(legs: List<TraverseLegInput>): TraverseResult {
        if (legs.isEmpty()) {
            return TraverseResult(emptyList(), emptyList(), 0.0, 0.0, "N/A", 0.0)
        }
        val raw = mutableListOf(SurveyPoint2D(0.0, 0.0, legs.first().station.ifBlank { "S1" }))
        var cx = 0.0
        var cy = 0.0
        var totalDist = 0.0
        legs.forEachIndexed { idx, leg ->
            val rad = Math.toRadians(leg.bearingDeg)
            val dx = leg.distanceFt * sin(rad)
            val dy = leg.distanceFt * cos(rad)
            cx += dx
            cy += dy
            totalDist += leg.distanceFt
            val label = if (idx < legs.lastIndex) legs[idx + 1].station.ifBlank { "S${idx + 2}" } else "S1'"
            raw.add(SurveyPoint2D(cx, cy, label))
        }
        val closingError = sqrt(cx * cx + cy * cy)
        val ratio = if (closingError > 1e-6) "1:${(totalDist / closingError).toInt()}" else "Perfect (0 err)"

        // Bowditch adjustment
        val adjusted = mutableListOf(SurveyPoint2D(0.0, 0.0, raw.first().label))
        var cumDist = 0.0
        var ax = 0.0
        var ay = 0.0
        legs.forEachIndexed { idx, leg ->
            if (idx == legs.lastIndex) return@forEachIndexed
            val rad = Math.toRadians(leg.bearingDeg)
            val dx = leg.distanceFt * sin(rad)
            val dy = leg.distanceFt * cos(rad)
            val corrX = if (totalDist > 0) -cx * (leg.distanceFt / totalDist) else 0.0
            val corrY = if (totalDist > 0) -cy * (leg.distanceFt / totalDist) else 0.0
            ax += dx + corrX
            ay += dy + corrY
            cumDist += leg.distanceFt
            adjusted.add(SurveyPoint2D(ax, ay, "S${idx + 2}"))
        }
        val area = polygonAreaShoelace(adjusted)
        return TraverseResult(raw, adjusted, totalDist, closingError, ratio, area)
    }

    fun partitionLand(
        totalLengthFt: Double,
        totalWidthFt: Double,
        partners: List<PartnerInput>,
        mode: PartitionMode,
        roadWidthFt: Double,
        roadPlacement: RoadPlacement,
        cutAlongLength: Boolean,
    ): PartitionPlanResult {
        val len = totalLengthFt.coerceAtLeast(1.0)
        val wid = totalWidthFt.coerceAtLeast(1.0)
        val gross = len * wid
        if (partners.isEmpty()) {
            return PartitionPlanResult(len, wid, gross, 0.0, gross, emptyList(), null, "কমপক্ষে ১ জন অংশীদার যোগ করুন।")
        }

        val roadW = if (roadPlacement == RoadPlacement.NONE) 0.0 else roadWidthFt.coerceIn(0.0, min(len, wid) * 0.5)
        val roadRect: RoadRect?
        val roadArea: Double
        val netLen: Double
        val netWid: Double

        when (roadPlacement) {
            RoadPlacement.NONE -> {
                roadRect = null
                roadArea = 0.0
                netLen = len
                netWid = wid
            }
            RoadPlacement.ONE_SIDE -> {
                if (cutAlongLength) {
                    // Road runs horizontally along bottom edge
                    roadRect = RoadRect(0.0, wid - roadW, len, roadW)
                    roadArea = len * roadW
                    netLen = len
                    netWid = (wid - roadW).coerceAtLeast(1.0)
                } else {
                    // Road runs vertically along left edge
                    roadRect = RoadRect(0.0, 0.0, roadW, wid)
                    roadArea = roadW * wid
                    netLen = (len - roadW).coerceAtLeast(1.0)
                    netWid = wid
                }
            }
            RoadPlacement.CENTER_BOTH_SIDES -> {
                if (cutAlongLength) {
                    val midY = (wid - roadW) / 2.0
                    roadRect = RoadRect(0.0, midY, len, roadW)
                    roadArea = len * roadW
                    netLen = len
                    netWid = (wid - roadW).coerceAtLeast(1.0)
                } else {
                    val midX = (len - roadW) / 2.0
                    roadRect = RoadRect(midX, 0.0, roadW, wid)
                    roadArea = roadW * wid
                    netLen = (len - roadW).coerceAtLeast(1.0)
                    netWid = wid
                }
            }
        }

        val netArea = (gross - roadArea).coerceAtLeast(0.0)
        val weights = when (mode) {
            PartitionMode.EQUAL -> List(partners.size) { 1.0 }
            PartitionMode.PERCENTAGE -> partners.map { it.shareValue.coerceAtLeast(0.0) }
            PartitionMode.RATIO -> partners.map { it.shareValue.coerceAtLeast(0.0) }
            PartitionMode.CUSTOM_SHOTOK -> partners.map {
                LandUnitConverter.toSqFeet(it.shareValue.coerceAtLeast(0.0), LandUnit.SHOTOK)
            }
            PartitionMode.ANA_GONDA -> partners.map { it.anaGonda.fractionOfWhole.coerceAtLeast(0.0) }
        }
        val weightSum = weights.sum().let { if (it <= 0.0) partners.size.toDouble() else it }
        val normalized = if (weights.sum() <= 0.0) List(partners.size) { 1.0 / partners.size } else weights.map { it / weightSum }

        var warning: String? = null
        if (mode == PartitionMode.CUSTOM_SHOTOK) {
            val requestedSqFt = weights.sum()
            if (requestedSqFt > netArea + 1.0) {
                warning = "মোট বরাদ্দকৃত জমি (${LandUnitConverter.format(requestedSqFt / 435.6)} শতাংশ) নিট জমির চেয়ে বেশি! অনুপাত অনুযায়ী সমন্বয় করা হয়েছে।"
            }
        } else if (mode == PartitionMode.PERCENTAGE) {
            val pctSum = partners.sumOf { it.shareValue }
            if (abs(pctSum - 100.0) > 0.1) {
                warning = "শতাংশের যোগফল ${LandUnitConverter.format(pctSum)}% (১০০% হওয়া উচিত)। অনুপাত সমন্বয় করা হয়েছে।"
            }
        } else if (mode == PartitionMode.ANA_GONDA) {
            val totalTil = partners.sumOf { it.anaGonda.totalTil }
            if (totalTil != 76_800L) {
                warning = "আনা-গণ্ডার মোট অংশ ১৬ আনার সমান নয় (${AnaGondaShare.fromFraction(totalTil / 76_800.0).formatBn()})। অনুপাত সমন্বয় করা হয়েছে।"
            }
        }

        val plots = mutableListOf<PartitionPlotResult>()
        if (roadPlacement == RoadPlacement.CENTER_BOTH_SIDES && partners.size >= 2) {
            // Split partners across both sides of the central road!
            val sideA = mutableListOf<Pair<Int, Double>>()
            val sideB = mutableListOf<Pair<Int, Double>>()
            var sumA = 0.0
            var sumB = 0.0
            normalized.forEachIndexed { idx, frac ->
                if (sumA <= sumB) {
                    sideA.add(idx to frac)
                    sumA += frac
                } else {
                    sideB.add(idx to frac)
                    sumB += frac
                }
            }
            val totalSides = (sumA + sumB).coerceAtLeast(1e-6)
            if (cutAlongLength) {
                // Horizontal central road: Side A above (0..topH), Side B below (midY+roadW .. wid)
                val topH = netWid * (sumA / totalSides)
                val botH = netWid - topH
                val botY = topH + roadW
                var curX = 0.0
                sideA.forEach { (idx, frac) ->
                    val p = partners[idx]
                    val w = if (sumA > 0) len * (frac / sumA) else 0.0
                    plots.add(
                        PartitionPlotResult(
                            p.id, p.name, p.color, w * topH, frac * 100.0,
                            curX, 0.0, w, topH, "রাস্তার উত্তর পাশ"
                        )
                    )
                    curX += w
                }
                curX = 0.0
                sideB.forEach { (idx, frac) ->
                    val p = partners[idx]
                    val w = if (sumB > 0) len * (frac / sumB) else 0.0
                    plots.add(
                        PartitionPlotResult(
                            p.id, p.name, p.color, w * botH, frac * 100.0,
                            curX, botY, w, botH, "রাস্তার দক্ষিণ পাশ"
                        )
                    )
                    curX += w
                }
                val adjustedRoad = RoadRect(0.0, topH, len, roadW)
                return PartitionPlanResult(len, wid, gross, roadArea, netArea, plots, adjustedRoad, warning)
            } else {
                // Vertical central road: Side A left, Side B right
                val leftW = netLen * (sumA / totalSides)
                val rightW = netLen - leftW
                val rightX = leftW + roadW
                var curY = 0.0
                sideA.forEach { (idx, frac) ->
                    val p = partners[idx]
                    val h = if (sumA > 0) wid * (frac / sumA) else 0.0
                    plots.add(
                        PartitionPlotResult(
                            p.id, p.name, p.color, leftW * h, frac * 100.0,
                            0.0, curY, leftW, h, "রাস্তার পশ্চিম পাশ"
                        )
                    )
                    curY += h
                }
                curY = 0.0
                sideB.forEach { (idx, frac) ->
                    val p = partners[idx]
                    val h = if (sumB > 0) wid * (frac / sumB) else 0.0
                    plots.add(
                        PartitionPlotResult(
                            p.id, p.name, p.color, rightW * h, frac * 100.0,
                            rightX, curY, rightW, h, "রাস্তার পূর্ব পাশ"
                        )
                    )
                    curY += h
                }
                val adjustedRoad = RoadRect(leftW, 0.0, roadW, wid)
                return PartitionPlanResult(len, wid, gross, roadArea, netArea, plots, adjustedRoad, warning)
            }
        } else {
            val startX = if (roadPlacement == RoadPlacement.ONE_SIDE && !cutAlongLength) roadW else 0.0
            var cursorX = startX
            var cursorY = 0.0
            partners.forEachIndexed { idx, p ->
                val frac = normalized[idx]
                if (cutAlongLength) {
                    val plotW = netLen * frac
                    plots.add(
                        PartitionPlotResult(
                            p.id, p.name, p.color, plotW * netWid, frac * 100.0,
                            cursorX, 0.0, plotW, netWid, "অংশ #${idx + 1}"
                        )
                    )
                    cursorX += plotW
                } else {
                    val plotH = netWid * frac
                    plots.add(
                        PartitionPlotResult(
                            p.id, p.name, p.color, netLen * plotH, frac * 100.0,
                            startX, cursorY, netLen, plotH, "অংশ #${idx + 1}"
                        )
                    )
                    cursorY += plotH
                }
            }
            return PartitionPlanResult(len, wid, gross, roadArea, netArea, plots, roadRect, warning)
        }
    }
}
