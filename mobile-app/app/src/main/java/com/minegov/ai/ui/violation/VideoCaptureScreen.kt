package com.minegov.ai.ui.violation

import android.content.ContentValues
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView

@Composable
fun VideoCaptureScreen(
    onBack: () -> Unit,
    onVideoCaptured: (String) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var videoCapture by remember {
        mutableStateOf<VideoCapture<Recorder>?>(null)
    }

    var recording by remember {
        mutableStateOf<Recording?>(null)
    }

    var capturedVideoUri by remember {
        mutableStateOf<String?>(null)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // =================================================
    // VIDEO PREVIEW
    // =================================================

    if (capturedVideoUri != null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(20.dp)
        ) {

            Text(
                text = "VIDEO PREVIEW",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Review the video evidence before continuing.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            AndroidView(
                factory = { ctx ->

                    VideoView(ctx).apply {

                        setVideoURI(
                            Uri.parse(capturedVideoUri)
                        )

                        setMediaController(
                            MediaController(ctx)
                        )

                        requestFocus()

                        setOnPreparedListener {
                            start()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        capturedVideoUri = null
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF202020),
                        contentColor = Color.White
                    )
                ) {
                    Text("RETAKE")
                }

                Button(
                    onClick = {

                        capturedVideoUri?.let { uri ->
                            onVideoCaptured(uri)
                        }

                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "USE VIDEO",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        return
    }

    // =================================================
    // LIVE VIDEO CAMERA
    // =================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            factory = { ctx ->

                val previewView =
                    PreviewView(ctx)

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({

                    val cameraProvider =
                        cameraProviderFuture.get()

                    val preview =
                        Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider =
                                    previewView.surfaceProvider
                            }

                    val recorder =
                        Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(
                                    Quality.HD
                                )
                            )
                            .build()

                    val videoCaptureInstance =
                        VideoCapture.withOutput(
                            recorder
                        )

                    videoCapture =
                        videoCaptureInstance

                    val cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA

                    try {

                        cameraProvider.unbindAll()

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            videoCaptureInstance
                        )

                    } catch (exception: Exception) {

                        errorMessage =
                            "Unable to start video camera."
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // =================================================
        // CAMERA CONTROLS
        // =================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = if (isRecording) {
                    "● RECORDING"
                } else {
                    "VIDEO EVIDENCE"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.65f)
                    )
                    .padding(12.dp)
            )

            Column {

                errorMessage?.let {

                    Text(
                        text = it,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f)
                            )
                            .padding(12.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = {

                            if (isRecording) {
                                recording?.stop()
                                recording = null
                                isRecording = false
                            } else {
                                onBack()
                            }

                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isRecording) {
                                "STOP"
                            } else {
                                "BACK"
                            }
                        )
                    }

                    Button(
                        onClick = {

                            val capture =
                                videoCapture
                                    ?: return@Button

                            if (isRecording) {

                                recording?.stop()
                                recording = null
                                isRecording = false

                                return@Button
                            }

                            val fileName =
                                "MINEGOV_VIDEO_" +
                                        System.currentTimeMillis() +
                                        ".mp4"

                            val contentValues =
                                ContentValues().apply {

                                    put(
                                        MediaStore.Video.Media.DISPLAY_NAME,
                                        fileName
                                    )

                                    put(
                                        MediaStore.Video.Media.MIME_TYPE,
                                        "video/mp4"
                                    )
                                }

                            val mediaStoreOutput =
                                MediaStoreOutputOptions
                                    .Builder(
                                        context.contentResolver,
                                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                    )
                                    .setContentValues(
                                        contentValues
                                    )
                                    .build()

                            val pendingRecording =
                                capture.output
                                    .prepareRecording(
                                        context,
                                        mediaStoreOutput
                                    )

                            recording =
                                pendingRecording.start(
                                    ContextCompat.getMainExecutor(
                                        context
                                    )
                                ) { event ->

                                    when (event) {

                                        is VideoRecordEvent.Start -> {

                                            isRecording = true
                                            errorMessage = null
                                        }

                                        is VideoRecordEvent.Finalize -> {

                                            isRecording = false
                                            recording = null

                                            if (
                                                !event.hasError()
                                            ) {

                                                val uri =
                                                    event.outputResults
                                                        .outputUri

                                                if (uri != Uri.EMPTY) {

                                                    capturedVideoUri =
                                                        uri.toString()
                                                }

                                            } else {

                                                errorMessage =
                                                    "Video recording failed."
                                            }
                                        }
                                    }
                                }

                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (isRecording) {
                                    Color(0xFF202020)
                                } else {
                                    Color.White
                                },
                            contentColor =
                                if (isRecording) {
                                    Color.White
                                } else {
                                    Color.Black
                                }
                        )
                    ) {

                        Text(
                            text = if (isRecording) {
                                "STOP RECORDING"
                            } else {
                                "START RECORDING"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}