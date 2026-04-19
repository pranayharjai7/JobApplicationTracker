package com.pranay.jobtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.pranay.jobtracker.data.EmailEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    onBackClick: () -> Unit,
    viewModel: ApplicationDetailViewModel = hiltViewModel()
) {
    val application by viewModel.application.collectAsState()
    val events by viewModel.events.collectAsState()
    val accountInfo by viewModel.accountInfo.collectAsState()

    val accountColor = accountInfo?.colorHash?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF5C6BC0)

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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(accountColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(app.jobTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }

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
                    text = app.summary ?: app.notes ?: "No summary available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE0E0E0)
                )

                if (events.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF333333))
                    Text("Email Timeline", fontWeight = FontWeight.Bold, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    events.forEach { event ->
                        EmailEventItem(event, accountColor)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmailEventItem(event: EmailEvent, accountColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accountColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Badge(status = event.detectedStatus)
            }
            Text(
                text = event.date,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA0A0A0)
            )
        }
        if (!event.summary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE0E0E0)
            )
        } else if (event.snippet.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.snippet.take(150),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA0A0A0)
            )
        }
    }
}
