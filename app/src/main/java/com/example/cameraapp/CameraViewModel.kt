package com.example.cameraapp

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CameraMode { PHOTO, VIDEO }
enum class FlashMode { OFF, ON, AUTO }

data class CameraUiState(
    val cameraMode: CameraMode       = CameraMode.PHOTO,
    val lensFacing: Int               = CameraSelector.LENS_FACING_BACK,
    val flashMode: FlashMode          = FlashMode.OFF,
    val isRecording: Boolean          = false,
    val recordingDurationSeconds: Int = 0,
    val lastCapturedUri: Uri?         = null,
    val zoomRatio: Float              = 1f,
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    var imageCapture: ImageCapture? = null
    var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    // ──────────────────────────────────────────
    // UI actions
    // ──────────────────────────────────────────

    fun setCameraMode(mode: CameraMode) {
        _uiState.update { it.copy(cameraMode = mode) }
    }

    fun toggleLens() {
        _uiState.update {
            it.copy(
                lensFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT
                else
                    CameraSelector.LENS_FACING_BACK
            )
        }
    }

    fun cycleFlash() {
        _uiState.update {
            val next = when (it.flashMode) {
                FlashMode.OFF  -> FlashMode.ON
                FlashMode.ON   -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.OFF
            }
            it.copy(flashMode = next)
        }
    }

    fun setZoom(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio) }
    }

    // ──────────────────────────────────────────
    // Photo capture
    // ──────────────────────────────────────────

    fun capturePhoto(context: Context) {
        val capture = imageCapture ?: return

        capture.flashMode = when (_uiState.value.flashMode) {
            FlashMode.OFF  -> ImageCapture.FLASH_MODE_OFF
            FlashMode.ON   -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "UB_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/UB Camera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri
                    _uiState.update { it.copy(lastCapturedUri = uri) }
                    Toast.makeText(context, "Photo saved!", Toast.LENGTH_SHORT).show()
                    Log.d("UBCamera", "Photo saved: $uri")
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(context, "Failed to save photo.", Toast.LENGTH_SHORT).show()
                    Log.e("UBCamera", "Photo capture failed", exc)
                }
            }
        )
    }

    // ──────────────────────────────────────────
    // Video recording
    // ──────────────────────────────────────────

    fun startRecording(context: Context) {
        val capture = videoCapture ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "UB_VID_${System.currentTimeMillis()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/UB Camera")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        activeRecording = capture.output
            .prepareRecording(context, outputOptions)
            .apply {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start  -> {
                        _uiState.update { it.copy(isRecording = true, recordingDurationSeconds = 0) }
                    }
                    is VideoRecordEvent.Status -> {
                        _uiState.update {
                            it.copy(recordingDurationSeconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt())
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        _uiState.update { it.copy(isRecording = false, recordingDurationSeconds = 0) }
                        if (!event.hasError()) {
                            val uri = event.outputResults.outputUri
                            _uiState.update { it.copy(lastCapturedUri = uri) }
                            Toast.makeText(context, "Video saved!", Toast.LENGTH_SHORT).show()
                            Log.d("UBCamera", "Video saved: $uri")
                        } else {
                            Toast.makeText(context, "Video recording failed.", Toast.LENGTH_SHORT).show()
                            Log.e("UBCamera", "Video error: ${event.error}")
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    fun buildVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        return VideoCapture.withOutput(recorder)
    }

    override fun onCleared() {
        super.onCleared()
        activeRecording?.stop()
    }
}

