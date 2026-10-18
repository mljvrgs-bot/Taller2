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
import com.caos.dominocaos.ui.screens.room.RoomScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOBBY = "lobby"
    const val ROOM = "room/{code}"
    const val GAME = "game/{code}"
    const val GAME_OFFLINE = "game_offline"
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
                onSuccess = { nav.navigate(Routes.LOBBY) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onGoRegister = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onSuccess = { nav.navigate(Routes.LOBBY) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onGoLogin = { nav.popBackStack() }
            )
        }
        composable(Routes.LOBBY) {
            LobbyScreen(
                onRoom = { code -> nav.navigate("room/$code") },
                onPlayOffline = { nav.navigate(Routes.GAME_OFFLINE) },
                onSignOut = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.LOBBY) { inclusive = true } } }
            )
        }
        composable(Routes.ROOM) { entry ->
            val code = entry.arguments?.getString("code") ?: return@composable
            RoomScreen(
                code = code,
                // Al iniciar, la sala se saca del historial para que "atras" no vuelva a ella.
                onStarted = { nav.navigate("game/$code") { popUpTo(Routes.ROOM) { inclusive = true } } },
                onLeave = { nav.popBackStack() }
            )
        }
        composable(Routes.GAME) { entry ->
            val code = entry.arguments?.getString("code") ?: return@composable
            GameScreen(gameCode = code, onLeave = { nav.popBackStack(Routes.LOBBY, false) })
        }
        composable(Routes.GAME_OFFLINE) {
            GameScreen(gameCode = null, onLeave = { nav.popBackStack(Routes.LOBBY, false) })
        }
    }
}