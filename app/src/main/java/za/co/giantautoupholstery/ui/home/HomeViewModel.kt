package za.co.giantautoupholstery.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import za.co.giantautoupholstery.domain.*
import za.co.giantautoupholstery.ui.uidFlow
import javax.inject.Inject

data class HomeState(val name: String? = null, val latest: Booking? = null, val featured: List<Service> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    auth: AuthRepository, profiles: ProfileRepository, bookings: BookingRepository, services: ServiceRepository,
) : ViewModel() {
    private val uid = auth.uidFlow()
    val state: StateFlow<HomeState> = combine(
        uid.flatMapLatest { if (it == null) flowOf(null) else profiles.observe(it) },
        uid.flatMapLatest { if (it == null) flowOf(emptyList()) else bookings.observeAll(it) },
        services.observeAll(),
    ) { profile, list, all ->
        HomeState(profile?.fullName, list.maxByOrNull { it.createdAt }, all.take(3))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}
