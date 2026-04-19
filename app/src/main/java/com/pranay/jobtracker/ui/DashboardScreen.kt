package com.pranay.jobtracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.gmail.GmailScopes
import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.AccountInfo
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.pranay.jobtracker.data.ApplicationStage
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onApplicationClick: (Int) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val applications by viewModel.applications.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val accounts by viewModel.accountRepository.getActiveAccountsFlow().collectAsState(initial = emptyList())
    val activeAccountId by viewModel.activeAccountFlow.collectAsState(initial = null)
    val activeAccount = accounts.find { it.accountId == activeAccountId }

    var showAccountSwitcher by remember { mutableStateOf(false) }
    var showNoAccountDialog by remember { mutableStateOf(false) }
    var showSmartFilterSheet by remember { mutableStateOf(false) }

    val selectedCompanies by viewModel.selectedCompanies.collectAsState()
    val companyGroups by viewModel.companyGroups.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val selectedStages by viewModel.selectedStages.collectAsState()
    val applicationJourneys by viewModel.applicationJourneys.collectAsState()
    val isJourneyModeEnabled by viewModel.isJourneyModeEnabled.collectAsState()

    val scope = rememberCoroutineScope()

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(GmailScopes.GMAIL_READONLY))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        task.addOnSuccessListener { account ->
            scope.launch {
                val email = account.email ?: return@launch
                val displayName = account.displayName ?: email
                val photoUrl = account.photoUrl?.toString() ?: ""
                val savedAccount = viewModel.accountRepository.addOrUpdateAccount(email, displayName, photoUrl)
                viewModel.accountRepository.setActiveAccount(savedAccount.accountId)
                viewModel.syncEmails()
            }
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    
    val onRefresh = {
        if (activeAccountId != null) {
            viewModel.syncEmails()
        } else if (accounts.isNotEmpty()) {
            scope.launch {
                viewModel.accountRepository.setActiveAccount(accounts.first().accountId)
                viewModel.syncEmails()
            }
        } else {
            showNoAccountDialog = true
            pullToRefreshState.endRefresh()
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }

    LaunchedEffect(isSyncing) {
        if (isSyncing) pullToRefreshState.startRefresh()
        else pullToRefreshState.endRefresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            HeaderSection(
                activeAccount = activeAccount,
                onProfileClick = { showAccountSwitcher = true }
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (accounts.isEmpty()) {
                EmptyStateView(
                    onAddAccountClick = { signInLauncher.launch(googleSignInClient.signInIntent) }
                )
            } else {
                StatsSection(applications)
                Spacer(modifier = Modifier.height(16.dp))
                JobActivityHeatmap(heatmapData, timeFilter.days)
                Spacer(modifier = Modifier.height(16.dp))
                // Recent Header + Fast Filter Launch Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Recent Applications", style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF2C2C2C),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                IconButton(
                                    onClick = { viewModel.isJourneyModeEnabled.value = false },
                                    modifier = Modifier.background(if (!isJourneyModeEnabled) Color(0xFF5C6BC0) else Color.Transparent, RoundedCornerShape(8.dp)).size(32.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = "List View", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.isJourneyModeEnabled.value = true },
                                    modifier = Modifier.background(if (isJourneyModeEnabled) Color(0xFF5C6BC0) else Color.Transparent, RoundedCornerShape(8.dp)).size(32.dp)
                                ) {
                                    Icon(Icons.Default.Timeline, contentDescription = "Journey View", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        IconButton(onClick = { showSmartFilterSheet = true }) {
                            Icon(Icons.Default.Star, "Smart Filters", tint = Color(0xFF5C6BC0))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isJourneyModeEnabled) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(applicationJourneys) { journey ->
                            val appColorString = activeAccount?.colorHash ?: "#5C6BC0"
                            val appColor = Color(android.graphics.Color.parseColor(appColorString))
                            JourneyTimelineRow(journey = journey, accountColor = appColor, onClick = onApplicationClick)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(applications) { app ->
                            val appColorString = activeAccount?.colorHash ?: "#5C6BC0"
                            val appColor = Color(android.graphics.Color.parseColor(appColorString))
                            ApplicationCard(app = app, accountColor = appColor, onClick = onApplicationClick)
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color(0xFF5C6BC0)
        )

        // Floating Stop Sync Pill
        androidx.compose.animation.AnimatedVisibility(
            visible = isSyncing,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                onClick = { viewModel.stopSyncing() },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF5C6BC0), // Using primary indigo to signal it's a main action
                tonalElevation = 12.dp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .wrapContentWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Syncing... Tap to stop",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    if (showAccountSwitcher) {
        AccountSwitcherSheet(
            activeAccount = activeAccount,
            accounts = accounts,
            onDismiss = { showAccountSwitcher = false },
            onAccountSelected = { account ->
                scope.launch {
                    viewModel.accountRepository.setActiveAccount(account.accountId)
                }
            },
            onAddAccount = {
                signInLauncher.launch(googleSignInClient.signInIntent)
            },
            onSignOut = { account ->
                scope.launch {
                    viewModel.accountRepository.signOutAccount(account.accountId)
                    googleSignInClient.signOut()
                }
            },
            onRemoveAccount = { account ->
                scope.launch {
                    viewModel.accountRepository.removeAccountAndData(account.accountId)
                    googleSignInClient.signOut()
                }
            },
            onWipeData = { viewModel.clearDatabase() }
        )
    }

    if (showSmartFilterSheet) {
        SmartFilterSheet(
            viewModel = viewModel,
            onDismiss = { showSmartFilterSheet = false }
        )
    }

    if (showNoAccountDialog) {
        AlertDialog(
            onDismissRequest = { showNoAccountDialog = false },
            title = { Text("Connect an Email Account") },
            text = { Text("To track job applications automatically, connect your Gmail account so the app can detect application confirmations and updates.") },
            confirmButton = {
                TextButton(onClick = {
                    showNoAccountDialog = false
                    signInLauncher.launch(googleSignInClient.signInIntent)
                }) {
                    Text("Add Google Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoAccountDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE0E0E0)
        )
    }
}

@Composable
fun HeaderSection(
    activeAccount: AccountInfo?,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("JobTracker", style = MaterialTheme.typography.titleLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar Button
            if (activeAccount != null) {
                AsyncImage(
                    model = activeAccount.photoUrl.ifBlank { "https://ui-avatars.com/api/?name=${activeAccount.displayName.replace(" ", "+")}&background=${activeAccount.colorHash.removePrefix("#")}&color=fff" },
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .clickable(onClick = onProfileClick),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = "Add Account", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun StatsSection(apps: List<JobApplication>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard(title = "Total", count = apps.size.toString(), modifier = Modifier.weight(1f))
        StatCard(title = "Interviews", count = apps.count { it.stage == ApplicationStage.INTERVIEW.name }.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(title: String, count: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFA0A0A0))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, style = MaterialTheme.typography.titleLarge, color = Color(0xFFE0E0E0))
        }
    }
}

@Composable
fun ApplicationCard(app: JobApplication, accountColor: Color, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(app.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accountColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(app.companyName, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(app.jobTitle, color = Color(0xFFA0A0A0))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stageEnum = runCatching { ApplicationStage.valueOf(app.stage) }.getOrDefault(ApplicationStage.APPLIED)
                Badge(stage = stageEnum)
                Text(app.dateApplied, color = Color(0xFFA0A0A0), style = MaterialTheme.typography.bodySmall)
            }
            if (!app.subStatus.isNullOrBlank() && app.subStatus != app.status) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(app.subStatus!!, color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun Badge(stage: ApplicationStage) {
    val baseColor = stage.getColor()
    
    Box(
        modifier = Modifier
            .background(baseColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(stage.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = baseColor)
    }
}

@Composable
fun EmptyStateView(onAddAccountClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "No Account",
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF5C6BC0)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No email account connected",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect your Gmail account to automatically track job applications from your inbox.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFA0A0A0),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddAccountClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
        ) {
            Text("Add Account")
        }
    }
}

fun ApplicationStage.getColor(): Color = when(this) {
    ApplicationStage.APPLIED -> Color(0xFF9E9E9E)       // Gray
    ApplicationStage.IN_REVIEW -> Color(0xFF29B6F6)     // Light Blue
    ApplicationStage.ASSESSMENT -> Color(0xFFFFA726)    // Orange
    ApplicationStage.INTERVIEW -> Color(0xFFAB47BC)     // Purple
    ApplicationStage.OFFER -> Color(0xFF66BB6A)         // Green
    ApplicationStage.REJECTED -> Color(0xFFEF5350)      // Red
    ApplicationStage.WITHDRAWN -> Color(0xFF8D6E63)     // Brown
}

@Composable
fun JobActivityHeatmap(densityMap: Map<LocalDate, Int>, trailingDays: Int?) {
    val activeDays = trailingDays ?: 365
    val maxDensity = densityMap.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val today = LocalDate.now()
    val startDate = today.minusDays(activeDays.toLong())
    val days = (0..activeDays).map { startDate.plusDays(it.toLong()) }
    
    val title = when (activeDays) {
        7 -> "Activity (Last 7 Days)"
        30 -> "Activity (Last 30 Days)"
        90 -> "Activity (Last 3 Months)"
        180 -> "Activity (Last 6 Months)"
        365 -> "Activity (Last Year)"
        else -> "Activity"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFA0A0A0))
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = true
        ) {
            val weeks = days.chunked(7).reversed()
            items(weeks) { weekDays ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekDays.forEach { day ->
                        val count = densityMap[day] ?: 0
                        val alpha = if (count == 0) 0.1f else (count.toFloat() / maxDensity).coerceIn(0.3f, 1f)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF5C6BC0).copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFilterSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val aiFilters by viewModel.aiSmartFilters.collectAsState()
    val isFetchingAi by viewModel.isFetchingAiFilters.collectAsState()
    
    val selectedStages by viewModel.selectedStages.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val companyGroups by viewModel.companyGroups.collectAsState()
    val selectedCompanies by viewModel.selectedCompanies.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredCompanies = companyGroups.keys.filter {
        it.contains(searchQuery, ignoreCase = true)
    }.sorted()

    LaunchedEffect(Unit) {
        viewModel.fetchSmartFilters()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "AI", tint = Color(0xFF5C6BC0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Smart Suggestions", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                
                if (isFetchingAi) {
                    CircularProgressIndicator(color = Color(0xFF5C6BC0), modifier = Modifier.padding(bottom = 16.dp))
                } else if (!aiFilters.isNullOrEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                        items(aiFilters!!) { suggestion ->
                            Card(
                                onClick = { 
                                    viewModel.applySmartFilter(suggestion)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                                modifier = Modifier.width(220.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(suggestion.label, style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C6BC0), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(suggestion.rationale, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A0A0), fontStyle = FontStyle.Italic)
                                }
                            }
                        }
                    }
                } else {
                    Text("No suggestions available right now.", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))
                }
            }

            item {
                Text("Timeline", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                    items(TimeFilter.values()) { filter ->
                        FilterChip(
                            selected = timeFilter == filter,
                            onClick = { viewModel.setTimeFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF5C6BC0).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF5C6BC0),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
            }

            item {
                Text("Status", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                    items(ApplicationStage.values()) { stage ->
                        val isSelected = selectedStages.contains(stage)
                        val stageColor = stage.getColor()
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleStageFilter(stage) },
                            label = { Text(stage.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = stageColor.copy(alpha = 0.2f),
                                selectedLabelColor = stageColor,
                                labelColor = Color(0xFFA0A0A0)
                            )
                        )
                    }
                }
            }

            item {
                Text("Company", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text("Search companies...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5C6BC0),
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            items(filteredCompanies) { company ->
                val isSelected = selectedCompanies.contains(company)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleCompanyFilter(company) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://logo.clearbit.com/${company.replace(" ", "").lowercase()}.com",
                            contentDescription = "$company Logo",
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.DarkGray),
                            contentScale = ContentScale.Crop,
                            error = coil.compose.rememberAsyncImagePainter(android.R.drawable.sym_def_app_icon)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(company, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5C6BC0))
                    )
                }
            }
        }
    }
}
@Composable
fun JourneyTimelineRow(
    journey: com.pranay.jobtracker.domain.ApplicationJourney,
    accountColor: Color,
    onClick: (Int) -> Unit
) {
    val app = journey.application
    val events = journey.timeline.sortedBy { it.dateEpochMs }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(app.id) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accountColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${app.companyName} — ${app.jobTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val uniqueEvents = events.distinctBy { it.detectedStatus }.filter { it.detectedStatus.isNotBlank() && it.detectedStatus != "Unknown" }
                items(uniqueEvents) { event ->
                    JourneyNode(label = event.detectedStatus, isFinal = false, color = Color.Gray)
                    JourneyConnector()
                }
                
                val currentStage = runCatching { com.pranay.jobtracker.data.ApplicationStage.valueOf(app.stage) }.getOrNull()
                val stageLabel = currentStage?.label ?: app.stage
                val stageColor = currentStage?.getColor() ?: Color(0xFF5C6BC0)
                
                item {
                    JourneyNode(label = stageLabel, isFinal = true, color = stageColor)
                }
            }
        }
    }
}

@Composable
fun JourneyNode(label: String, isFinal: Boolean, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isFinal) color.copy(alpha = 0.2f) else Color(0xFF2C2C2C),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = label,
            color = if (isFinal) color else Color.LightGray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

@Composable
fun JourneyConnector() {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(2.dp)
            .background(Color(0xFF404040))
    )
}
