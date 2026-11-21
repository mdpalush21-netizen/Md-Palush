package com.example.data.local

import com.example.core.math.SurveyPoint2D
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class SurveyRepository(
    private val dao: SurveyDao,
    val preferences: SurveyPreferences,
) {
    val projects: Flow<List<SurveyProjectEntity>> = dao.observeProjects()
    val khatians: Flow<List<KhatianRecordEntity>> = dao.observeKhatians()
    val csBsRecords: Flow<List<CsBsRecordEntity>> = dao.observeCsBsRecords()

    suspend fun saveProject(project: SurveyProjectEntity): Long {
        val now = System.currentTimeMillis()
        return if (project.id == 0L) {
            dao.insertProject(project.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateProject(project.copy(updatedAt = now))
            project.id
        }
    }

    suspend fun duplicateProject(project: SurveyProjectEntity): Long {
        val now = System.currentTimeMillis()
        return dao.insertProject(
            project.copy(
                id = 0L,
                name = "${project.name} (কপি)",
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun deleteProject(id: Long) = dao.deleteProjectById(id)

    suspend fun saveKhatian(record: KhatianRecordEntity): Long {
        val now = System.currentTimeMillis()
        return if (record.id == 0L) {
            dao.insertKhatian(record.copy(updatedAt = now))
        } else {
            dao.updateKhatian(record.copy(updatedAt = now))
            record.id
        }
    }

    suspend fun deleteKhatian(id: Long) = dao.deleteKhatianById(id)

    suspend fun saveCsBs(record: CsBsRecordEntity): Long {
        val now = System.currentTimeMillis()
        return if (record.id == 0L) {
            dao.insertCsBs(record.copy(updatedAt = now))
        } else {
            dao.updateCsBs(record.copy(updatedAt = now))
            record.id
        }
    }

    suspend fun deleteCsBs(id: Long) = dao.deleteCsBsById(id)

    suspend fun exportBackupJson(): String {
        val allProjects = dao.getAllProjectsOnce()
        val allKhatians = dao.getAllKhatiansOnce()
        val allCsBs = dao.getAllCsBsOnce()

        val root = JSONObject()
        root.put("app", "HowladerDigitalSurvey")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val projArr = JSONArray()
        allProjects.forEach { p ->
            projArr.put(JSONObject().apply {
                put("name", p.name)
                put("ownerName", p.ownerName)
                put("fatherOrHusbandName", p.fatherOrHusbandName)
                put("district", p.district)
                put("upazila", p.upazila)
                put("mouza", p.mouza)
                put("jlNumber", p.jlNumber)
                put("khatianNumber", p.khatianNumber)
                put("dagNumber", p.dagNumber)
                put("landClass", p.landClass)
                put("recordType", p.recordType)
                put("areaSqFt", p.areaSqFt)
                put("perimeterFt", p.perimeterFt)
                put("status", p.status)
                put("notes", p.notes)
                put("verticesJson", p.verticesJson)
                put("measurementsSummary", p.measurementsSummary)
                put("partitionSummary", p.partitionSummary)
                put("deedPhotoUrisJson", p.deedPhotoUrisJson)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("projects", projArr)

        val khatArr = JSONArray()
        allKhatians.forEach { k ->
            khatArr.put(JSONObject().apply {
                put("ownerName", k.ownerName)
                put("fatherOrHusbandName", k.fatherOrHusbandName)
                put("district", k.district)
                put("upazila", k.upazila)
                put("mouza", k.mouza)
                put("jlNumber", k.jlNumber)
                put("khatianNumber", k.khatianNumber)
                put("dagNumber", k.dagNumber)
                put("areaShotok", k.areaShotok)
                put("landClass", k.landClass)
                put("recordType", k.recordType)
                put("photoUrisJson", k.photoUrisJson)
                put("notes", k.notes)
                put("updatedAt", k.updatedAt)
            })
        }
        root.put("khatians", khatArr)

        val csArr = JSONArray()
        allCsBs.forEach { c ->
            csArr.put(JSONObject().apply {
                put("mouza", c.mouza)
                put("csDag", c.csDag)
                put("saDag", c.saDag)
                put("rsDag", c.rsDag)
                put("bsDag", c.bsDag)
                put("brsDag", c.brsDag)
                put("csAreaShotok", c.csAreaShotok)
                put("rsAreaShotok", c.rsAreaShotok)
                put("bsAreaShotok", c.bsAreaShotok)
                put("ownerTrail", c.ownerTrail)
                put("mutationNote", c.mutationNote)
                put("updatedAt", c.updatedAt)
            })
        }
        root.put("csBsRecords", csArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String): Result<Int> = runCatching {
        val root = JSONObject(jsonText)
        var count = 0
        val projArr = root.optJSONArray("projects") ?: JSONArray()
        for (i in 0 until projArr.length()) {
            val o = projArr.getJSONObject(i)
            dao.insertProject(
                SurveyProjectEntity(
                    name = o.optString("name", "Imported Project"),
                    ownerName = o.optString("ownerName"),
                    fatherOrHusbandName = o.optString("fatherOrHusbandName"),
                    district = o.optString("district"),
                    upazila = o.optString("upazila"),
                    mouza = o.optString("mouza"),
                    jlNumber = o.optString("jlNumber"),
                    khatianNumber = o.optString("khatianNumber"),
                    dagNumber = o.optString("dagNumber"),
                    landClass = o.optString("landClass", "নাল"),
                    recordType = o.optString("recordType", "RS"),
                    areaSqFt = o.optDouble("areaSqFt", 0.0),
                    perimeterFt = o.optDouble("perimeterFt", 0.0),
                    status = o.optString("status", "চলমান"),
                    notes = o.optString("notes"),
                    verticesJson = o.optString("verticesJson", "[]"),
                    measurementsSummary = o.optString("measurementsSummary"),
                    partitionSummary = o.optString("partitionSummary"),
                    deedPhotoUrisJson = o.optString("deedPhotoUrisJson", "[]"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                )
            )
            count++
        }
        val khatArr = root.optJSONArray("khatians") ?: JSONArray()
        for (i in 0 until khatArr.length()) {
            val o = khatArr.getJSONObject(i)
            dao.insertKhatian(
                KhatianRecordEntity(
                    ownerName = o.optString("ownerName", "অজানা মালিক"),
                    fatherOrHusbandName = o.optString("fatherOrHusbandName"),
                    district = o.optString("district"),
                    upazila = o.optString("upazila"),
                    mouza = o.optString("mouza"),
                    jlNumber = o.optString("jlNumber"),
                    khatianNumber = o.optString("khatianNumber"),
                    dagNumber = o.optString("dagNumber"),
                    areaShotok = o.optDouble("areaShotok", 0.0),
                    landClass = o.optString("landClass", "নাল"),
                    recordType = o.optString("recordType", "RS"),
                    photoUrisJson = o.optString("photoUrisJson", "[]"),
                    notes = o.optString("notes"),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                )
            )
            count++
        }
        val csArr = root.optJSONArray("csBsRecords") ?: JSONArray()
        for (i in 0 until csArr.length()) {
            val o = csArr.getJSONObject(i)
            dao.insertCsBs(
                CsBsRecordEntity(
                    mouza = o.optString("mouza", "মৌজা"),
                    csDag = o.optString("csDag"),
                    saDag = o.optString("saDag"),
                    rsDag = o.optString("rsDag"),
                    bsDag = o.optString("bsDag"),
                    brsDag = o.optString("brsDag"),
                    csAreaShotok = o.optDouble("csAreaShotok", 0.0),
                    rsAreaShotok = o.optDouble("rsAreaShotok", 0.0),
                    bsAreaShotok = o.optDouble("bsAreaShotok", 0.0),
                    ownerTrail = o.optString("ownerTrail"),
                    mutationNote = o.optString("mutationNote"),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                )
            )
            count++
        }
        count
    }

    companion object {
        fun encodePoints(points: List<SurveyPoint2D>): String {
            val arr = JSONArray()
            points.forEach { pt ->
                arr.put(JSONObject().apply {
                    put("x", pt.x)
                    put("y", pt.y)
                    put("label", pt.label)
                    put("elevation", pt.elevation)
                    pt.lat?.let { put("lat", it) }
                    pt.lon?.let { put("lon", it) }
                })
            }
            return arr.toString()
        }

        fun decodePoints(json: String): List<SurveyPoint2D> {
            return runCatching {
                val arr = JSONArray(json)
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    SurveyPoint2D(
                        x = o.optDouble("x", 0.0),
                        y = o.optDouble("y", 0.0),
                        label = o.optString("label", "P${idx + 1}"),
                        elevation = o.optDouble("elevation", 0.0),
                        lat = if (o.has("lat")) o.optDouble("lat") else null,
                        lon = if (o.has("lon")) o.optDouble("lon") else null,
                    )
                }
            }.getOrDefault(emptyList())
        }

        fun encodeStringList(items: List<String>): String {
            val arr = JSONArray()
            items.forEach { arr.put(it) }
            return arr.toString()
        }

        fun decodeStringList(json: String): List<String> {
            return runCatching {
                val arr = JSONArray(json)
                List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
        }
    }
}
