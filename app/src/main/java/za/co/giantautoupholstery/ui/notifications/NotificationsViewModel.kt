package za.co.giantautoupholstery.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.core.userMessage
import za.co.giantautoupholstery.domain.AppNotification
import za.co.giantautoupholstery.domain.AuthRepository
import za.co.giantautoupholstery.domain.NotificationRepository
import za.co.giantautoupholstery.ui.currentUid
import za.co.giantautoupholstery.ui.uidFlow
import javax.inject.Inject

data class NotificationsUi(val loading: Boolean = true, val error: Boolean = false, val items: List<AppNotification> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(private val auth: AuthRepository, private val repo: NotificationRepository) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val ui: StateFlow<NotificationsUi> = combine(auth.uidFlow(), retry) { uid, _ -> uid }.flatMapLatest { uid ->
        if (uid == null) flowOf(NotificationsUi(false))
        else repo.observeAll(uid).map { NotificationsUi(false, false, it) }.catch { emit(NotificationsUi(false, true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUi())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun consumeMessage() { _message.value = null }
    fun retry() { retry.value++ }

    private fun act(block: suspend (String) -> Result<Unit>) {
        val uid = auth.currentUid() ?: return
        viewModelScope.launch { block(uid).onFailure { _message.value = it.userMessage() } }
    }
    fun markRead(id: String) = act { repo.markRead(it, id) }
    fun markAllRead() = act { repo.markAllRead(it) }
    fun delete(id: String) = act { repo.delete(it, id) }
}
