package com.nanaseditor.nanas.feature.auth

import com.nanaseditor.nanas.core.common.DispatcherProvider
import com.nanaseditor.nanas.core.database.UserDao
import com.nanaseditor.nanas.core.model.GitHubScopeParser
import com.nanaseditor.nanas.core.model.GitHubToken
import com.nanaseditor.nanas.core.model.GitHubUser
import com.nanaseditor.nanas.core.model.NetworkResult
import com.nanaseditor.nanas.core.model.RateLimitInfo
import com.nanaseditor.nanas.core.network.GitHubRepository
import com.nanaseditor.nanas.core.security.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private lateinit var gitHubRepository: GitHubRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var userDao: UserDao
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        gitHubRepository = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        userDao = mockk(relaxed = true)

        every { gitHubRepository.rateLimitFlow } returns MutableStateFlow(null)
        every { gitHubRepository.lastScopeResultFlow } returns MutableStateFlow(null)
        every { tokenManager.getAllAccounts() } returns emptyList()
        every { tokenManager.getActiveToken() } returns null
        every { tokenManager.getActiveUsername() } returns null

        viewModel = AuthViewModel(
            gitHubRepository = gitHubRepository,
            tokenManager = tokenManager,
            userDao = userDao,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with blank token sets error state immediately`() = runTest {
        viewModel.login("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Token tidak boleh kosong", (state as AuthState.Error).message)
    }

    @Test
    fun `login with valid token updates state to Authenticated and persists token`() = runTest {
        val testUser = GitHubUser(
            id = 12345,
            login = "nanasdev",
            name = "Nanas Developer",
            publicRepos = 10,
            totalPrivateRepos = 5
        )
        val testToken = GitHubToken("ghp_valid1234567890abcdef")
        val scopeResult = GitHubScopeParser.parseHeader("repo, read:user, workflow", testToken)

        coEvery { gitHubRepository.validateToken(testToken) } returns NetworkResult.Success(
            Pair(testUser, scopeResult)
        )

        viewModel.login("ghp_valid1234567890abcdef")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("nanasdev", (state as AuthState.Authenticated).user.login)

        // Verify token saved to encrypted token manager
        coVerify { tokenManager.saveToken("nanasdev", testToken) }
    }

    @Test
    fun `login with 401 returns unauthorized error message`() = runTest {
        val testToken = GitHubToken("ghp_invalidtoken123456")
        coEvery { gitHubRepository.validateToken(testToken) } returns NetworkResult.Unauthorized()

        viewModel.login("ghp_invalidtoken123456")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Token tidak valid atau telah kedaluwarsa", (state as AuthState.Error).message)
    }

    @Test
    fun `login with rate limit error reflects rate limit status`() = runTest {
        val testToken = GitHubToken("ghp_ratelimitedtoken123")
        val rateLimit = RateLimitInfo(5000, 0, 1729080000L)
        coEvery { gitHubRepository.validateToken(testToken) } returns NetworkResult.Error(
            code = 403,
            message = "Rate limit reached",
            isRateLimit = true,
            rateLimitInfo = rateLimit
        )

        viewModel.login("ghp_ratelimitedtoken123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthState.Error)
        val errorState = state as AuthState.Error
        assertTrue(errorState.isRateLimit)
        assertEquals(1729080000L, errorState.resetSeconds)
    }
}
