package com.abook.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abook.app.data.sync.DriveSyncRepository
import com.abook.app.data.sync.GoogleAuthManager
import com.abook.app.data.sync.SyncResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf(GoogleAuthManager.getLastSignedInAccount(context)) }
    var isSyncing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        account = task.result
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (account != null) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = com.abook.app.ui.theme.OrangeAccent
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("مزامنة Google Drive", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (account == null) {
                Text(
                    "سجّل الدخول لحفظ مواضع القراءة والمفضلة تلقائيًا، ومزامنتها بين أجهزتك.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { signInLauncher.launch(GoogleAuthManager.getSignInIntent(context)) }) {
                    Text("تسجيل الدخول بحساب Google")
                }
            } else {
                Text("مسجّل الدخول بحساب: ${account?.email ?: ""}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val currentAccount = account ?: return@Button
                        isSyncing = true
                        statusMessage = null
                        scope.launch {
                            val result = DriveSyncRepository(context).sync(currentAccount)
                            isSyncing = false
                            statusMessage = when (result) {
                                is SyncResult.Success -> "تمت المزامنة بنجاح ✓"
                                is SyncResult.Error -> "فشلت المزامنة: ${result.message}"
                            }
                        }
                    },
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("مزامنة الآن")
                    }
                }

                statusMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    scope.launch {
                        GoogleAuthManager.signOut(context)
                        account = null
                    }
                }) { Text("تسجيل الخروج") }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
