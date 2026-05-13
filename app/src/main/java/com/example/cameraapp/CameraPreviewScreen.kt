package com.example.cameraapp

import android.content.Intent
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.cameraapp.ui.theme.AccentGold
import com.example.cameraapp.ui.theme.AccentRed
import com.example.cameraapp.ui.theme.BlackAlpha60
import com.example.cameraapp.ui.theme.DarkBackground
import com.example.cameraapp.ui.theme.WhiteAlpha20
import com.example.cameraapp.ui.theme.WhiteAlpha60
import com.example.cameraapp.ui.theme.WhiteAlpha90
import kotlin.math.max
import kotlin.math.min

@Composable
fun CameraPreviewScreen(viewModel: CameraViewModel) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState        by viewModel.uiState.collectAsState()
    val previewView    = remember { PreviewView(context) }
    val cameraState    = remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(uiState.lensFacing, uiState.cameraMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview: Preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(uiState.lensFacing)
                .build()
            cameraProvider.unbindAll()
            try {
                cameraState.value = if (uiState.cameraMode == CameraMode.PHOTO) {
                    val imageCapture = ImageCapture.Builder().build()
                    viewModel.imageCapture = imageCapture
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                } else {
                    val videoCapture = viewModel.buildVideoCapture()
                    viewModel.videoCapture = videoCapture
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(uiState.zoomRatio) {
        cameraState.value?.cameraControl?.setZoomRatio(uiState.zoomRatio)
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {

        AndroidView(
            factory  = { previewView },
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    viewModel.setZoom(min(max(uiState.zoomRatio * zoom, 1f), 8f))
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(BlackAlpha60, Color.Transparent)))
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground)))
        )

        TopBar(
            uiState   = uiState,
            onFlash   = { viewModel.cycleFlash() },
            onFlipCam = { viewModel.toggleLens() },
            modifier  = Modifier.align(Alignment.TopCenter).statusBarsPadding()
        )

        AnimatedVisibility(
            visible  = uiState.isRecording,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp)
        ) {
            RecordingTimer(seconds = uiState.recordingDurationSeconds)
        }

        BottomControls(
            uiState   = uiState,
            onPhoto   = { viewModel.setCameraMode(CameraMode.PHOTO) },
            onVideo   = { viewModel.setCameraMode(CameraMode.VIDEO) },
            onCapture = {
                when (uiState.cameraMode) {
                    CameraMode.PHOTO -> viewModel.capturePhoto(context)
                    CameraMode.VIDEO -> if (uiState.isRecording) viewModel.stopRecording() else viewModel.startRecording(context)
                }
            },
            onGallery = {
                uiState.lastCapturedUri?.let { uri ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                }
            },
            modifier  = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )

        if (uiState.zoomRatio > 1.05f) {
            ZoomLabel(ratio = uiState.zoomRatio, modifier = Modifier.align(Alignment.Center).offset(y = 80.dp))
        }
    }
}

@Composable
private fun TopBar(uiState: CameraUiState, onFlash: () -> Unit, onFlipCam: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = "UB Camera", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.5.sp)
        Row {
            IconButton(onClick = onFlash) {
                Icon(
                    imageVector = when (uiState.flashMode) {
                        FlashMode.OFF  -> Icons.Filled.FlashOff
                        FlashMode.ON   -> Icons.Filled.FlashOn
                        FlashMode.AUTO -> Icons.Filled.FlashAuto
                    },
                    contentDescription = "Flash",
                    tint = if (uiState.flashMode == FlashMode.OFF) WhiteAlpha60 else AccentGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onFlipCam) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = "Flip", tint = WhiteAlpha90, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun RecordingTimer(seconds: Int) {
    val minutes = seconds / 60
    val secs    = seconds % 60
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink_alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(BlackAlpha60).padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentRed.copy(alpha = alpha)))
        Spacer(Modifier.width(8.dp))
        Text(text = "%02d:%02d".format(minutes, secs), color = WhiteAlpha90, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun BottomControls(
    uiState: CameraUiState, onPhoto: () -> Unit, onVideo: () -> Unit,
    onCapture: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ModeSelector(uiState.cameraMode, onPhoto, onVideo)
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GalleryThumbnail(uri = uiState.lastCapturedUri, onClick = onGallery, modifier = Modifier.size(56.dp))
            ShutterButton(mode = uiState.cameraMode, isRecording = uiState.isRecording, onClick = onCapture)
            Spacer(Modifier.size(56.dp))
        }
    }
}

@Composable
private fun ModeSelector(selectedMode: CameraMode, onPhoto: () -> Unit, onVideo: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(30.dp)).background(WhiteAlpha20).padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        ModeTab("PHOTO", selectedMode == CameraMode.PHOTO, onPhoto)
        Spacer(Modifier.width(4.dp))
        ModeTab("VIDEO", selectedMode == CameraMode.VIDEO, onVideo)
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (selected) AccentGold else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, fontSize = 13.sp, letterSpacing = 1.sp,
            color = if (selected) Color.Black else WhiteAlpha60,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ShutterButton(mode: CameraMode, isRecording: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isRecording) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp).scale(scale).clip(CircleShape)
            .border(BorderStroke(4.dp, if (mode == CameraMode.VIDEO && isRecording) AccentRed else AccentGold), CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (mode == CameraMode.VIDEO && isRecording) {
            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = AccentRed, modifier = Modifier.size(36.dp))
        } else {
            Box(
                modifier = Modifier.size(60.dp)
                    .clip(if (mode == CameraMode.VIDEO) RoundedCornerShape(8.dp) else CircleShape)
                    .background(if (mode == CameraMode.VIDEO) AccentRed else Color.White)
            )
        }
    }
}

@Composable
private fun GalleryThumbnail(uri: Uri?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(2.dp, WhiteAlpha60), RoundedCornerShape(10.dp))
            .background(WhiteAlpha20)
            .clickable(enabled = uri != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = "Last captured", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = WhiteAlpha60, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ZoomLabel(ratio: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(BlackAlpha60).padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(text = "%.1fx".format(ratio), color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

