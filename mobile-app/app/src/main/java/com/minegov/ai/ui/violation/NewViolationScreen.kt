package com.minegov.ai.ui.violation

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.minegov.ai.data.local.AppDatabase
import com.minegov.ai.data.local.ViolationEntity
import com.minegov.ai.location.LocationManager
import com.minegov.ai.ocr.OcrProcessor
import com.minegov.ai.ui.ocr.DocumentCaptureScreen
import com.minegov.ai.sync.SyncScheduler
import kotlinx.coroutines.launch

@Composable
fun NewViolationScreen(
    onBack: () -> Unit
) {

    // =================================================
    // BASIC STATE
    // =================================================

    var selectedType by remember {
        mutableStateOf<String?>(null)
    }

    var currentStep by remember {
        mutableStateOf(1)
    }

    // =================================================
    // LOCATION STATE
    // =================================================

    var latitude by remember {
        mutableStateOf<Double?>(null)
    }

    var longitude by remember {
        mutableStateOf<Double?>(null)
    }

    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var isLoadingLocation by remember {
        mutableStateOf(false)
    }

    // =================================================
    // EVIDENCE STATE
    // =================================================

    var photoUri by remember {
        mutableStateOf<String?>(null)
    }

    var videoUri by remember {
        mutableStateOf<String?>(null)
    }

    var voiceUri by remember {
        mutableStateOf<String?>(null)
    }

    var documentUri by remember {
        mutableStateOf<String?>(null)
    }

    // =================================================
    // DETAILS STATE
    // =================================================

    var description by remember {
        mutableStateOf("")
    }

    var observedCondition by remember {
        mutableStateOf("")
    }

    var selectedSeverity by remember {
        mutableStateOf<String?>(null)
    }

    // =================================================
    // OCR STATE
    // =================================================

    var ocrText by remember {
        mutableStateOf("")
    }

    var isOcrProcessing by remember {
        mutableStateOf(false)
    }

    var ocrError by remember {
        mutableStateOf<String?>(null)
    }

    // =================================================
    // SUBMISSION STATE
    // =================================================

    var submitted by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var saveError by remember {
        mutableStateOf<String?>(null)
    }

    // =================================================
    // CONTEXT
    // =================================================

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val locationManager = remember {
        LocationManager(context)
    }

    // =================================================
    // OCR PROCESSOR
    // =================================================

    suspend fun runOcr() {

        if (documentUri.isNullOrBlank()) {
            ocrError = "Scan a document first."
            return
        }

        try {

            isOcrProcessing = true
            ocrError = null

            val uri = Uri.parse(documentUri)

            val bitmap: Bitmap? =
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }

            if (bitmap == null) {
                throw Exception("Unable to read document image.")
            }

            ocrText =
                OcrProcessor.recognizeText(bitmap)

            if (ocrText.isBlank()) {
                ocrError = "No readable text found."
            }

        } catch (e: Exception) {

            ocrError =
                e.message ?: "OCR failed."

        } finally {

            isOcrProcessing = false
        }
    }

    // =================================================
    // LOCATION PERMISSION
    // =================================================

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseGranted =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            if (fineGranted || coarseGranted) {

                scope.launch {

                    isLoadingLocation = true
                    locationError = null

                    val location =
                        locationManager.getCurrentLocation()

                    if (location != null) {

                        latitude = location.latitude
                        longitude = location.longitude

                    } else {

                        locationError =
                            "Unable to get current location."
                    }

                    isLoadingLocation = false
                }

            } else {

                locationError =
                    "Location permission is required."
            }
        }

    // =================================================
    // LOCATION REQUEST
    // =================================================

    fun requestLocation() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {

            scope.launch {

                isLoadingLocation = true
                locationError = null

                val location =
                    locationManager.getCurrentLocation()

                if (location != null) {

                    latitude = location.latitude
                    longitude = location.longitude

                } else {

                    locationError =
                        "Unable to get current location."
                }

                isLoadingLocation = false
            }

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // =================================================
    // STEP 3 — PHOTO
    // =================================================

    if (currentStep == 3) {

        CameraCaptureScreen(

            onBack = {
                currentStep = 2
            },

            onPhotoCaptured = { uri ->

                photoUri = uri
                currentStep = 4
            }
        )

        return
    }

    // =================================================
    // STEP 4 — VIDEO
    // =================================================

    if (currentStep == 4) {

        VideoCaptureScreen(

            onBack = {
                currentStep = 3
            },

            onVideoCaptured = { uri ->

                videoUri = uri
                currentStep = 5
            }
        )

        return
    }

    // =================================================
    // STEP 5 — VOICE NOTE
    // =================================================

    if (currentStep == 5) {

        VoiceNoteScreen(

            onBack = {
                currentStep = 4
            },

            onVoiceCaptured = { path ->

                voiceUri = path
                currentStep = 6
            }
        )

        return
    }

    // =================================================
    // STEP 8 — DOCUMENT CAPTURE (OPTIONAL)
    // =================================================

    if (currentStep == 8) {
        DocumentCaptureScreen(
            onBack = {
                currentStep = 6
            },
            onDocumentCaptured = { uri ->
                documentUri = uri
                ocrText = ""
                ocrError = null
                currentStep = 6
                scope.launch {
                    runOcr()
                }
            }
        )
        return
    }

    // =================================================
    // SUBMITTED SCREEN
    // =================================================

    if (submitted) {

        SubmissionSuccessScreen(
            onDone = onBack
        )

        return
    }

    // =================================================
    // MAIN CONTAINER
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
            text = "New Violation",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Step $currentStep of 7",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // =================================================
        // STEP 1
        // =================================================

        if (currentStep == 1) {

            StepTypeScreen(

                selectedType = selectedType,

                onSelected = {
                    selectedType = it
                },

                onBack = onBack,

                onNext = {
                    currentStep = 2
                    requestLocation()
                }
            )
        }

        // =================================================
        // STEP 2
        // =================================================

        else if (currentStep == 2) {

            StepLocationScreen(

                selectedType = selectedType,

                latitude = latitude,

                longitude = longitude,

                isLoadingLocation = isLoadingLocation,

                locationError = locationError,

                onBack = {
                    currentStep = 1
                },

                onRefresh = {
                    requestLocation()
                },

                onNext = {
                    currentStep = 3
                }
            )
        }

        // =================================================
        // STEP 6
        // =================================================

        else if (currentStep == 6) {

            StepDetailsScreen(

                description = description,

                observedCondition = observedCondition,

                selectedSeverity = selectedSeverity,

                latitude = latitude,

                longitude = longitude,

                photoUri = photoUri,

                videoUri = videoUri,

                voiceUri = voiceUri,

                documentUri = documentUri,
                ocrText = ocrText,
                isOcrProcessing = isOcrProcessing,
                ocrError = ocrError,

                onRunOcr = {
                    currentStep = 8
                },

                onOcrTextChange = {
                    ocrText = it
                },

                onDescriptionChange = {
                    description = it
                },

                onObservedConditionChange = {
                    observedCondition = it
                },

                onSeveritySelected = {
                    selectedSeverity = it
                },

                onBack = {
                    currentStep = 5
                },

                onNext = {
                    currentStep = 7
                }
            )
        }

        // =================================================
        // STEP 7
        // =================================================

        else if (currentStep == 7) {

            StepReviewScreen(

                selectedType = selectedType,

                latitude = latitude,

                longitude = longitude,

                description = description,

                observedCondition = observedCondition,

                selectedSeverity = selectedSeverity,

                photoUri = photoUri,

                videoUri = videoUri,

                voiceUri = voiceUri,

                documentUri = documentUri,
                ocrText = ocrText,

                isSaving = isSaving,

                saveError = saveError,

                onEdit = {
                    currentStep = 6
                },

                onBack = {
                    currentStep = 6
                },

                onSubmit = {

                    if (isSaving) return@StepReviewScreen

                    scope.launch {

                        try {

                            isSaving = true
                            saveError = null

                            // =================================================
                            // CREATE LOCAL VIOLATION
                            // =================================================

                            val violation =
                                ViolationEntity(

                                    violationType =
                                        selectedType ?: "Other",

                                    description =
                                        description,

                                    observedCondition =
                                        observedCondition,

                                    severity =
                                        selectedSeverity ?: "Low",

                                    latitude =
                                        latitude,

                                    longitude =
                                        longitude,

                                    photoUri =
                                        photoUri,

                                    videoUri =
                                        videoUri,

                                    voiceUri =
                                        voiceUri,

                                    documentUri =
                                        documentUri,

                                    ocrText =
                                        ocrText.ifBlank { null },

                                    syncStatus =
                                        "PENDING"
                                )

                            // =================================================
                            // SAVE TO ROOM
                            // =================================================

                            database
                                .violationDao()
                                .insert(violation)

                            // =================================================
                            // SCHEDULE WORKMANAGER SYNC
                            // =================================================

                            SyncScheduler.scheduleSync(context)

                            // =================================================
                            // SHOW SUCCESS
                            // =================================================

                            submitted = true

                        } catch (e: Exception) {

                            saveError =
                                e.message
                                    ?: "Failed to save violation."

                        } finally {

                            isSaving = false
                        }
                    }
                }
            )
        }
    }
}


