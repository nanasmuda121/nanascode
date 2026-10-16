package com.nanascode.app.feature.auth

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nanascode.app.core.model.GitHubUser
import com.nanascode.app.core.model.ScopeCheckResult
import com.nanascode.app.core.ui.components.RateLimitCard
import com.nanascode.app.core.ui.components.ScopeWarningCard
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasGreen
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRepos: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val rateLimit by viewModel.rateLimitFlow.collectAsState()

    var tokenInput by remember { mutableStateOf("") }
    var isTokenVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NanasDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = NanasTextPrimary
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🍍 NanasCode",
                            fontWeight = FontWeight.Bold,
                            color = NanasYellow,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    if (savedAccounts.isNotEmpty()) {
                        IconButton(onClick = { showAccountSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Ganti Akun",
                                tint = NanasYellow
                            )
                        }
                    }
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Panduan Token",
                            tint = NanasTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NanasDarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rate Limit Banner jika ada
            rateLimit?.let {
                RateLimitCard(rateLimit = it)
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (val state = uiState) {
                is AuthState.Authenticated -> {
                    // Tampilan Profil Sukses Login
                    UserProfileCard(
                        user = state.user,
                        scopeResult = state.scopeResult,
                        onContinue = onNavigateToRepos,
                        onLogout = { viewModel.logoutCurrentAccount() }
                    )
                }
                is AuthState.Loading -> {
                    Spacer(modifier = Modifier.height(60.dp))
                    CircularProgressIndicator(color = NanasYellow)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Memverifikasi token dengan GitHub API...",
                        color = NanasTextSecondary,
                        fontSize = 14.sp
                    )
                }
                is AuthState.Error, AuthState.Idle -> {
                    // Form Login
                    Text(
                        text = "Masuk dengan Access Token",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NanasTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan Personal Access Token (Classic atau Fine-grained) untuk mengakses repositori Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NanasTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pesan Error jika ada
                    if (state is AuthState.Error) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B1515))
                                .border(1.dp, NanasError.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = Color(0xFFFF8A80),
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Input Token
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Personal Access Token") },
                        placeholder = { Text("ghp_... atau github_pat_...") },
                        singleLine = true,
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login(tokenInput)
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = NanasYellow
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                    Icon(
                                        imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isTokenVisible) "Sembunyikan" else "Tampilkan",
                                        tint = NanasTextSecondary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NanasYellow,
                            unfocusedBorderColor = NanasDarkBorder,
                            focusedContainerColor = NanasDarkSurface,
                            unfocusedContainerColor = NanasDarkSurface,
                            focusedTextColor = NanasTextPrimary,
                            unfocusedTextColor = NanasTextPrimary,
                            focusedLabelColor = NanasYellow,
                            unfocusedLabelColor = NanasTextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tombol Tempel dari Clipboard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                if (clipboard.hasPrimaryClip() &&
                                    (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true)
                                ) {
                                    val item = clipboard.primaryClip?.getItemAt(0)
                                    val text = item?.text?.toString()?.trim()
                                    if (!text.isNullOrBlank()) {
                                        tokenInput = text
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Token berhasil ditempel dari clipboard")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NanasYellow
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(NanasDarkBorder)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tempel dari Clipboard", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { showGuideDialog = true }
                        ) {
                            Text(
                                text = "Panduan Token",
                                color = NanasYellow,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tombol Masuk
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(tokenInput)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = tokenInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NanasYellow,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Masuk ke GitHub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Info Keamanan Token
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NanasDarkSurface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NanasGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Token disimpan dengan enkripsi Android Keystore (AES-256 GCM) dan tidak pernah masuk ke log atau analytics.",
                            fontSize = 12.sp,
                            color = NanasTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Dialog Panduan Token
    if (showGuideDialog) {
        TokenGuideDialog(onDismiss = { showGuideDialog = false })
    }

    // Sheet Ganti Akun
    if (showAccountSheet) {
        AccountSwitcherSheet(
            accounts = savedAccounts,
            currentAccount = if (uiState is AuthState.Authenticated) (uiState as AuthState.Authenticated).user.login else null,
            onSelectAccount = {
                viewModel.switchAccount(it)
                showAccountSheet = false
            },
            onAddNewAccount = {
                viewModel.resetState()
                tokenInput = ""
                showAccountSheet = false
            },
            onLogoutAll = {
                viewModel.logoutAll()
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }
}

@Composable
fun UserProfileCard(
    user: GitHubUser,
    scopeResult: ScopeCheckResult?,
    onContinue: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NanasDarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NanasDarkBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.login,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, NanasYellow, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = NanasYellow
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user.name ?: user.login,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NanasTextPrimary
                    )
                    Text(
                        text = "@${user.login}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NanasYellow
                    )
                    val email = user.email
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = NanasTextSecondary
                        )
                    }
                }
            }

            val bio = user.bio
            if (!bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = NanasTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = NanasDarkBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Statistik Repositori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(label = "Repo Publik", value = "${user.publicRepos}")
                StatColumn(label = "Repo Private", value = "${user.totalPrivateRepos}")
                StatColumn(label = "Followers", value = "${user.followers}")
            }

            // Peringatan Scope bila ada
            scopeResult?.let {
                Spacer(modifier = Modifier.height(14.dp))
                ScopeWarningCard(scopeResult = it)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NanasYellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Lanjut ke Repositori",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NanasError
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NanasDarkBorder)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Keluar (Logout)")
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = NanasYellow
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = NanasTextSecondary
        )
    }
}

@Composable
fun TokenGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Panduan Personal Access Token",
                fontWeight = FontWeight.Bold,
                color = NanasYellow,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Buka github.com -> Settings -> Developer settings -> Personal access tokens.",
                    color = NanasTextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "A. Token Classic (Disarankan):",
                    fontWeight = FontWeight.Bold,
                    color = NanasYellow,
                    fontSize = 13.sp
                )
                Text(
                    text = "Centang scope berikut:\n• repo (Wajib untuk repo private & commit)\n• read:user (Membaca profil & email)\n• workflow (Wajib untuk tab GitHub Actions)",
                    color = NanasTextSecondary,
                    fontSize = 12.sp
                )

                Text(
                    text = "B. Token Fine-grained:",
                    fontWeight = FontWeight.Bold,
                    color = NanasYellow,
                    fontSize = 13.sp
                )
                Text(
                    text = "Beri repository permissions:\n• Contents: Read and write\n• Metadata: Read-only\n• Workflows: Read and write (untuk GitHub Actions)\n• Administration: Read and write (opsional buat repo)",
                    color = NanasTextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Mengerti", color = NanasYellow)
            }
        },
        containerColor = NanasDarkSurface,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<String>,
    currentAccount: String?,
    onSelectAccount: (String) -> Unit,
    onAddNewAccount: () -> Unit,
    onLogoutAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = NanasDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Kelola Akun GitHub",
                fontWeight = FontWeight.Bold,
                color = NanasYellow,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            accounts.forEach { acc ->
                val isSelected = acc == currentAccount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NanasDarkSurfaceLight else Color.Transparent)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "@$acc",
                        color = if (isSelected) NanasYellow else NanasTextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Aktif",
                            tint = NanasYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        TextButton(onClick = { onSelectAccount(acc) }) {
                            Text("Ganti", color = NanasYellow)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddNewAccount,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
            ) {
                Text("+ Tambah Akun Baru")
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogoutAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NanasError)
            ) {
                Text("Logout Semua Akun")
            }
        }
    }
}
