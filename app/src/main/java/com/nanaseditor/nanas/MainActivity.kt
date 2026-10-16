package com.nanaseditor.nanas

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nanaseditor.nanas.core.ui.theme.NanasCodeTheme
import com.nanaseditor.nanas.core.ui.theme.NanasDarkBg
import com.nanaseditor.nanas.feature.auth.AuthViewModel
import com.nanaseditor.nanas.feature.auth.LoginScreen
import com.nanaseditor.nanas.feature.repo.RepoListScreen
import com.nanaseditor.nanas.feature.repo.RepoViewModel
import dagger.hilt.android.AndroidEntryPoint

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
                    NanasNavHost(
                        onShowToast = { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NanasNavHost(
    onShowToast: (String) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRepos = {
                    navController.navigate("repos")
                }
            )
        }

        composable("repos") {
            val repoViewModel: RepoViewModel = hiltViewModel()
            RepoListScreen(
                viewModel = repoViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectRepo = { repo ->
                    onShowToast("Membuka editor untuk ${repo.fullName} (Milestone 2)")
                },
                onNavigateToActions = { repo ->
                    if (repo.isPrivate) {
                        onShowToast("Fitur Actions hanya untuk repo publik")
                    } else {
                        onShowToast("Membuka GitHub Actions untuk ${repo.fullName} (Fitur F6)")
                    }
                }
            )
        }
    }
}
