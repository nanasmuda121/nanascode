package com.nascode.nanas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nanascode.app.core.common.NanasForegroundService
import com.nanascode.app.core.ui.theme.NanasCodeTheme
import com.nanascode.app.core.ui.theme.NanasDarkBg
import com.nanascode.app.feature.auth.AuthViewModel
import com.nanascode.app.feature.auth.LoginScreen
import com.nanascode.app.feature.editor.EditorScreen
import com.nanascode.app.feature.editor.EditorViewModel
import com.nanascode.app.feature.repo.RepoListScreen
import com.nanascode.app.feature.repo.RepoViewModel
import com.nanascode.app.feature.workspace.WelcomeScreen
import com.nanascode.app.feature.workspace.WorkspaceListScreen
import com.nanascode.app.feature.workspace.WorkspaceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NanasCodeTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NanasDarkBg
                ) {
                    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                            Toast.makeText(this@MainActivity, "Folder dipilih via SAF: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    NasNavHost(
                        onShowToast = { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        },
                        onOpenSafFolder = {
                            openDocumentTreeLauncher.launch(null)
                        },
                        onStartForeground = { name ->
                            NanasForegroundService.start(this@MainActivity, name)
                        },
                        onStopForeground = {
                            NanasForegroundService.stop(this@MainActivity)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NasNavHost(
    onShowToast: (String) -> Unit,
    onOpenSafFolder: () -> Unit,
    onStartForeground: (String) -> Unit,
    onStopForeground: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // Layar Awal: Sambutan, Pilihan Login GitHub atau Lanjut Mode Lokal
        composable("welcome") {
            WelcomeScreen(
                onLoginWithGitHubClick = {
                    navController.navigate("auth")
                },
                onContinueOfflineClick = {
                    navController.navigate("workspaces") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // Layar Workspace Lokal & Offline-First Projects
        composable("workspaces") {
            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            WorkspaceListScreen(
                viewModel = workspaceViewModel,
                onOpenWorkspace = { workspace ->
                    onStartForeground(workspace.name)
                    val encoded = Uri.encode(workspace.rootPath)
                    navController.navigate("editor/${workspace.name}/$encoded")
                },
                onNavigateToLogin = {
                    navController.navigate("auth")
                },
                onNavigateToGitHubRepos = {
                    navController.navigate("repos")
                },
                onImportFolderSaf = onOpenSafFolder
            )
        }

        // Layar Login GitHub (Opsional)
        composable("auth") {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRepos = {
                    navController.navigate("workspaces") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Layar Manajemen Repositori GitHub & File Explorer
        composable("repos") {
            val repoViewModel: RepoViewModel = hiltViewModel()
            val workspaceViewModel: WorkspaceViewModel = hiltViewModel()
            val scope = rememberCoroutineScope()

            RepoListScreen(
                viewModel = repoViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectRepo = { repo ->
                    onShowToast("Menyiapkan workspace untuk ${repo.name}...")
                    scope.launch {
                        val result = workspaceViewModel.cloneRemoteRepository(repo)
                        result.onSuccess { ws ->
                            onStartForeground(ws.name)
                            val encoded = Uri.encode(ws.rootPath)
                            navController.navigate("editor/${ws.name}/$encoded")
                        }.onFailure { err ->
                            onShowToast("Gagal menyiapkan repo: ${err.message}")
                        }
                    }
                },
                onNavigateToActions = { repo ->
                    if (repo.isPrivate) {
                        onShowToast("Fitur Actions hanya untuk repo publik")
                    } else {
                        onShowToast("Membuka GitHub Actions untuk ${repo.fullName} (Fitur F6)")
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("auth")
                }
            )
        }

        // Layar Editor Koding Mobile (CodeMirror 6 WebView)
        composable(
            route = "editor/{workspaceName}/{rootPath}",
            arguments = listOf(
                navArgument("workspaceName") { type = NavType.StringType },
                navArgument("rootPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("workspaceName") ?: "Editor"
            val encodedPath = backStackEntry.arguments?.getString("rootPath") ?: ""
            val rootPath = Uri.decode(encodedPath)

            val editorViewModel: EditorViewModel = hiltViewModel()
            LaunchedEffect(rootPath) {
                editorViewModel.initWorkspace(rootPath, name)
            }

            EditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = {
                    onStopForeground()
                    navController.popBackStack()
                }
            )
        }
    }
}
