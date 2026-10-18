package com.caos.dominocaos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.caos.dominocaos.data.FirebaseAuthRepository
import com.caos.dominocaos.ui.screens.auth.LoginScreen
import com.caos.dominocaos.ui.screens.auth.RegisterScreen
import com.caos.dominocaos.ui.screens.game.GameScreen
import com.caos.dominocaos.ui.screens.lobby.LobbyScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOBBY = "lobby"
    const val GAME = "game"
}

/** Grafo de navegacion. Si ya hay sesion iniciada, arranca en el lobby. */
@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val loggedIn = remember { FirebaseAuthRepository().currentUser() != null }

    NavHost(
        navController = nav,
        startDestination = if (loggedIn) Routes.LOBBY else Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSuccess = {
                    // Se limpia el historial para que "atras" no vuelva al login.
                    nav.navigate(Routes.LOBBY) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onGoRegister = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onSuccess = {
                    nav.navigate(Routes.LOBBY) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onGoLogin = { nav.popBackStack() }
            )
        }
        composable(Routes.LOBBY) {
            LobbyScreen(
                onPlayOffline = { nav.navigate(Routes.GAME) },
                onSignOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.LOBBY) { inclusive = true } }
                }
            )
        }
        composable(Routes.GAME) {
            GameScreen(onLeave = { nav.popBackStack() })
        }
    }
}