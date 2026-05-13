package com.example.cameraapp

import android.Manifest
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cameraapp.ui.theme.AccentGold
import com.example.cameraapp.ui.theme.DarkBackground
import com.example.cameraapp.ui.theme.DarkCard
import com.example.cameraapp.ui.theme.WhiteAlpha60
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    val permissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    )

    if (permissions.allPermissionsGranted) {
        content()
    } else {
        PermissionsRationale(onRequest = { permissions.launchMultiplePermissionRequest() })
    }
}

@Composable
private fun PermissionsRationale(onRequest: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkBackground, DarkCard)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center,
            modifier              = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(AccentGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint               = AccentGold,
                    modifier           = Modifier.size(54.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text       = "UB Camera",
                color      = AccentGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 30.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "To capture stunning photos and videos,\nplease grant camera & microphone access.",
                color     = WhiteAlpha60,
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onRequest,
                shape   = RoundedCornerShape(30.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = AccentGold),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text       = "Grant Permissions",
                    color      = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
