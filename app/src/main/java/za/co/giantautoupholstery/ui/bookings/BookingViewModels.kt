package za.co.giantautoupholstery.ui.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.core.BookingRules
import za.co.giantautoupholstery.core.ValidationException
import za.co.giantautoupholstery.core.Validation
import za.co.giantautoupholstery.core.today
import za.co.giantautoupholstery.core.userMessage
import za.co.giantautoupholstery.domain.*
import za.co.giantautoupholstery.ui.Load
import za.co.giantautoupholstery.ui.currentUid
import za.co.giantautoupholstery.ui.uidFlow
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class BookingsUi(val loading: Boolean = true, val error: Boolean = false, val upcoming: List<Booking> = emptyList(), val past: List<Booking> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingsViewModel @Inject constructor(auth: AuthRepository, repo: BookingRepository, private val clock: Clock) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val ui: StateFlow<BookingsUi> = combine(auth.uidFlow(), retry) { uid, _ -> uid }
        .flatMapLatest { uid ->
            if (uid == null) flowOf(BookingsUi(loading = false))
            else repo.observeAll(uid).map { list ->
                val (up, past) = BookingRules.partition(list, clock.today())
                BookingsUi(false, false, up, past)
            }.catch { emit(BookingsUi(loading = false, error = true)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookingsUi())
    fun retry() { retry.value++ }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val auth: AuthRepository, private val repo: BookingRepository, savedState: SavedStateHandle,
) : ViewModel() {
    private val id: String = savedState.get<String>("id").orEmpty()
    val state: StateFlow<Load<Booking>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf<Load<Booking>>(Load.Missing)
        else repo.observe(uid, id).map<Booking?, Load<Booking>> { if (it == null) Load.Missing else Load.Data(it) }
    }.catch { emit(Load.Failed("Could not load this booking.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Load.Loading)

    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message = _message.asStateFlow()

    fun cancel() {
        val uid = auth.currentUid() ?: return
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            repo.cancel(uid, id).fold(
                onSuccess = { _message.value = "Booking cancelled. A notification was added to your inbox." },
                onFailure = { _message.value = it.userMessage() },
            )
            _busy.value = false
        }
    }
}

data class BookingFormState(
    val serviceId: String? = null, val date: LocalDate? = null, val time: String? = null,
    val address: String = "", val description: String = "", val terms: Boolean = false,
    val errors: Map<String, String> = emptyMap(), val submitting: Boolean = false,
    val error: String? = null, val created: Booking? = null,
) {
    fun toInput() = BookingInput(serviceId, date, time, address, description, terms)
}

@HiltViewModel
class BookingFormViewModel @Inject constructor(
    private val auth: AuthRepository, services: ServiceRepository, private val repo: BookingRepository,
    private val profiles: ProfileRepository, private val clock: Clock, savedState: SavedStateHandle,
) : ViewModel() {
    val services: StateFlow<List<Service>> = services.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val earliestDate: LocalDate get() = clock.today().plusDays(1)

    private val _form = MutableStateFlow(BookingFormState(serviceId = savedState.get<String>("serviceId")))
    val form: StateFlow<BookingFormState> = _form.asStateFlow()

    /** One id per form instance: a retry of the same submission cannot create a duplicate booking or notification. */
    private val submissionId = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            val uid = auth.currentUid() ?: return@launch
            profiles.observe(uid).filterNotNull().first().address?.let { addr ->
                if (_form.value.address.isBlank()) _form.update { it.copy(address = addr) }
            }
        }
    }

    fun edit(f: BookingFormState.() -> BookingFormState) = _form.update { it.f().copy(error = null) }

    fun submit() {
        val s = _form.value
        if (s.submitting || s.created != null) return
        val errors = Validation.booking(s.toInput(), clock)
        if (errors.isNotEmpty()) { _form.update { it.copy(errors = errors) }; return }
        val uid = auth.currentUid()
        if (uid == null) { _form.update { it.copy(error = "Your session has ended. Please sign in again.") }; return }
        _form.update { it.copy(submitting = true, errors = emptyMap(), error = null) }
        viewModelScope.launch {
            repo.create(uid, s.toInput(), submissionId).fold(
                onSuccess = { b -> _form.update { it.copy(submitting = false, created = b) } }, // success only after commit
                onFailure = { e ->
                    _form.update {
                        if (e is ValidationException) it.copy(submitting = false, errors = e.errors)
                        else it.copy(submitting = false, error = e.userMessage())
                    }
                },
            )
        }
    }
}
