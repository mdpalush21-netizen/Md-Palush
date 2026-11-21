package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survey_projects")
data class SurveyProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val ownerName: String = "",
    val fatherOrHusbandName: String = "",
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val khatianNumber: String = "",
    val dagNumber: String = "",
    val landClass: String = "নাল",
    val recordType: String = "RS",
    val areaSqFt: Double = 0.0,
    val perimeterFt: Double = 0.0,
    val status: String = "চলমান",
    val notes: String = "",
    val verticesJson: String = "[]",
    val measurementsSummary: String = "",
    val partitionSummary: String = "",
    val deedPhotoUrisJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "khatian_records")
data class KhatianRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ownerName: String,
    val fatherOrHusbandName: String = "",
    val district: String = "",
    val upazila: String = "",
    val mouza: String = "",
    val jlNumber: String = "",
    val khatianNumber: String = "",
    val dagNumber: String = "",
    val areaShotok: Double = 0.0,
    val landClass: String = "নাল",
    val recordType: String = "RS",
    val photoUrisJson: String = "[]",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cs_bs_records")
data class CsBsRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mouza: String,
    val csDag: String = "",
    val saDag: String = "",
    val rsDag: String = "",
    val bsDag: String = "",
    val brsDag: String = "",
    val csAreaShotok: Double = 0.0,
    val rsAreaShotok: Double = 0.0,
    val bsAreaShotok: Double = 0.0,
    val ownerTrail: String = "",
    val mutationNote: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
