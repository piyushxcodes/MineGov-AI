package com.minegov.ai.ui.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
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
                text = "Profile",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "Inspector Name",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Field Inspector",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        ProfileItem("Employee ID", "INS-10045")

        ProfileItem("Role", "Inspector")

        ProfileItem("Status", "Active")

        ProfileItem("Sync", "Automatic")
    }
}

@Composable
private fun ProfileItem(
    title: String,
    value: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {

        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}