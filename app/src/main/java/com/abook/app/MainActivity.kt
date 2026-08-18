package com.abook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.abook.app.navigation.ABookNavHost
import com.abook.app.ui.theme.ABookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ABookTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ABookNavHost()
                }
            }
        }
    }
}
