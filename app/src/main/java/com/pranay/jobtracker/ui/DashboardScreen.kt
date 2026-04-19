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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
    var showCompanyFilter by remember { mutableStateOf(false) }

    val selectedCompanies by viewModel.selectedCompanies.collectAsState()
    val companyGroups by viewModel.companyGroups.collectAsState()

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
                onProfileClick = { showAccountSwitcher = true },
                onDeleteClick = { viewModel.clearDatabase() },
                onStopClick = { viewModel.stopSyncing() },
                isSyncing = isSyncing
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (accounts.isEmpty()) {
                EmptyStateView(
                    onAddAccountClick = { signInLauncher.launch(googleSignInClient.signInIntent) }
                )
            } else {
                StatsSection(applications)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Company Filter Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Recent Applications", style = MaterialTheme.typography.titleLarge)
                    if (selectedCompanies.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCompanyFilters() }) {
                            Text("Clear all")
                        }
                    }
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(selectedCompanies.toList()) { company ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleCompanyFilter(company) },
                            label = { Text(company) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove") },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Color(0xFF5C6BC0).copy(alpha = 0.2f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    if (companyGroups.isNotEmpty()) {
                        item {
                            InputChip(
                                selected = false,
                                onClick = { showCompanyFilter = true },
                                label = { Text("Add company") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add") }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color(0xFF5C6BC0)
        )
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
            }
        )
    }

    if (showCompanyFilter) {
        CompanyFilterSheet(
            companyGroups = companyGroups,
            selectedCompanies = selectedCompanies,
            onDismiss = { showCompanyFilter = false },
            onToggleCompany = { viewModel.toggleCompanyFilter(it) }
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
    onProfileClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStopClick: () -> Unit,
    isSyncing: Boolean
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
            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("Wipe")
            }
            if (isSyncing) {
                Button(
                    onClick = onStopClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                ) {
                    Text("Stop Sync")
                }
            }

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
        StatCard(title = "Interviews", count = apps.count { it.status.contains("Interview") }.toString(), modifier = Modifier.weight(1f))
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Badge(status = app.status)
                Text(app.dateApplied, color = Color(0xFFA0A0A0), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun Badge(status: String) {
    val bgColor = when {
        status.contains("Interview") -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        status.contains("Reject") -> Color(0xFFF44336).copy(alpha = 0.2f)
        else -> Color(0xFF5C6BC0).copy(alpha = 0.2f)
    }
    val textColor = when {
        status.contains("Interview") -> Color(0xFF4CAF50)
        status.contains("Reject") -> Color(0xFFF44336)
        else -> Color(0xFF5C6BC0)
    }
    
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(status, color = textColor, style = MaterialTheme.typography.labelSmall)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyFilterSheet(
    companyGroups: Map<String, List<String>>,
    selectedCompanies: Set<String>,
    onDismiss: () -> Unit,
    onToggleCompany: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCompanies = companyGroups.keys.filter {
        it.contains(searchQuery, ignoreCase = true)
    }.sorted()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Filter by Company",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(filteredCompanies) { company ->
                    val isSelected = selectedCompanies.contains(company)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCompany(company) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = "https://logo.clearbit.com/${company.replace(" ", "").lowercase()}.com",
                                contentDescription = "$company Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray),
                                contentScale = ContentScale.Crop,
                                error = coil.compose.rememberAsyncImagePainter(android.R.drawable.sym_def_app_icon) // reliable fallback
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(company, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null, // Handled by Row click
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF5C6BC0)
                            )
                        )
                    }
                }
            }
        }
    }
}

