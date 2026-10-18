package com.caos.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import com.caos.dominocaos.data.FirebaseAuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de las pantallas de acceso: cargando y mensaje de error. */
data class AuthUiState(val loading: Boolean = false, val error: String? = null)

/** Puente entre las pantallas de login/registro y FirebaseAuthRepository. */
class AuthViewModel : ViewModel() {

    private val repo = FirebaseAuthRepository()

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    /** Nombre para mostrar: la parte del correo antes de la arroba. */
    fun userLabel(): String = repo.currentUser()?.email?.substringBefore('@') ?: "jugador"

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        _ui.value = AuthUiState(loading = true)
        repo.signIn(email, password) { result -> handle(result, onSuccess) }
    }

    fun register(email: String, password: String, confirm: String, onSuccess: () -> Unit) {
        // Validaciones locales antes de llamar a Firebase.
        if (password.length < 6) {
            _ui.value = AuthUiState(error = "La contrasena debe tener al menos 6 caracteres.")
            return
        }
        if (password != confirm) {
            _ui.value = AuthUiState(error = "Las contrasenas no coinciden.")
            return
        }
        _ui.value = AuthUiState(loading = true)
        repo.register(email, password) { result -> handle(result, onSuccess) }
    }

    fun signOut() = repo.signOut()

    private fun handle(result: Result<FirebaseUser>, onSuccess: () -> Unit) {
        result
            .onSuccess {
                _ui.value = AuthUiState()
                onSuccess()
            }
            .onFailure { _ui.value = AuthUiState(error = friendlyMessage(it)) }
    }

    /** Traduce las excepciones de Firebase a mensajes entendibles. El orden importa:
     *  WeakPassword hereda de InvalidCredentials, por eso va primero. */
    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contrasena es muy debil."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        is FirebaseAuthInvalidCredentialsException -> "Correo o contrasena incorrectos."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseNetworkException -> "Sin conexion. Revisa tu internet."
        is IllegalArgumentException -> e.message ?: "Datos invalidos."
        else -> e.message ?: "No se pudo completar la operacion."
    }
}