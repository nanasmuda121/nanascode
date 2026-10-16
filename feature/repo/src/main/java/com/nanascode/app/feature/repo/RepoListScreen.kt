package com.nanascode.app.feature.repo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanascode.app.core.model.GitHubRepo
import com.nanascode.app.core.model.RepoFilterType
import com.nanascode.app.core.model.RepoSortType
import com.nanascode.app.core.ui.components.RateLimitCard
import com.nanascode.app.core.ui.components.RepoItemCard
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.core.ui.theme.NanasDarkBorder
import com.nanascode.app.core.ui.theme.NanasDarkSurface
import com.nanascode.app.core.ui.theme.NanasDarkSurfaceLight
import com.nanascode.app.core.ui.theme.NanasError
import com.nanascode.app.core.ui.theme.NanasGreen
import com.nanascode.app.core.ui.theme.NanasTextPrimary
import com.nanascode.app.core.ui.theme.NanasTextSecondary
import com.nanascode.app.core.ui.theme.NanasYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    viewModel: RepoViewModel,
    onNavigateBack: () -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit,
    onNavigateToActions: (GitHubRepo) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val rateLimit by viewModel.rateLimitFlow.collectAsState()
    val activeToken by viewModel.activeToken.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isCreateSuccess) {
        if (uiState.isCreateSuccess) {
            snackbarHostState.showSnackbar("Repositori baru berhasil dibuat!")
            viewModel.clearCreateSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NanasDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Cari repositori...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = NanasTextSecondary)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = NanasTextPrimary,
                                unfocusedTextColor = NanasTextPrimary
                            )
                        )
                    } else {
                        Text(
                            text = "Repositori GitHub",
                            fontWeight = FontWeight.Bold,
                            color = NanasYellow,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = NanasTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = if (isSearchActive) NanasYellow else NanasTextPrimary
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Urutkan", tint = NanasTextPrimary)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(NanasDarkSurface)
                        ) {
                            RepoSortType.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.labelId,
                                            color = if (uiState.activeSort == sort) NanasYellow else NanasTextPrimary,
                                            fontWeight = if (uiState.activeSort == sort) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSort(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = viewModel::refreshRepos) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = NanasTextPrimary)
                    }
                    if (activeToken != null) {
                        IconButton(onClick = { showLogoutConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Keluar dari GitHub",
                                tint = NanasError
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NanasDarkSurface)
            )
        },
        floatingActionButton = {
            if (activeToken != null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = NanasYellow,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Buat Repositori Baru")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Baris Horisontal (Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NanasDarkSurfaceLight)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepoFilterType.values().forEach { filter ->
                    val isSelected = uiState.activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) NanasYellow else NanasDarkSurface)
                            .border(1.dp, if (isSelected) NanasYellow else NanasDarkBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter.labelId,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else NanasTextPrimary
                        )
                    }
                }
            }

            // Indikator Rate Limit & Error
            rateLimit?.let {
                RateLimitCard(
                    rateLimit = it,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Konten Daftar Repo
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeToken == null) {
                    // Tampilan jika user belum login ke GitHub
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NanasDarkSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = NanasYellow,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Mode Lokal / Tamu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = NanasTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Anda sedang dalam mode lokal. Masuk dengan GitHub Personal Access Token untuk melihat, mencari, dan mengelola repositori remote Anda.",
                                    fontSize = 13.sp,
                                    color = NanasTextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onNavigateToLogin,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NanasYellow, contentColor = Color.Black)
                                ) {
                                    Text(text = "Masuk dengan GitHub", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (uiState.isLoading && uiState.repos.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = NanasYellow
                    )
                } else if (uiState.filteredRepos.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "Tidak ada repositori yang cocok." else "Belum ada repositori.",
                            color = NanasTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = viewModel::refreshRepos) {
                            Text("Coba Muat Ulang", color = NanasYellow)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredRepos, key = { it.id }) { repo ->
                            RepoItemCard(
                                repo = repo,
                                onClick = { onSelectRepo(repo) },
                                onActionsClick = { onNavigateToActions(repo) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRepoDialog(
            isLoading = uiState.isCreatingRepo,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, isPrivate, autoInit, gitignore, license ->
                viewModel.createRepository(name, desc, isPrivate, autoInit, gitignore, license)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Konfirmasi Logout
    if (showLogoutConfirm) {
        val user = viewModel.getActiveUsername() ?: "GitHub"
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    text = "Keluar dari Akun GitHub?",
                    fontWeight = FontWeight.Bold,
                    color = NanasTextPrimary
                )
            },
            text = {
                Text(
                    text = "Anda akan keluar dari akun @$user. Anda tetap dapat melanjutkan bekerja dalam mode offline/lokal.",
                    color = NanasTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NanasError, contentColor = Color.White)
                ) {
                    Text(text = "Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = "Batal", color = NanasTextSecondary)
                }
            },
            containerColor = NanasDarkSurface
        )
    }
}