// =============================================================
// STEP 1 — TYPE
// =============================================================

@Composable
private fun ColumnScope.StepTypeScreen(
    selectedType: String?,
    onSelected: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {

    Text(
        text = "What type of violation?",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(
        modifier = Modifier.height(20.dp)
    )

    TypeRow(
        first = "Safety",
        second = "Environment",
        selectedType = selectedType,
        onSelected = onSelected
    )

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    TypeRow(
        first = "Operations",
        second = "Labor",
        selectedType = selectedType,
        onSelected = onSelected
    )

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    TypeRow(
        first = "Equipment",
        second = "Other",
        selectedType = selectedType,
        onSelected = onSelected
    )

    Spacer(
        modifier = Modifier.weight(1f)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Button(
            onClick = onBack,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF202020),
                contentColor = Color.White
            )
        ) {
            Text("BACK")
        }

        Button(
            onClick = onNext,
            enabled = selectedType != null,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor =
                    Color(0xFF202020),
                disabledContentColor =
                    Color(0xFF666666)
            )
        ) {

            Text(
                text = "NEXT",
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// =============================================================
// STEP 2 — LOCATION
// =============================================================

@Composable
private fun ColumnScope.StepLocationScreen(
    selectedType: String?,
    latitude: Double?,
    longitude: Double?,
    isLoadingLocation: Boolean,
    locationError: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNext: () -> Unit
) {

    Text(
        text = "Inspection Location",
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(
        modifier = Modifier.height(10.dp)
    )

    Text(
        text =
            "Capture the current GPS coordinates of the inspection.",
        color = Color.Gray,
        fontSize = 14.sp
    )

    Spacer(
        modifier = Modifier.height(30.dp)
    )

    if (isLoadingLocation) {

        Text(
            text = "Getting current location...",
            color = Color.White,
            fontSize = 16.sp
        )

    } else if (
        latitude != null &&
        longitude != null
    ) {

        Text(
            text = "LOCATION CAPTURED",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Latitude",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Text(
            text = String.format("%.6f", latitude),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = "Longitude",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Text(
            text = String.format("%.6f", longitude),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Text(
            text = "Violation Type: $selectedType",
            color = Color.LightGray,
            fontSize = 14.sp
        )

    } else {

        Text(
            text = "Location not captured",
            color = Color.Gray,
            fontSize = 16.sp
        )
    }

    locationError?.let {

        Spacer(
            modifier = Modifier.height(15.dp)
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Button(
            onClick = onBack,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    Color(0xFF202020),
                contentColor =
                    Color.White
            )
        ) {
            Text("BACK")
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    Color.White,
                contentColor =
                    Color.Black
            )
        ) {

            Text(
                text =
                    if (latitude != null) {
                        "REFRESH GPS"
                    } else {
                        "GET GPS"
                    },
                fontWeight =
                    FontWeight.Bold
            )
        }
    }

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    Button(
        onClick = onNext,
        enabled =
            latitude != null &&
                    longitude != null,
        modifier =
            Modifier.fillMaxWidth(),
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
            text = "NEXT: PHOTO",
            fontWeight =
                FontWeight.Bold
        )
    }
}


// =============================================================
// STEP 6 — DETAILS
// =============================================================

@Composable
private fun StepDetailsScreen(
    description: String,
    observedCondition: String,
    selectedSeverity: String?,
    latitude: Double?,
    longitude: Double?,
    photoUri: String?,
    videoUri: String?,
    voiceUri: String?,
    documentUri: String?,
    ocrText: String,
    isOcrProcessing: Boolean,
    ocrError: String?,
    onRunOcr: () -> Unit,
    onOcrTextChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onObservedConditionChange: (String) -> Unit,
    onSeveritySelected: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        Text(
            text = "Violation Details",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "Describe what you observed during the inspection.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Description",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            placeholder = {
                Text(
                    text =
                        "Example: Worker operating near excavation without required PPE...",
                    color = Color.Gray
                )
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor =
                        Color(0xFF555555),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor =
                        Color.Gray,
                    unfocusedPlaceholderColor =
                        Color.Gray
                )
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Observed Condition",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = observedCondition,
            onValueChange = onObservedConditionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            placeholder = {
                Text(
                    text =
                        "What exactly was observed at the site?",
                    color = Color.Gray
                )
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor =
                        Color(0xFF555555),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor =
                        Color.Gray,
                    unfocusedPlaceholderColor =
                        Color.Gray
                )
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "DOCUMENT OCR",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Scan a separate licence, permit, certificate or inspection document. OCR runs automatically after capture.",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onRunOcr,
            enabled = !isOcrProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF202020),
                disabledContentColor = Color(0xFF666666)
            )
        ) {
            Text(
                text = if (isOcrProcessing) {
                    "SCANNING..."
                } else {
                    if (documentUri != null) "RESCAN DOCUMENT" else "SCAN DOCUMENT WITH OCR"
                },
                fontWeight = FontWeight.Bold
            )
        }

        ocrError?.let {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "OCR ERROR: $it",
                color = Color.White,
                fontSize = 13.sp
            )
        }

        if (ocrText.isNotBlank()) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        text = "EXTRACTED TEXT",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = ocrText,
                        onValueChange = onOcrTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = {
                            Text("OCR Text")
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Button(
                        onClick = {
                            if (ocrText.isNotBlank()) {
                                onDescriptionChange(
                                    if (description.isBlank()) {
                                        ocrText
                                    } else {
                                        "$description\\n\\n$ocrText"
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("USE IN DESCRIPTION")
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Severity",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SeverityButton(
                title = "Low",
                selected =
                    selectedSeverity == "Low",
                onClick = {
                    onSeveritySelected("Low")
                },
                modifier =
                    Modifier.weight(1f)
            )

            SeverityButton(
                title = "Medium",
                selected =
                    selectedSeverity == "Medium",
                onClick = {
                    onSeveritySelected("Medium")
                },
                modifier =
                    Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SeverityButton(
                title = "High",
                selected =
                    selectedSeverity == "High",
                onClick = {
                    onSeveritySelected("High")
                },
                modifier =
                    Modifier.weight(1f)
            )

            SeverityButton(
                title = "Critical",
                selected =
                    selectedSeverity == "Critical",
                onClick = {
                    onSeveritySelected("Critical")
                },
                modifier =
                    Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Evidence Collected",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        EvidenceRow(
            title = "GPS Location",
            available =
                latitude != null &&
                        longitude != null
        )

        EvidenceRow(
            title = "Photo Evidence",
            available =
                photoUri != null
        )

        EvidenceRow(
            title = "Video Evidence",
            available =
                videoUri != null
        )

        EvidenceRow(
            title = "Voice Note",
            available =
                voiceUri != null
        )

        EvidenceRow(
            title = "Document",
            available = documentUri != null
        )

        if (ocrText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            ReviewSection(title = "OCR TEXT")
            ReviewBox(value = ocrText)
        }

        EvidenceRow(
            title = "Document + OCR",
            available =
                documentUri != null && ocrText.isNotBlank()
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = onBack,
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
                Text("BACK")
            }

            Button(
                onClick = onNext,
                enabled =
                    description.isNotBlank() &&
                            observedCondition.isNotBlank() &&
                            selectedSeverity != null,
                modifier =
                    Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
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
                    text = "NEXT: REVIEW",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}


// =============================================================
// STEP 7 — REVIEW
// =============================================================

@Composable
private fun StepReviewScreen(
    selectedType: String?,
    latitude: Double?,
    longitude: Double?,
    description: String,
    observedCondition: String,
    selectedSeverity: String?,
    photoUri: String?,
    videoUri: String?,
    voiceUri: String?,
    documentUri: String?,
    ocrText: String,
    isSaving: Boolean,
    saveError: String?,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        Text(
            text = "Review Violation",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "Verify all information before submitting.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        ReviewSection(
            title = "Violation Type"
        )

        ReviewValue(
            value =
                selectedType
                    ?: "Not selected"
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ReviewSection(
            title = "Severity"
        )

        ReviewValue(
            value =
                selectedSeverity
                    ?: "Not selected"
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ReviewSection(
            title = "Description"
        )

        ReviewBox(
            value = description
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ReviewSection(
            title = "Observed Condition"
        )

        ReviewBox(
            value = observedCondition
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ReviewSection(
            title = "Inspection Location"
        )

        if (
            latitude != null &&
            longitude != null
        ) {

            ReviewValue(
                value =
                    "Latitude: ${
                        String.format(
                            "%.6f",
                            latitude
                        )
                    }\nLongitude: ${
                        String.format(
                            "%.6f",
                            longitude
                        )
                    }"
            )

        } else {

            ReviewValue(
                value = "Location unavailable"
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        ReviewSection(
            title = "Evidence"
        )

        EvidenceRow(
            title = "Photo",
            available =
                photoUri != null
        )

        EvidenceRow(
            title = "Video",
            available =
                videoUri != null
        )

        EvidenceRow(
            title = "Voice Note",
            available =
                voiceUri != null
        )

        saveError?.let {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "ERROR: $it",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = onEdit,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    Color(0xFF202020),
                contentColor =
                    Color.White,
                disabledContainerColor =
                    Color(0xFF151515),
                disabledContentColor =
                    Color(0xFF555555)
            )
        ) {

            Text(
                text = "EDIT DETAILS",
                fontWeight =
                    FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onSubmit,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    Color.White,
                contentColor =
                    Color.Black,
                disabledContainerColor =
                    Color(0xFF555555),
                disabledContentColor =
                    Color(0xFFAAAAAA)
            )
        ) {

            Text(
                text =
                    if (isSaving) {
                        "SAVING..."
                    } else {
                        "SUBMIT VIOLATION"
                    },
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onBack,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    Color(0xFF151515),
                contentColor =
                    Color.Gray
            )
        ) {

            Text("BACK")
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}


// =============================================================
// SUBMISSION SUCCESS
// =============================================================

@Composable
private fun SubmissionSuccessScreen(
    onDone: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp),
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "✓",
            color = Color.White,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Violation Submitted",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text =
                "The violation has been recorded successfully.",
            color = Color.Gray,
            fontSize = 15.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text =
                "Saved locally and marked as PENDING for synchronization.",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(35.dp)
        )

        Button(
            onClick = onDone,
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
                text = "DONE",
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// =============================================================
// REVIEW SECTION
// =============================================================

@Composable
private fun ReviewSection(
    title: String
) {

    Text(
        text = title.uppercase(),
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(6.dp)
    )
}


// =============================================================
// REVIEW VALUE
// =============================================================

@Composable
private fun ReviewValue(
    value: String
) {

    Text(
        text = value,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}


// =============================================================
// REVIEW BOX
// =============================================================

@Composable
private fun ReviewBox(
    value: String
) {

    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF151515)
            )
            .padding(14.dp),
        color = Color.White,
        fontSize = 14.sp
    )
}


// =============================================================
// EVIDENCE ROW
// =============================================================

@Composable
private fun EvidenceRow(
    title: String,
    available: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {

        Text(
            text =
                if (available) {
                    "✓"
                } else {
                    "○"
                },
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = title,
            color =
                if (available) {
                    Color.White
                } else {
                    Color.Gray
                },
            fontSize = 14.sp
        )
    }
}


// =============================================================
// SEVERITY BUTTON
// =============================================================

@Composable
private fun SeverityButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {

    Button(
        onClick = onClick,
        modifier =
            modifier.height(55.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (selected) {
                        Color.White
                    } else {
                        Color(0xFF151515)
                    },
                contentColor =
                    if (selected) {
                        Color.Black
                    } else {
                        Color.White
                    }
            )
    ) {

        Text(
            text = title,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}


// =============================================================
// TYPE ROW
// =============================================================

@Composable
private fun TypeRow(
    first: String,
    second: String,
    selectedType: String?,
    onSelected: (String) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        TypeButton(
            title = first,
            selected =
                selectedType == first,
            onClick = {
                onSelected(first)
            },
            modifier =
                Modifier.weight(1f)
        )

        TypeButton(
            title = second,
            selected =
                selectedType == second,
            onClick = {
                onSelected(second)
            },
            modifier =
                Modifier.weight(1f)
        )
    }
}


// =============================================================
// TYPE BUTTON
// =============================================================

@Composable
private fun TypeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {

    Button(
        onClick = onClick,
        modifier =
            modifier.height(90.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (selected) {
                    Color.White
                } else {
                    Color(0xFF151515)
                },
            contentColor =
                if (selected) {
                    Color.Black
                } else {
                    Color.White
                }
        )
    ) {

        Text(
            text = title,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}
