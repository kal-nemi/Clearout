package com.clearout.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clearout.app.data.datastore.GamificationDataStore
import com.clearout.app.ui.navigation.AppNavigation
import com.clearout.app.ui.theme.ClearOutTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: GamificationDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() MUST be called before super.onCreate()
        // This hooks into the Android 12+ SplashScreen API and applies
        // the windowSplashScreen* theme attributes from themes.xml
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Fully enable modern edge-to-edge display visuals
        enableEdgeToEdge()

        setContent {
            ClearOutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    // AppNavigation starts with the animated Compose SplashScreen
                    // before transitioning to Onboarding or Home
                    AppNavigation(dataStore = dataStore)
                }
            }
        }
    }
}
