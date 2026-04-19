package com.pranay.jobtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.pranay.jobtracker.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val privacyPolicyLink = stringResource(R.string.privacy_policy_link)
    val termsOfServiceLink = stringResource(R.string.terms_of_service_link)
    var showRemoveDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
    var showWipeDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
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
                text = stringResource(R.string.add_account),
                icon = Icons.Default.Add,
                onClick = {
                    onAddAccount()
                    onDismiss()
                }
            )

            if (activeAccount != null) {
                ActionItem(
                    text = stringResource(R.string.sign_out),
                    icon = Icons.Default.Logout,
                    onClick = {
                        onSignOut(activeAccount)
                        onDismiss()
                    }
                )

                ActionItem(
                    text = stringResource(R.string.wipe_data_option),
                    icon = Icons.Default.Delete,
                    color = Color(0xFFF44336),
                    onClick = { showWipeDialog = true }
                )

                ActionItem(
                    text = stringResource(R.string.remove_account_option),
                    icon = Icons.Default.Delete,
                    color = Color(0xFFF44336),
                    onClick = { showRemoveDialog = true }
                )

                ActionItem(
                    text = stringResource(R.string.privacy_policy_title),
                    icon = Icons.Default.PrivacyTip,
                    color = Color.Gray,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyLink))
                        context.startActivity(intent)
                        onDismiss()
                    }
                )

                ActionItem(
                    text = stringResource(R.string.terms_of_service_title),
                    icon = Icons.Default.Description,
                    color = Color.Gray,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(termsOfServiceLink))
                        context.startActivity(intent)
                        onDismiss()
                    }
                )
            }
        }
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text(stringResource(R.string.wipe_data_title)) },
            text = { Text(stringResource(R.string.wipe_data_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showWipeDialog = false
                    onWipeData()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.action_wipe), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE0E0E0)
        )
    }

    if (showRemoveDialog && activeAccount != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.remove_account_title)) },
            text = { Text(stringResource(R.string.remove_account_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemoveAccount(activeAccount)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.action_remove), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = Color.White)
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
