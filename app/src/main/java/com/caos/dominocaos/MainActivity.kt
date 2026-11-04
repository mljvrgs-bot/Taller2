package com.caos.dominocaos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.caos.dominocaos.ui.navigation.AppNavigation
import com.caos.dominocaos.ui.theme.DominoCaosTheme
import android.graphics.Color as AndroidColor

/** Unica actividad de la app: aloja el tema y el grafo de navegacion. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app es siempre oscura: iconos claros en las barras del sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            DominoCaosTheme {
                AppNavigation()
            }
        }
    }
}