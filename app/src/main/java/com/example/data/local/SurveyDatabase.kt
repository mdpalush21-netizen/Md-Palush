package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SurveyProjectEntity::class,
        KhatianRecordEntity::class,
        CsBsRecordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SurveyDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao

    companion object {
        @Volatile
        private var INSTANCE: SurveyDatabase? = null

        fun getInstance(context: Context): SurveyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SurveyDatabase::class.java,
                    "howlader_digital_survey.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
