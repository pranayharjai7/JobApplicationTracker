package com.pranay.jobtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pranay.jobtracker.data.AccountInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    activeAccount: AccountInfo?,
    accounts: List<AccountInfo>,
    onDismiss: () -> Unit,
    onAccountSelected: (AccountInfo) -> Unit,
    onAddAccount: () -> Unit,
    onSignOut: (AccountInfo) -> Unit,
    onRemoveAccount: (AccountInfo) -> Unit,
    onWipeData: () -> Unit
) {
    var showRemoveDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showWipeDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Active Account Header
            if (activeAccount != null) {
                AccountItem(
                    account = activeAccount,
                    isActive = true,
                    onClick = { onDismiss() }
                )
                Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))
            }

            // Other Accounts
            val inactiveAccounts = accounts.filter { it.accountId != activeAccount?.accountId }
            if (inactiveAccounts.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(inactiveAccounts) { account ->
                        AccountItem(
                            account = account,
                            isActive = false,
                            onClick = {
                                onAccountSelected(account)
                                onDismiss()
                            }
                        )
                    }
                }
                Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))
            }

            // Actions
            ActionItem(
                text = "Add another account",
                icon = Icons.Default.Add,
                onClick = {
                    onAddAccount()
                    onDismiss()
                }
            )

            if (activeAccount != null) {
                ActionItem(
                    text = "Sign out",
                    icon = Icons.Default.Logout,
                    onClick = {
                        onSignOut(activeAccount)
                        onDismiss()
                    }
                )

                ActionItem(
                    text = "Remove account & delete data",
                    icon = Icons.Default.Delete,
                    color = Color(0xFFF44336),
                    onClick = { showRemoveDialog = true }
                )

                ActionItem(
                    text = "Wipe all local data",
                    icon = Icons.Default.Delete,
                    color = Color(0xFFF44336),
                    onClick = { showWipeDialog = true }
                )
            }
        }
    }

    if (showRemoveDialog && activeAccount != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Account") },
            text = { Text("Are you sure you want to completely remove this account and delete its associated data? You will be signed out.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemoveAccount(activeAccount)
                    onDismiss()
                }) {
                    Text("Remove", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE0E0E0)
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Wipe Local Data") },
            text = { Text("Are you sure you want to completely wipe all cached local application data for this app?") },
            confirmButton = {
                TextButton(onClick = {
                    showWipeDialog = false
                    onWipeData()
                    onDismiss()
                }) {
                    Text("Wipe", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE0E0E0)
        )
    }
}

@Composable
fun AccountItem(account: AccountInfo, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture
        AsyncImage(
            model = account.photoUrl.ifBlank { "https://ui-avatars.com/api/?name=${account.displayName.replace(" ", "+")}&background=${account.colorHash.removePrefix("#")}&color=fff" },
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                color = Color.White,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = account.email,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ActionItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
