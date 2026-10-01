package com.example.donttrustthehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.donttrustthehouse.ui.GameViewModel
import com.example.donttrustthehouse.ui.MainGameScreen
import com.example.donttrustthehouse.ui.theme.DontTrustTheHouseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels {
        GameViewModel.provideFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.initAudio(this)

        setContent {
            DontTrustTheHouseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF08060A)
                ) {
                    MainGameScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseAudio()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }
}
