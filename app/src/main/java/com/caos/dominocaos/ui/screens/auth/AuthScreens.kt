package com.caos.dominocaos.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caos.dominocaos.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthLayout(title = "Domino Caos", subtitle = "Inicia sesion para jugar", error = state.error) {
        AuthField(email, { email = it }, "Correo", KeyboardType.Email)
        AuthField(password, { password = it }, "Contrasena", KeyboardType.Password, isPassword = true)
        Button(
            onClick = { viewModel.signIn(email, password, onSuccess) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.loading) "Cargando..." else "Entrar") }
        TextButton(onClick = onGoRegister) { Text("No tengo cuenta") }
    }
}

@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    AuthLayout(title = "Crear cuenta", subtitle = "Registrate para jugar", error = state.error) {
        AuthField(email, { email = it }, "Correo", KeyboardType.Email)
        AuthField(password, { password = it }, "Contrasena", KeyboardType.Password, isPassword = true)
        AuthField(confirm, { confirm = it }, "Confirmar contrasena", KeyboardType.Password, isPassword = true)
        Button(
            onClick = { viewModel.register(email, password, confirm, onSuccess) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.loading) "Cargando..." else "Registrarme") }
        TextButton(onClick = onGoLogin) { Text("Ya tengo cuenta") }
    }
}

/** Estructura comun: titulo, subtitulo, contenido y mensaje de error. */
@Composable
private fun AuthLayout(
    title: String,
    subtitle: String,
    error: String?,
    content: @Composable () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}