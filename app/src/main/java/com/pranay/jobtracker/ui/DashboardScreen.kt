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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

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
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(GmailScopes.GMAIL_READONLY))) {
            viewModel.syncEmails()
        } else {
            signInLauncher.launch(googleSignInClient.signInIntent)
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
            StatsSection(applications)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Recent Applications", style = MaterialTheme.typography.titleLarge)
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
                        .clickable(onClick = onProfileClick)
                )
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
