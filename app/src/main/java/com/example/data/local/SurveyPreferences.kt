package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.math.LandUnit
import com.example.model.SurveyToolId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.surveyDataStore by preferencesDataStore(name = "howlader_survey_prefs")

data class UserPreferencesState(
    val isBengali: Boolean = true,
    val defaultUnit: LandUnit = LandUnit.SHOTOK,
    val pinnedToolIds: Set<String> = setOf(
        SurveyToolId.AUTO_NOKSHA.id,
        SurveyToolId.LAND_PARTITION.id,
        SurveyToolId.AREA_CALCULATOR.id,
        SurveyToolId.GPS_COORDINATES.id,
    ),
    val isProUnlocked: Boolean = false,
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "",
    val surveyorName: String = "মোঃ হাওলাদার সার্ভেয়ার",
    val surveyorTitle: String = "ডিজিটাল সার্ভেয়ার ও আমিন",
    val surveyorPhone: String = "",
    val surveyorEmail: String = "",
    val surveyorOffice: String = "",
)

class SurveyPreferences(private val context: Context) {
    private object Keys {
        val IS_BENGALI = booleanPreferencesKey("is_bengali")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val PINNED_TOOLS = stringSetPreferencesKey("pinned_tools")
        val IS_PRO_UNLOCKED = booleanPreferencesKey("is_pro_unlocked")
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val SURVEYOR_NAME = stringPreferencesKey("surveyor_name")
        val SURVEYOR_TITLE = stringPreferencesKey("surveyor_title")
        val SURVEYOR_PHONE = stringPreferencesKey("surveyor_phone")
        val SURVEYOR_EMAIL = stringPreferencesKey("surveyor_email")
        val SURVEYOR_OFFICE = stringPreferencesKey("surveyor_office")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.surveyDataStore.data.map { prefs ->
        UserPreferencesState(
            isBengali = prefs[Keys.IS_BENGALI] ?: true,
            defaultUnit = LandUnit.fromId(prefs[Keys.DEFAULT_UNIT] ?: LandUnit.SHOTOK.id),
            pinnedToolIds = prefs[Keys.PINNED_TOOLS] ?: setOf(
                SurveyToolId.AUTO_NOKSHA.id,
                SurveyToolId.LAND_PARTITION.id,
                SurveyToolId.AREA_CALCULATOR.id,
                SurveyToolId.GPS_COORDINATES.id,
            ),
            isProUnlocked = prefs[Keys.IS_PRO_UNLOCKED] ?: false,
            pinLockEnabled = prefs[Keys.PIN_LOCK_ENABLED] ?: false,
            pinCode = prefs[Keys.PIN_CODE] ?: "",
            surveyorName = prefs[Keys.SURVEYOR_NAME] ?: "হাওলাদার ডিজিটাল সার্ভেয়ার",
            surveyorTitle = prefs[Keys.SURVEYOR_TITLE] ?: "লাইসেন্সপ্রাপ্ত আমিন ও সার্ভেয়ার",
            surveyorPhone = prefs[Keys.SURVEYOR_PHONE] ?: "",
            surveyorEmail = prefs[Keys.SURVEYOR_EMAIL] ?: "",
            surveyorOffice = prefs[Keys.SURVEYOR_OFFICE] ?: "",
        )
    }

    suspend fun setLanguage(isBengali: Boolean) {
        context.surveyDataStore.edit { it[Keys.IS_BENGALI] = isBengali }
    }

    suspend fun setDefaultUnit(unit: LandUnit) {
        context.surveyDataStore.edit { it[Keys.DEFAULT_UNIT] = unit.id }
    }

    suspend fun togglePinnedTool(toolId: String) {
        context.surveyDataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_TOOLS] ?: setOf(
                SurveyToolId.AUTO_NOKSHA.id,
                SurveyToolId.LAND_PARTITION.id,
                SurveyToolId.AREA_CALCULATOR.id,
                SurveyToolId.GPS_COORDINATES.id,
            )
            prefs[Keys.PINNED_TOOLS] = if (toolId in current) current - toolId else current + toolId
        }
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        context.surveyDataStore.edit { it[Keys.IS_PRO_UNLOCKED] = unlocked }
    }

    suspend fun setPinLock(enabled: Boolean, pin: String) {
        context.surveyDataStore.edit {
            it[Keys.PIN_LOCK_ENABLED] = enabled
            it[Keys.PIN_CODE] = pin
        }
    }

    suspend fun saveSurveyorProfile(
        name: String,
        title: String,
        phone: String,
        email: String,
        office: String,
    ) {
        context.surveyDataStore.edit {
            it[Keys.SURVEYOR_NAME] = name
            it[Keys.SURVEYOR_TITLE] = title
            it[Keys.SURVEYOR_PHONE] = phone
            it[Keys.SURVEYOR_EMAIL] = email
            it[Keys.SURVEYOR_OFFICE] = office
        }
    }
}
