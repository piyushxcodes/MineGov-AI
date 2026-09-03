package com.minegov.ai.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            TextButton(onClick = onBack) {
                Text(
                    text = "←",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }

            Text(
                text = "Notifications",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        NotificationItem(
            title = "Violation submitted",
            message = "Your latest violation was successfully saved."
        )

        Spacer(modifier = Modifier.height(10.dp))

        NotificationItem(
            title = "Inspection assigned",
            message = "A new inspection has been assigned to you."
        )

        Spacer(modifier = Modifier.height(10.dp))

        NotificationItem(
            title = "Sync completed",
            message = "Pending offline data has been synchronized."
        )
    }
}

@Composable
private fun NotificationItem(
    title: String,
    message: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151515))
            .padding(18.dp)
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = message,
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}