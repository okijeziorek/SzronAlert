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

private data class PlantFilterState(
    val allPlants: List<Plant>,
    val userPlants: List<Plant>,
    val categories: List<String>,
    val selectedCategory: String?,
    val query: String
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

    private val plantFilterFlow: Flow<PlantFilterState> = combine(
        plantDao.getAllPlants(),
        userPlantDao.getUserPlants(),
        plantDao.getAllCategories(),
        _selectedCategory,
        _searchQuery
    ) { allPlants, userPlants, categories, selectedCat, query ->
        PlantFilterState(allPlants, userPlants, categories, selectedCat, query)
    }

    private val weatherWateringFlow: Flow<Pair<List<TemperatureRecord>, Map<Int, Long>>> = combine(
        temperatureDao.getRecentRecords(),
        wateringLogDao.getLastWateringPerPlant()
    ) { records, entries ->
        val lastWateringByPlantId = entries.associate { it.plantId to it.lastTimestamp }
        Pair(records, lastWateringByPlantId)
    }

    val gardenState: StateFlow<GardenUiState> = combine(
        plantFilterFlow,
        weatherWateringFlow
    ) { plantState, (recentRecords, lastWateringByPlantId) ->
        val userPlantIds = plantState.userPlants.map { it.id }.toSet()
        val filtered = plantState.allPlants.filter { plant ->
            val matchesCategory = plantState.selectedCategory == null || plant.category == plantState.selectedCategory
            val matchesQuery = plantState.query.isBlank() || plant.name.contains(plantState.query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        val lastMinTemp = recentRecords.firstOrNull()?.minTemp
        val plantsAtRisk = if (lastMinTemp != null) {
            plantState.userPlants.filter { plant -> plant.frostThresholdCelsius >= lastMinTemp }
        } else {
            emptyList()
        }

        val monthData = GardenSeasonalTips.getCurrentMonthData()

        GardenUiState(
            allPlants = filtered,
            userPlants = plantState.userPlants,
            categories = plantState.categories,
            selectedCategory = plantState.selectedCategory,
            searchQuery = plantState.query,
            userPlantIds = userPlantIds,
            isSeeded = plantState.allPlants.isNotEmpty(),
            plantsAtRisk = plantsAtRisk,
            lastMinTemp = lastMinTemp,
            lastWateringByPlantId = lastWateringByPlantId,
            seasonalTips = monthData.tips,
            currentMonthName = monthData.monthName
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

