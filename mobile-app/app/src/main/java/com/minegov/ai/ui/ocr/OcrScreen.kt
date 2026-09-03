package com.minegov.ai.ui.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.minegov.ai.ocr.OcrProcessor
import kotlinx.coroutines.launch

@Composable
fun OcrScreen(
    imageUri: String?,
    onBack: () -> Unit,
    onUseText: (String) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var extractedText by remember {
        mutableStateOf("")
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun runOcr() {

        if (imageUri.isNullOrBlank()) {
            errorMessage = "No document image selected."
            return
        }

        scope.launch {

            try {

                isProcessing = true
                errorMessage = null

                val uri = Uri.parse(imageUri)

                val bitmap: Bitmap? =
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use {
                            BitmapFactory.decodeStream(it)
                        }

                if (bitmap == null) {
                    throw Exception("Unable to read document image.")
                }

                extractedText =
                    OcrProcessor.recognizeText(bitmap)

                if (extractedText.isBlank()) {
                    errorMessage = "No readable text found."
                }

            } catch (e: Exception) {

                errorMessage =
                    e.message ?: "OCR failed."

            } finally {

                isProcessing = false
            }
        }
    }

    LaunchedEffect(imageUri) {
        if (!imageUri.isNullOrBlank()) {
            runOcr()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Document OCR",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Scan a mining licence, permit or inspection document.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (isProcessing) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    CircularProgressIndicator()

                    Text(
                        text = "Extracting text..."
                    )
                }
            }
        }

        errorMessage?.let {

            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = extractedText,
            onValueChange = {
                extractedText = it
            },
            modifier = Modifier
                .fillMaxWidth(),
            minLines = 10,
            label = {
                Text("Extracted Text")
            }
        )

        Button(
            onClick = {
                onUseText(extractedText)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = extractedText.isNotBlank() && !isProcessing
        ) {
            Text("Use Extracted Text")
        }

        OutlinedButton(
            onClick = {
                runOcr()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Text("Scan Again")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}