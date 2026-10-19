package za.co.giantautoupholstery.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.core.Validation
import za.co.giantautoupholstery.core.ValidationException
import za.co.giantautoupholstery.core.userMessage
import za.co.giantautoupholstery.domain.*
import za.co.giantautoupholstery.ui.Load
import za.co.giantautoupholstery.ui.currentUid
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(repo: SupportRepository) : ViewModel() {
    val faqs: StateFlow<Load<List<Faq>>> = repo.observeFaqs()
        .map<List<Faq>, Load<List<Faq>>> { Load.Data(it) }
        .catch { emit(Load.Failed("Could not load help.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Load.Loading)
}

data class ContactState(
    val name: String = "", val email: String = "", val subject: String = "", val message: String = "",
    val errors: Map<String, String> = emptyMap(), val submitting: Boolean = false, val saved: Boolean = false, val error: String? = null,
)

@HiltViewModel
class ContactViewModel @Inject constructor(private val auth: AuthRepository, private val repo: SupportRepository) : ViewModel() {
    private val _state = MutableStateFlow(
        (auth.state.value as? AuthState.SignedIn)?.user?.let { ContactState(name = it.displayName.orEmpty(), email = it.email) } ?: ContactState()
    )
    val state: StateFlow<ContactState> = _state.asStateFlow()

    fun edit(f: ContactState.() -> ContactState) = _state.update { it.f().copy(saved = false, error = null) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        val input = ContactInput(s.name, s.email, s.subject, s.message)
        val errors = buildMap {
            Validation.contactName(s.name)?.let { put("name", it) }
            Validation.email(s.email)?.let { put("email", it) }
            Validation.contactSubject(s.subject)?.let { put("subject", it) }
            Validation.contactMessage(s.message)?.let { put("message", it) }
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        _state.update { it.copy(submitting = true, errors = emptyMap(), error = null) }
        viewModelScope.launch {
            repo.saveContact(auth.currentUid(), input).fold(
                onSuccess = { _state.update { it.copy(submitting = false, saved = true, subject = "", message = "") } },
                onFailure = { e ->
                    _state.update { if (e is ValidationException) it.copy(submitting = false, errors = e.errors) else it.copy(submitting = false, error = e.userMessage()) }
                },
            )
        }
    }
}
