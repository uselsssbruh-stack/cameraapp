package com.example.cameraapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.cameraapp.ui.theme.CameraAppTheme

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraAppTheme {
                PermissionsGate {
                    CameraPreviewScreen(viewModel = cameraViewModel)
                }
            }
        }
    }
}
