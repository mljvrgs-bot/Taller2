package com.caos.dominocaos.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    fun currentUser(): FirebaseUser? = auth.currentUser

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

    fun signOut() = auth.signOut()
}
