package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.*
import pl.oki.frostalert.utils.GardenSeasonalTips
import javax.inject.Inject

data class GardenUiState(
    val allPlants: List<Plant> = emptyList(),
    val userPlants: List<Plant> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val userPlantIds: Set<Int> = emptySet(),
    val isSeeded: Boolean = false,
    val plantsAtRisk: List<Plant> = emptyList(),
    val lastMinTemp: Double? = null,
    val lastWateringByPlantId: Map<Int, Long> = emptyMap(),
    val seasonalTips: List<GardenSeasonalTips.MonthlyTip> = emptyList(),
    val currentMonthName: String = ""
)

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val plantDao: PlantDao,
    private val userPlantDao: UserPlantDao,
    private val temperatureDao: TemperatureDao,
    private val wateringLogDao: WateringLogDao
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val gardenState: StateFlow<GardenUiState> = combine(
        plantDao.getAllPlants(),
        userPlantDao.getUserPlants(),
        plantDao.getAllCategories(),
        _selectedCategory,
        _searchQuery,
        temperatureDao.getRecentRecords(),
        wateringLogDao.getLastWateringPerPlant()
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allPlants = args[0] as List<Plant>
        @Suppress("UNCHECKED_CAST")
        val userPlants = args[1] as List<Plant>
        @Suppress("UNCHECKED_CAST")
        val categories = args[2] as List<String>
        val selectedCat = args[3] as String?
        val query = args[4] as String
        @Suppress("UNCHECKED_CAST")
        val recentRecords = args[5] as List<TemperatureRecord>
        @Suppress("UNCHECKED_CAST")
        val lastWateringEntries = args[6] as List<LastWateringEntry>

        val userPlantIds = userPlants.map { it.id }.toSet()
        val filtered = allPlants.filter { plant ->
            val matchesCategory = selectedCat == null || plant.category == selectedCat
            val matchesQuery = query.isBlank() || plant.name.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        val lastMinTemp = recentRecords.firstOrNull()?.minTemp
        val plantsAtRisk = if (lastMinTemp != null) {
            userPlants.filter { plant -> plant.frostThresholdCelsius >= lastMinTemp }
        } else {
            emptyList()
        }

        val lastWateringByPlantId = lastWateringEntries.associate { it.plantId to it.lastTimestamp }

        GardenUiState(
            allPlants = filtered,
            userPlants = userPlants,
            categories = categories,
            selectedCategory = selectedCat,
            searchQuery = query,
            userPlantIds = userPlantIds,
            isSeeded = allPlants.isNotEmpty(),
            plantsAtRisk = plantsAtRisk,
            lastMinTemp = lastMinTemp,
            lastWateringByPlantId = lastWateringByPlantId,
            seasonalTips = GardenSeasonalTips.getCurrentMonthTips(),
            currentMonthName = GardenSeasonalTips.getMonthName()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GardenUiState()
    )

    init {
        seedPlantsIfNeeded()
    }

    private fun seedPlantsIfNeeded() {
        viewModelScope.launch {
            val count = plantDao.getPlantCount()
            if (count == 0) {
                plantDao.insertAll(PlantSeeder.getDefaultPlants())
            }
        }
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePlantInGarden(plant: Plant) {
        viewModelScope.launch {
            val currentState = gardenState.value
            if (plant.id in currentState.userPlantIds) {
                userPlantDao.removeUserPlant(plant.id)
                wateringLogDao.deleteForPlant(plant.id)
            } else {
                userPlantDao.addUserPlant(UserPlant(plantId = plant.id))
            }
        }
    }

    fun markAsWatered(plantId: Int) {
        viewModelScope.launch {
            wateringLogDao.insert(WateringLog(plantId = plantId))
        }
    }
}

