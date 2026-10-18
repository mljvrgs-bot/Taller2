package com.caos.dominocaos

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.caos.dominocaos.ui.theme.DominoCaosTheme
import com.google.firebase.FirebaseApp
import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        val firebaseStatus = try {
            val app = FirebaseApp.initializeApp(this)

            if (app != null) {
                "Firebase se inicializó correctamente."
            } else {
                "No se encontró la configuración de Firebase."
            }
        } catch (e: Exception) {
            "Error al iniciar Firebase: ${e.message}"
        }

        setContent {
            DominoCaosTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    FirebaseTestScreen(
                        status = firebaseStatus,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FirebaseTestScreen(
    status: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Domino Caos")
        Text(
            text = status,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}