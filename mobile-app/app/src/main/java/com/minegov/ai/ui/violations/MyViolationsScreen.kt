package com.minegov.ai.ui.violations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minegov.ai.data.local.AppDatabase
import com.minegov.ai.data.local.ViolationEntity
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyViolationsScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val violations by database
        .violationDao()
        .observeAll()
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {

        // =================================================
        // HEADER
        // =================================================

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            TextButton(
                onClick = onBack
            ) {

                Text(
                    text = "←",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }

            Text(
                text = "My Violations",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =================================================
        // EMPTY STATE
        // =================================================

        if (violations.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = "No violations recorded.",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Submitted violations will appear here.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

        } else {

            // =================================================
            // REAL ROOM DATA
            // =================================================

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                items(
                    items = violations,
                    key = { it.localId }
                ) { violation ->

                    ViolationItem(
                        violation = violation
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }
            }
        }
    }
}


// =============================================================
// VIOLATION ITEM
// =============================================================

@Composable
private fun ViolationItem(
    violation: ViolationEntity
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151515))
            .padding(18.dp)
    ) {

        Text(
            text = "ID: ${violation.localId.take(8).uppercase()}",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = violation.violationType,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "${violation.severity.uppercase()}  •  ${violation.syncStatus}",
            color = Color.LightGray,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = violation.description,
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "Evidence: " +
                        "${if (violation.photoUri != null) "Photo " else ""}" +
                        "${if (violation.videoUri != null) "Video " else ""}" +
                        "${if (violation.voiceUri != null) "Voice" else ""}"
                            .trim(),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}