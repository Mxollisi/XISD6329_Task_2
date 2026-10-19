package za.co.giantautoupholstery.ui.services

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import za.co.giantautoupholstery.domain.Service
import za.co.giantautoupholstery.domain.ServiceRepository
import za.co.giantautoupholstery.ui.Load
import javax.inject.Inject

sealed interface ServicesUi {
    data object Loading : ServicesUi
    data class Content(val items: List<Service>) : ServicesUi
    data class Empty(val catalogueEmpty: Boolean) : ServicesUi
    data object Error : ServicesUi
}

/** Pure filter used by the ViewModel and unit tests. */
fun filterServices(all: List<Service>, query: String, category: String): List<Service> = all.filter {
    (category == "All" || it.category == category) &&
        (query.isBlank() || it.name.contains(query.trim(), true) || it.description.contains(query.trim(), true))
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ServicesViewModel @Inject constructor(repo: ServiceRepository, savedState: SavedStateHandle) : ViewModel() {
    val query = MutableStateFlow(savedState.get<String>("query").orEmpty())
    val category = MutableStateFlow("All")
    private val retry = MutableStateFlow(0)

    private val source = retry.flatMapLatest {
        repo.observeAll().map { Result.success(it) }.catch { e -> emit(Result.failure(e)) }
    }

    val ui: StateFlow<ServicesUi> = combine(source, query, category) { res, q, c ->
        res.fold(
            onSuccess = { all ->
                val f = filterServices(all, q, c)
                when {
                    all.isEmpty() -> ServicesUi.Empty(true)
                    f.isEmpty() -> ServicesUi.Empty(false)
                    else -> ServicesUi.Content(f)
                }
            },
            onFailure = { ServicesUi.Error },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServicesUi.Loading)

    fun retry() { retry.value++ }
}

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(repo: ServiceRepository, savedState: SavedStateHandle) : ViewModel() {
    val id: String = savedState.get<String>("id").orEmpty()
    val state: StateFlow<Load<Service>> = repo.observe(id)
        .map<Service?, Load<Service>> { if (it == null) Load.Missing else Load.Data(it) }
        .catch { emit(Load.Failed("Could not load this service.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Load.Loading)
}
