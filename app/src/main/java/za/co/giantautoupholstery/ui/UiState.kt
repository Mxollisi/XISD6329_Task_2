package za.co.giantautoupholstery.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import za.co.giantautoupholstery.domain.AuthRepository
import za.co.giantautoupholstery.domain.AuthState

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Data<T>(val value: T) : Load<T>
    data object Missing : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
}

/** The verified Firebase UID of the current session, or null. Private ViewModel flows key off this so account switches are isolated. */
fun AuthRepository.uidFlow(): Flow<String?> = state.map { (it as? AuthState.SignedIn)?.user?.uid }.distinctUntilChanged()
fun AuthRepository.currentUid(): String? = (state.value as? AuthState.SignedIn)?.user?.uid
