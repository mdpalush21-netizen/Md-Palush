package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Query("SELECT * FROM survey_projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<SurveyProjectEntity>>

    @Query("SELECT * FROM survey_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): SurveyProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SurveyProjectEntity): Long

    @Update
    suspend fun updateProject(project: SurveyProjectEntity)

    @Query("DELETE FROM survey_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT * FROM survey_projects")
    suspend fun getAllProjectsOnce(): List<SurveyProjectEntity>

    // Khatian Records
    @Query("SELECT * FROM khatian_records ORDER BY updatedAt DESC")
    fun observeKhatians(): Flow<List<KhatianRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhatian(record: KhatianRecordEntity): Long

    @Update
    suspend fun updateKhatian(record: KhatianRecordEntity)

    @Query("DELETE FROM khatian_records WHERE id = :id")
    suspend fun deleteKhatianById(id: Long)

    @Query("SELECT * FROM khatian_records")
    suspend fun getAllKhatiansOnce(): List<KhatianRecordEntity>

    // CS-BS Tracker Records
    @Query("SELECT * FROM cs_bs_records ORDER BY updatedAt DESC")
    fun observeCsBsRecords(): Flow<List<CsBsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCsBs(record: CsBsRecordEntity): Long

    @Update
    suspend fun updateCsBs(record: CsBsRecordEntity)

    @Query("DELETE FROM cs_bs_records WHERE id = :id")
    suspend fun deleteCsBsById(id: Long)

    @Query("SELECT * FROM cs_bs_records")
    suspend fun getAllCsBsOnce(): List<CsBsRecordEntity>
}
