package com.minegov.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.minegov.ai.ui.navigation.AppNavigation
import com.minegov.ai.ui.theme.MineGovAITheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MineGovAITheme {
                AppNavigation()
            }
        }
    }
}