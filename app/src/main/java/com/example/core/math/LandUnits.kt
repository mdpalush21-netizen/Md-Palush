package com.example.core.math

import kotlin.math.roundToInt

enum class LandUnit(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val shortBn: String,
    val sqFeetFactor: Double,
) {
    SHOTOK("shotok", "শতাংশ / শতক", "Shotok / Decimal", "শতাংশ", 435.6),
    DECIMAL("decimal", "ডেসিমেল", "Decimal", "ডেসিমেল", 435.6),
    KATHA("katha", "কাঠা", "Katha", "কাঠা", 720.0),
    BIGHA("bigha", "বিঘা", "Bigha", "বিঘা", 14400.0),
    ACRE("acre", "একর", "Acre", "একর", 43560.0),
    SQ_FEET("sqft", "বর্গফুট", "Sq. Feet", "বর্গফুট", 1.0),
    SQ_METER("sqm", "বর্গমিটার", "Sq. Meter", "বর্গমি.", 10.7639104167),
    SQ_YARD("sqyd", "বর্গগজ", "Sq. Yard", "বর্গগজ", 9.0),
    HECTARE("hectare", "হেক্টর", "Hectare", "হেক্টর", 107639.104167);

    companion object {
        fun fromId(id: String): LandUnit = entries.firstOrNull { it.id == id } ?: SHOTOK
    }
}

enum class LinearUnit(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val feetFactor: Double,
) {
    FEET("ft", "ফুট", "Feet", 1.0),
    INCH("in", "ইঞ্চি", "Inch", 1.0 / 12.0),
    METER("m", "মিটার", "Meter", 3.280839895),
    YARD("yd", "গজ", "Yard", 3.0),
    LINK("link", "লিঙ্ক (Gunter)", "Link", 0.66),
    HATH("hath", "হাত (১৮ ইঞ্চি)", "Hand (18\")", 1.5),
    GUNIA("gunia", "গুনিয়া (১৬\"=১মাইল)", "Gunia (16\"=1mi)", 3.3);

    companion object {
        fun fromId(id: String): LinearUnit = entries.firstOrNull { it.id == id } ?: FEET
    }
}

data class LandConversionSummary(
    val sqFeet: Double,
    val shotok: Double,
    val decimal: Double,
    val katha: Double,
    val bigha: Double,
    val acre: Double,
    val sqMeter: Double,
    val sqYard: Double,
    val hectare: Double,
) {
    fun valueFor(unit: LandUnit): Double = when (unit) {
        LandUnit.SHOTOK -> shotok
        LandUnit.DECIMAL -> decimal
        LandUnit.KATHA -> katha
        LandUnit.BIGHA -> bigha
        LandUnit.ACRE -> acre
        LandUnit.SQ_FEET -> sqFeet
        LandUnit.SQ_METER -> sqMeter
        LandUnit.SQ_YARD -> sqYard
        LandUnit.HECTARE -> hectare
    }
}

object LandUnitConverter {
    fun toSqFeet(value: Double, unit: LandUnit): Double = value * unit.sqFeetFactor

    fun fromSqFeet(sqFeet: Double, unit: LandUnit): Double =
        if (unit.sqFeetFactor == 0.0) 0.0 else sqFeet / unit.sqFeetFactor

    fun convert(value: Double, from: LandUnit, to: LandUnit): Double =
        fromSqFeet(toSqFeet(value, from), to)

    fun summarizeFromSqFeet(sqFeet: Double): LandConversionSummary {
        val safeSqFt = sqFeet.coerceAtLeast(0.0)
        return LandConversionSummary(
            sqFeet = safeSqFt,
            shotok = safeSqFt / LandUnit.SHOTOK.sqFeetFactor,
            decimal = safeSqFt / LandUnit.DECIMAL.sqFeetFactor,
            katha = safeSqFt / LandUnit.KATHA.sqFeetFactor,
            bigha = safeSqFt / LandUnit.BIGHA.sqFeetFactor,
            acre = safeSqFt / LandUnit.ACRE.sqFeetFactor,
            sqMeter = safeSqFt / LandUnit.SQ_METER.sqFeetFactor,
            sqYard = safeSqFt / LandUnit.SQ_YARD.sqFeetFactor,
            hectare = safeSqFt / LandUnit.HECTARE.sqFeetFactor,
        )
    }

    fun feetInchToFeet(feet: Double, inches: Double): Double = feet + (inches / 12.0)

    fun toFeet(value: Double, unit: LinearUnit): Double = value * unit.feetFactor

    fun fromFeet(feet: Double, unit: LinearUnit): Double = feet / unit.feetFactor

    fun format(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return "0.00"
        return String.format(java.util.Locale.US, "%.${decimals}f", value)
    }
}

/**
 * Traditional Bangladeshi Ana-Gonda-Kora-Kranti-Til system:
 * 1 Whole (16 Ana) = 16 * 20 * 4 * 3 * 20 = 76,800 Til
 */
data class AnaGondaShare(
    val ana: Int = 0,
    val gonda: Int = 0,
    val kora: Int = 0,
    val kranti: Int = 0,
    val til: Int = 0,
) {
    val totalTil: Long
        get() = (ana.toLong() * 4800L) +
            (gonda.toLong() * 240L) +
            (kora.toLong() * 60L) +
            (kranti.toLong() * 20L) +
            til.toLong()

    val fractionOfWhole: Double
        get() = totalTil.toDouble() / TOTAL_TIL_IN_WHOLE

    val percentage: Double
        get() = fractionOfWhole * 100.0

    fun formatBn(): String = "${ana}আ ${gonda}গ ${kora}ক ${kranti}ক্রা ${til}তি"

    companion object {
        const val TOTAL_TIL_IN_WHOLE = 76_800.0

        fun fromFraction(fraction: Double): AnaGondaShare {
            val clamped = fraction.coerceIn(0.0, 1.0)
            var rem = (clamped * TOTAL_TIL_IN_WHOLE).roundToInt().coerceAtLeast(0)
            val ana = rem / 4800
            rem %= 4800
            val gonda = rem / 240
            rem %= 240
            val kora = rem / 60
            rem %= 60
            val kranti = rem / 20
            val til = rem % 20
            return AnaGondaShare(ana, gonda, kora, kranti, til)
        }
    }
}
