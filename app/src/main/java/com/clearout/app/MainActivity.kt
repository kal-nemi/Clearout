package com.clearout.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
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
        super.onCreate(savedInstanceState)
        
        // Fully enable modern edge-to-edge display visuals
        enableEdgeToEdge()

        setContent {
            ClearOutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Navigation Host handles sub-paddings internally for beautiful immersion
                    AppNavigation(dataStore = dataStore)
                }
            }
        }
    }
}
