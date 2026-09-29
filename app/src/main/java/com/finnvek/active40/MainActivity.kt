package com.finnvek.active40

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finnvek.active40.ui.navigation.Active40Root
import com.finnvek.active40.ui.theme.Active40Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Tausta on aina tumman vihreä, joten järjestelmäpalkkien ikonit ovat vaaleat molemmissa teemoissa.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            Active40Theme {
                Active40Root()
            }
        }
    }

    // Palautusajastin värisee näkyvissä ja ilmoittaa taustalla.
    override fun onStart() {
        super.onStart()
        (application as Active40Application).container.restTimer.isAppInForeground = true
    }

    override fun onStop() {
        (application as Active40Application).container.restTimer.isAppInForeground = false
        super.onStop()
    }
}
