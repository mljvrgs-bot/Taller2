package com.caos.dominocaos.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Acceso a Firebase Authentication (correo y contrasena).
 * Cada operacion es asincrona y devuelve el resultado por callback como Result.
 */
class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    /** Usuario con sesion iniciada, o null si no hay. */
    fun currentUser(): FirebaseUser? = auth.currentUser

    /** Crea una cuenta nueva e inicia sesion. */
    fun register(email: String, password: String, onResult: (Result<FirebaseUser>) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Ingresa el correo y la contraseña.")))
            return
        }
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) onResult(Result.success(user))
                else onResult(Result.failure(IllegalStateException("Firebase no devolvió el usuario.")))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Inicia sesion con una cuenta existente. */
    fun signIn(email: String, password: String, onResult: (Result<FirebaseUser>) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Ingresa el correo y la contraseña.")))
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) onResult(Result.success(user))
                else onResult(Result.failure(IllegalStateException("Firebase no devolvió el usuario.")))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Cierra la sesion actual. */
    fun signOut() = auth.signOut()
}
