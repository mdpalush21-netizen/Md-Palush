package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.math.LandUnit
import com.example.core.math.SurveyMath
import com.example.core.math.SurveyPoint2D
import com.example.data.local.CsBsRecordEntity
import com.example.data.local.KhatianRecordEntity
import com.example.data.local.SurveyProjectEntity
import com.example.data.local.SurveyRepository
import com.example.data.local.UserPreferencesState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SurveyViewModel(
    private val repository: SurveyRepository,
) : ViewModel() {

    val projects: StateFlow<List<SurveyProjectEntity>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val khatians: StateFlow<List<KhatianRecordEntity>> = repository.khatians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val csBsRecords: StateFlow<List<CsBsRecordEntity>> = repository.csBsRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<UserPreferencesState> = repository.preferences.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState())

    private val _activeDraftPoints = MutableStateFlow<List<SurveyPoint2D>>(
        listOf(
            SurveyPoint2D(0.0, 0.0, "P1"),
            SurveyPoint2D(80.0, 0.0, "P2"),
            SurveyPoint2D(85.0, 54.0, "P3"),
            SurveyPoint2D(5.0, 56.0, "P4"),
        )
    )
    val activeDraftPoints: StateFlow<List<SurveyPoint2D>> = _activeDraftPoints.asStateFlow()

    private val _selectedProject = MutableStateFlow<SurveyProjectEntity?>(null)
    val selectedProject: StateFlow<SurveyProjectEntity?> = _selectedProject.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val uiMessage = _uiMessage.asSharedFlow()

    fun setActiveDraftPoints(points: List<SurveyPoint2D>) {
        if (points.isNotEmpty()) {
            _activeDraftPoints.value = points
        }
    }

    fun selectProject(project: SurveyProjectEntity?) {
        _selectedProject.value = project
        if (project != null) {
            val pts = SurveyRepository.decodePoints(project.verticesJson)
            if (pts.isNotEmpty()) _activeDraftPoints.value = pts
        }
    }

    fun togglePinTool(toolId: String) {
        viewModelScope.launch {
            repository.preferences.togglePinnedTool(toolId)
        }
    }

    fun setLanguage(isBengali: Boolean) {
        viewModelScope.launch {
            repository.preferences.setLanguage(isBengali)
        }
    }

    fun setDefaultUnit(unit: LandUnit) {
        viewModelScope.launch {
            repository.preferences.setDefaultUnit(unit)
        }
    }

    fun setProUnlocked(unlocked: Boolean) {
        viewModelScope.launch {
            repository.preferences.setProUnlocked(unlocked)
        }
    }

    fun setPinLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            repository.preferences.setPinLock(enabled, pin)
        }
    }

    fun saveSurveyorProfile(
        name: String,
        title: String,
        phone: String,
        email: String,
        office: String,
    ) {
        viewModelScope.launch {
            repository.preferences.saveSurveyorProfile(name, title, phone, email, office)
            _uiMessage.tryEmit("প্রোফাইল সংরক্ষিত হয়েছে")
        }
    }

    fun saveProject(project: SurveyProjectEntity) {
        viewModelScope.launch {
            repository.saveProject(project)
            _uiMessage.tryEmit("প্রজেক্ট সংরক্ষিত হয়েছে")
        }
    }

    fun saveMeasurementToProject(
        projectName: String,
        ownerName: String,
        mouza: String,
        dagNo: String,
        khatianNo: String,
        areaSqFt: Double,
        points: List<SurveyPoint2D>,
        summaryText: String,
        partitionText: String = "",
    ) {
        viewModelScope.launch {
            val perimeter = SurveyMath.polygonPerimeter(points)
            val entity = SurveyProjectEntity(
                name = projectName.ifBlank { "সার্ভে প্রজেক্ট" },
                ownerName = ownerName,
                mouza = mouza,
                dagNumber = dagNo,
                khatianNumber = khatianNo,
                areaSqFt = areaSqFt,
                perimeterFt = perimeter,
                status = "পরিমাপ সম্পন্ন",
                verticesJson = SurveyRepository.encodePoints(points),
                measurementsSummary = summaryText,
                partitionSummary = partitionText,
            )
            repository.saveProject(entity)
            _uiMessage.tryEmit("প্রজেক্টে সংরক্ষণ সম্পন্ন!")
        }
    }

    fun duplicateProject(project: SurveyProjectEntity) {
        viewModelScope.launch {
            repository.duplicateProject(project)
            _uiMessage.tryEmit("প্রজেক্ট ডুপ্লিকেট করা হয়েছে")
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            _uiMessage.tryEmit("প্রজেক্ট মুছে ফেলা হয়েছে")
        }
    }

    fun saveKhatian(record: KhatianRecordEntity) {
        viewModelScope.launch {
            repository.saveKhatian(record)
            _uiMessage.tryEmit("খতিয়ান রেকর্ড সংরক্ষিত হয়েছে")
        }
    }

    fun deleteKhatian(id: Long) {
        viewModelScope.launch {
            repository.deleteKhatian(id)
            _uiMessage.tryEmit("খতিয়ান রেকর্ড মুছে ফেলা হয়েছে")
        }
    }

    fun saveCsBs(record: CsBsRecordEntity) {
        viewModelScope.launch {
            repository.saveCsBs(record)
            _uiMessage.tryEmit("CS-BS রেকর্ড সংরক্ষিত হয়েছে")
        }
    }

    fun deleteCsBs(id: Long) {
        viewModelScope.launch {
            repository.deleteCsBs(id)
            _uiMessage.tryEmit("রেকর্ড মুছে ফেলা হয়েছে")
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(jsonText: String) {
        viewModelScope.launch {
            val res = repository.importBackupJson(jsonText)
            res.onSuccess { count ->
                _uiMessage.tryEmit("$count টি রেকর্ড সফলভাবে রিস্টোর হয়েছে")
            }.onFailure {
                _uiMessage.tryEmit("ব্যাকআপ ফাইল পড়া যায়নি: ${it.message}")
            }
        }
    }

    fun loadSampleSurveyProjects() {
        viewModelScope.launch {
            val samplePts = listOf(
                SurveyPoint2D(0.0, 0.0, "P1"),
                SurveyPoint2D(92.0, 0.0, "P2"),
                SurveyPoint2D(88.0, 62.0, "P3"),
                SurveyPoint2D(4.0, 58.0, "P4"),
            )
            repository.saveProject(
                SurveyProjectEntity(
                    name = "মৌজা সোনাপুর - দাগ ৪১২ সার্ভে",
                    ownerName = "আবুল কাশেম মিয়া",
                    fatherOrHusbandName = "মৃত আঃ রশিদ",
                    district = "ঢাকা",
                    upazila = "সাভার",
                    mouza = "সোনাপুর",
                    jlNumber = "৪৫",
                    khatianNumber = "১২৮",
                    dagNumber = "৪১২",
                    landClass = "ভিটি",
                    recordType = "RS",
                    areaSqFt = SurveyMath.polygonAreaShoelace(samplePts),
                    perimeterFt = SurveyMath.polygonPerimeter(samplePts),
                    status = "পরিমাপ সম্পন্ন",
                    notes = "চার বাহু ও কর্ণ যাচাইকৃত ডিজিটাল নকশা",
                    verticesJson = SurveyRepository.encodePoints(samplePts),
                    measurementsSummary = "উত্তর ৮৪', দক্ষিণ ৯২', পূর্ব ৬২', পশ্চিম ৫৮'",
                )
            )
            repository.saveKhatian(
                KhatianRecordEntity(
                    ownerName = "আবুল কাশেম মিয়া",
                    fatherOrHusbandName = "মৃত আঃ রশিদ",
                    district = "ঢাকা",
                    upazila = "সাভার",
                    mouza = "সোনাপুর",
                    jlNumber = "৪৫",
                    khatianNumber = "১২৮",
                    dagNumber = "৪১২",
                    areaShotok = 12.18,
                    landClass = "ভিটি",
                    recordType = "RS",
                    notes = "নামজারি সম্পন্ন",
                )
            )
            repository.saveCsBs(
                CsBsRecordEntity(
                    mouza = "সোনাপুর",
                    csDag = "১০২",
                    saDag = "২১৮",
                    rsDag = "৪১২",
                    bsDag = "৭০৫",
                    brsDag = "৭০৫",
                    csAreaShotok = 12.50,
                    rsAreaShotok = 12.18,
                    bsAreaShotok = 12.18,
                    ownerTrail = "রহিম -> রশিদ -> আবুল কাশেম",
                    mutationNote = "রাস্তার জন্য ০.৩২ শতক কর্তন",
                )
            )
            _uiMessage.tryEmit("নমুনা সার্ভে ডেটা লোড হয়েছে")
        }
    }

    class Factory(private val repository: SurveyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SurveyViewModel(repository) as T
        }
    }
}
