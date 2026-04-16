package com.pranay.jobtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    onBackClick: () -> Unit,
    viewModel: ApplicationDetailViewModel = hiltViewModel()
) {
    val application by viewModel.application.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(application?.companyName ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val app = application
        if (app == null) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(app.jobTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(app.status)
                    Text("Applied: ${app.dateApplied}", color = Color.Gray)
                }

                HorizontalDivider(color = Color(0xFF333333))

                if (!app.recruiterEmail.isNullOrBlank()) {
                    Text("Recruiter:", fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Text(app.recruiterEmail, color = Color(0xFF5C6BC0))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("AI Application Summary", fontWeight = FontWeight.Bold, color = Color.LightGray)
                Text(
                    text = app.notes ?: "No summary available.", 
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}
