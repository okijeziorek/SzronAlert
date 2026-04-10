package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.*
import javax.inject.Inject

data class GardenUiState(
    val allPlants: List<Plant> = emptyList(),
    val userPlants: List<Plant> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val userPlantIds: Set<Int> = emptySet(),
    val isSeeded: Boolean = false
)

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val plantDao: PlantDao,
    private val userPlantDao: UserPlantDao
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val gardenState: StateFlow<GardenUiState> = combine(
        plantDao.getAllPlants(),
        userPlantDao.getUserPlants(),
        plantDao.getAllCategories(),
        _selectedCategory,
        _searchQuery
    ) { allPlants, userPlants, categories, selectedCat, query ->
        val userPlantIds = userPlants.map { it.id }.toSet()
        val filtered = allPlants.filter { plant ->
            val matchesCategory = selectedCat == null || plant.category == selectedCat
            val matchesQuery = query.isBlank() || plant.name.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        GardenUiState(
            allPlants = filtered,
            userPlants = userPlants,
            categories = categories,
            selectedCategory = selectedCat,
            searchQuery = query,
            userPlantIds = userPlantIds,
            isSeeded = allPlants.isNotEmpty()
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
            } else {
                userPlantDao.addUserPlant(UserPlant(plantId = plant.id))
            }
        }
    }
}
