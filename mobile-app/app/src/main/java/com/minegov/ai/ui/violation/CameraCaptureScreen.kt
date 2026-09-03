package com.minegov.ai.ui.violation

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onPhotoCaptured: (String) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // -----------------------------------------
    // STATE
    // -----------------------------------------

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var imageCapture by remember {
        mutableStateOf<ImageCapture?>(null)
    }

    var capturedPhotoUri by remember {
        mutableStateOf<String?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isCapturing by remember {
        mutableStateOf(false)
    }

    // -----------------------------------------
    // CAMERA PERMISSION
    // -----------------------------------------

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCameraPermission = granted

            if (!granted) {
                errorMessage =
                    "Camera permission is required."
            }
        }

    // -----------------------------------------
    // REQUEST PERMISSION
    // -----------------------------------------

    if (!hasCameraPermission) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Camera Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "MineGov AI needs camera access to capture inspection evidence.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Button(
                onClick = {
                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "ALLOW CAMERA",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF202020),
                    contentColor = Color.White
                )
            ) {
                Text("BACK")
            }
        }

        return
    }

    // =================================================
    // PHOTO PREVIEW
    // =================================================

    if (capturedPhotoUri != null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(20.dp)
        ) {

            Text(
                text = "PHOTO PREVIEW",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Review the evidence before continuing.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // -----------------------------------------
            // IMAGE
            // -----------------------------------------

            AsyncImage(
                model = capturedPhotoUri,
                contentDescription = "Captured inspection evidence",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // -----------------------------------------
            // RETAKE / USE
            // -----------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {

                        capturedPhotoUri = null
                        errorMessage = null

                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF202020),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "RETAKE"
                    )
                }

                Button(
                    onClick = {

                        capturedPhotoUri?.let { uri ->

                            onPhotoCaptured(uri)
                        }

                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "USE PHOTO",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        return
    }

    // =================================================
    // LIVE CAMERA
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

                    val capture =
                        ImageCapture.Builder()
                            .setCaptureMode(
                                ImageCapture
                                    .CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            .build()

                    imageCapture = capture

                    val cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA

                    try {

                        cameraProvider.unbindAll()

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )

                    } catch (exception: Exception) {

                        errorMessage =
                            "Unable to start camera."
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // -----------------------------------------
        // CAMERA UI
        // -----------------------------------------

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "PHOTO EVIDENCE",
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

                    // BACK
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                Color.Black,
                            contentColor =
                                Color.White
                        )
                    ) {
                        Text("BACK")
                    }

                    // CAPTURE
                    Button(
                        onClick = {

                            val capture =
                                imageCapture
                                    ?: return@Button

                            if (isCapturing) {
                                return@Button
                            }

                            isCapturing = true
                            errorMessage = null

                            val fileName =
                                "MINEGOV_" +
                                        SimpleDateFormat(
                                            "yyyyMMdd_HHmmss",
                                            Locale.US
                                        ).format(Date()) +
                                        ".jpg"

                            val contentValues =
                                ContentValues().apply {

                                    put(
                                        MediaStore.Images.Media.DISPLAY_NAME,
                                        fileName
                                    )

                                    put(
                                        MediaStore.Images.Media.MIME_TYPE,
                                        "image/jpeg"
                                    )
                                }

                            val outputOptions =
                                ImageCapture
                                    .OutputFileOptions
                                    .Builder(
                                        context.contentResolver,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    )
                                    .build()

                            capture.takePicture(
                                outputOptions,
                                ContextCompat
                                    .getMainExecutor(context),

                                object :
                                    ImageCapture.OnImageSavedCallback {

                                    override fun onImageSaved(
                                        outputFileResults:
                                        ImageCapture
                                        .OutputFileResults
                                    ) {

                                        val uri =
                                            outputFileResults
                                                .savedUri

                                        if (uri != null) {

                                            capturedPhotoUri =
                                                uri.toString()
                                        } else {

                                            errorMessage =
                                                "Photo saved but URI was unavailable."
                                        }

                                        isCapturing = false
                                    }

                                    override fun onError(
                                        exception:
                                        ImageCaptureException
                                    ) {

                                        errorMessage =
                                            "Photo capture failed."

                                        isCapturing = false
                                    }
                                }
                            )
                        },
                        enabled = !isCapturing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                Color.White,
                            contentColor =
                                Color.Black,
                            disabledContainerColor =
                                Color(0xFF202020),
                            disabledContentColor =
                                Color(0xFF666666)
                        )
                    ) {
                        Text(
                            text = if (isCapturing) {
                                "SAVING..."
                            } else {
                                "CAPTURE"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}