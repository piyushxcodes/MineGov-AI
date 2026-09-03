package com.minegov.ai.ui.violation

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VoiceNoteScreen(
    onBack: () -> Unit,
    onVoiceCaptured: (String) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // =================================================
    // STATE
    // =================================================

    var isRecording by remember {
        mutableStateOf(false)
    }

    var recordedFilePath by remember {
        mutableStateOf<String?>(null)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var recordingSeconds by remember {
        mutableStateOf(0)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // =================================================
    // MEDIA RECORDER
    // =================================================

    var mediaRecorder by remember {
        mutableStateOf<MediaRecorder?>(null)
    }

    var mediaPlayer by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    // =================================================
    // AUDIO PERMISSION
    // =================================================

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startRecording(
                    context = context,
                    onRecorderCreated = { recorder, path ->
                        mediaRecorder = recorder
                        recordedFilePath = path
                        isRecording = true
                        recordingSeconds = 0
                        errorMessage = null
                    },
                    onError = {
                        errorMessage = it
                    }
                )
            } else {
                errorMessage =
                    "Microphone permission is required."
            }
        }

    // =================================================
    // RECORDING TIMER
    // =================================================

    androidx.compose.runtime.LaunchedEffect(
        isRecording
    ) {

        if (isRecording) {

            recordingSeconds = 0

            while (isRecording) {

                delay(1000)

                recordingSeconds++
            }
        }
    }

    // =================================================
    // CLEANUP
    // =================================================

    DisposableEffect(Unit) {

        onDispose {

            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }

            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    // =================================================
    // START RECORDING
    // =================================================

    fun requestRecording() {

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            startRecording(
                context = context,
                onRecorderCreated = { recorder, path ->

                    mediaRecorder = recorder
                    recordedFilePath = path
                    isRecording = true
                    recordingSeconds = 0
                    errorMessage = null
                },
                onError = {
                    errorMessage = it
                }
            )

        } else {

            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    // =================================================
    // STOP RECORDING
    // =================================================

    fun stopRecording() {

        try {

            mediaRecorder?.stop()

        } catch (_: RuntimeException) {

            recordedFilePath = null
            errorMessage =
                "Recording was too short. Please try again."
        }

        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }

        mediaRecorder = null
        isRecording = false
    }

    // =================================================
    // PLAY RECORDING
    // =================================================

    fun playRecording() {

        val path = recordedFilePath ?: return

        try {

            mediaPlayer?.release()

            val player =
                MediaPlayer()

            player.setDataSource(path)

            player.setOnCompletionListener {

                isPlaying = false

                try {
                    it.release()
                } catch (_: Exception) {
                }

                mediaPlayer = null
            }

            player.prepare()
            player.start()

            mediaPlayer = player
            isPlaying = true

        } catch (e: Exception) {

            isPlaying = false

            errorMessage =
                "Unable to play recording."
        }
    }

    // =================================================
    // STOP PLAYBACK
    // =================================================

    fun stopPlayback() {

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }

        mediaPlayer = null
        isPlaying = false
    }

    // =================================================
    // RETAKE
    // =================================================

    fun retakeRecording() {

        stopPlayback()

        recordedFilePath?.let {

            try {
                File(it).delete()
            } catch (_: Exception) {
            }
        }

        recordedFilePath = null
        recordingSeconds = 0
        errorMessage = null
    }

    // =================================================
    // UI
    // =================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {

        // =================================================
        // HEADER
        // =================================================

        Text(
            text = "Voice Note",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Step 5 of 7",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(45.dp)
        )

        // =================================================
        // RECORDING AREA
        // =================================================

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            if (isRecording) {

                Text(
                    text = "● RECORDING",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = formatTime(recordingSeconds),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(25.dp)
                )

                Text(
                    text =
                        "Speak clearly. Tap stop when finished.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

            } else if (recordedFilePath != null) {

                Text(
                    text = "✓ VOICE RECORDED",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text =
                        "Your voice note is ready.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(25.dp)
                )

                // -----------------------------------------
                // PLAY / STOP
                // -----------------------------------------

                Button(
                    onClick = {

                        if (isPlaying) {
                            stopPlayback()
                        } else {
                            playRecording()
                        }

                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF202020),
                            contentColor =
                                Color.White
                        )
                ) {

                    Text(
                        text =
                            if (isPlaying) {
                                "STOP PLAYBACK"
                            } else {
                                "▶ PLAY RECORDING"
                            },
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

            } else {

                Text(
                    text = "VOICE NOTE",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                Text(
                    text =
                        "Record an audio note describing the violation.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // =================================================
        // ERROR
        // =================================================

        errorMessage?.let {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = it,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // =================================================
        // RECORD BUTTON
        // =================================================

        if (isRecording) {

            Button(
                onClick = {
                    stopRecording()
                },
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.White,
                        contentColor =
                            Color.Black
                    )
            ) {

                Text(
                    text = "STOP RECORDING",
                    fontWeight =
                        FontWeight.Bold
                )
            }

        } else if (recordedFilePath == null) {

            // ---------------------------------------------
            // START RECORDING
            // ---------------------------------------------

            Button(
                onClick = {
                    requestRecording()
                },
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.White,
                        contentColor =
                            Color.Black
                    )
            ) {

                Text(
                    text = "START RECORDING",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onBack,
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF202020),
                        contentColor =
                            Color.White
                    )
            ) {

                Text("BACK")
            }

        } else {

            // =================================================
            // AFTER RECORDING
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        retakeRecording()
                    },
                    modifier =
                        Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF202020),
                            contentColor =
                                Color.White
                        )
                ) {

                    Text("RETAKE")
                }

                Button(
                    onClick = {

                        val path =
                            recordedFilePath

                        if (path != null) {

                            onVoiceCaptured(path)
                        }

                    },
                    modifier =
                        Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color.White,
                            contentColor =
                                Color.Black
                        )
                ) {

                    Text(
                        text = "USE VOICE",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onBack,
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF151515),
                        contentColor =
                            Color.Gray
                    )
            ) {

                Text("BACK")
            }
        }
    }
}


// =============================================================
// START RECORDING HELPER
// =============================================================

private fun startRecording(
    context: android.content.Context,
    onRecorderCreated:
        (MediaRecorder, String) -> Unit,
    onError: (String) -> Unit
) {

    try {

        val file =
            File(
                context.cacheDir,
                "voice_${System.currentTimeMillis()}.m4a"
            )

        val recorder =
            MediaRecorder(context)

        recorder.setAudioSource(
            MediaRecorder.AudioSource.MIC
        )

        recorder.setOutputFormat(
            MediaRecorder.OutputFormat.MPEG_4
        )

        recorder.setAudioEncoder(
            MediaRecorder.AudioEncoder.AAC
        )

        recorder.setAudioSamplingRate(44100)

        recorder.setAudioEncodingBitRate(128000)

        recorder.setOutputFile(
            file.absolutePath
        )

        recorder.prepare()

        recorder.start()

        // IMPORTANT:
        // Save the path immediately after recording starts.
        onRecorderCreated(
            recorder,
            file.absolutePath
        )

    } catch (e: Exception) {

        onError(
            "Unable to start recording."
        )
    }
}


// =============================================================
// FORMAT TIME
// =============================================================

private fun formatTime(
    seconds: Int
): String {

    val minutes =
        seconds / 60

    val remainingSeconds =
        seconds % 60

    return String.format(
        "%02d:%02d",
        minutes,
        remainingSeconds
    )
}