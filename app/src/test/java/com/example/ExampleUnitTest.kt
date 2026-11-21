package com.example

import com.example.core.math.AnaGondaShare
import com.example.core.math.LandUnit
import com.example.core.math.LandUnitConverter
import com.example.core.math.PartnerInput
import com.example.core.math.PartitionMode
import com.example.core.math.RoadPlacement
import com.example.core.math.SurveyMath
import com.example.core.math.SurveyPoint2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun landUnitConversions_areAccurate() {
        val shotokSqFt = LandUnitConverter.toSqFeet(1.0, LandUnit.SHOTOK)
        assertEquals(435.6, shotokSqFt, 0.001)

        val acreInShotok = LandUnitConverter.convert(1.0, LandUnit.ACRE, LandUnit.SHOTOK)
        assertEquals(100.0, acreInShotok, 0.001)

        val bighaInKatha = LandUnitConverter.convert(1.0, LandUnit.BIGHA, LandUnit.KATHA)
        assertEquals(20.0, bighaInKatha, 0.001)
    }

    @Test
    fun anaGonda_math_isExact() {
        val half = AnaGondaShare(ana = 8)
        assertEquals(0.5, half.fractionOfWhole, 0.0001)

        val whole = AnaGondaShare(ana = 16)
        assertEquals(1.0, whole.fractionOfWhole, 0.0001)
    }

    @Test
    fun heronTriangle_and_shoelacePolygon_matchGeometry() {
        val tri = SurveyMath.heronTriangleArea(30.0, 40.0, 50.0)
        assertTrue(tri.isValid)
        assertEquals(600.0, tri.areaSqFt, 0.01)

        val rect = listOf(
            SurveyPoint2D(0.0, 0.0),
            SurveyPoint2D(80.0, 0.0),
            SurveyPoint2D(80.0, 50.0),
            SurveyPoint2D(0.0, 50.0),
        )
        assertEquals(4000.0, SurveyMath.polygonAreaShoelace(rect), 0.01)
    }

    @Test
    fun partitionLand_withCenterRoad_splitsBothSidesCleanly() {
        val plan = SurveyMath.partitionLand(
            totalLengthFt = 100.0,
            totalWidthFt = 60.0,
            partners = listOf(
                PartnerInput("1", "A", 50.0),
                PartnerInput("2", "B", 50.0),
            ),
            mode = PartitionMode.EQUAL,
            roadWidthFt = 10.0,
            roadPlacement = RoadPlacement.CENTER_BOTH_SIDES,
            cutAlongLength = true,
        )
        assertEquals(6000.0, plan.grossAreaSqFt, 0.01)
        assertEquals(1000.0, plan.roadAreaSqFt, 0.01)
        assertEquals(5000.0, plan.netAreaSqFt, 0.01)
        assertEquals(2, plan.plots.size)
        assertEquals(2500.0, plan.plots[0].areaSqFt, 0.01)
        assertEquals(2500.0, plan.plots[1].areaSqFt, 0.01)
    }
}
