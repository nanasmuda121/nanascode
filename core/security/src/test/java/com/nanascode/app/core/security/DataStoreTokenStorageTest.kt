package com.nanascode.app.core.security

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nanascode.app.core.common.DefaultDispatcherProvider
import com.nanascode.app.core.model.GitHubToken
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreTokenStorageTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher + Job())
    private lateinit var tempFile: File
    private lateinit var storage: DataStoreTokenStorage

    @Before
    fun setup() {
        tempFile = File.createTempFile("test_tokens_", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )

        val mockContext = mockk<Context>(relaxed = true)
        val cipher = AndroidKeystoreCipher()
        val dispatchers = DefaultDispatcherProvider()

        storage = DataStoreTokenStorage(
            context = mockContext,
            cipher = cipher,
            dispatcherProvider = dispatchers,
            customDataStore = testDataStore
        )
    }

    @After
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun `initial state has no active token and empty account list`() {
        assertNull(storage.getActiveToken())
        assertNull(storage.getActiveUsername())
        assertTrue(storage.getAllAccounts().isEmpty())
        assertNull(storage.activeTokenFlow.value)
    }

    @Test
    fun `saveToken persists encrypted token and sets active user`() {
        val token = GitHubToken("ghp_testToken1234567890abcdef")
        storage.saveToken("octocat", token)

        assertEquals("octocat", storage.getActiveUsername())
        assertNotNull(storage.getActiveToken())
        assertEquals(token.rawValue, storage.getActiveToken()?.rawValue)
        assertEquals(listOf("octocat"), storage.getAllAccounts())
        assertEquals(token.rawValue, storage.activeTokenFlow.value?.rawValue)
    }

    @Test
    fun `multi-account switching works correctly`() {
        val token1 = GitHubToken("ghp_userOneToken111111111111111")
        val token2 = GitHubToken("ghp_userTwoToken222222222222222")

        storage.saveToken("user_one", token1)
        storage.saveToken("user_two", token2)

        assertEquals(listOf("user_one", "user_two").sorted(), storage.getAllAccounts().sorted())
        assertEquals("user_two", storage.getActiveUsername())
        assertEquals(token2.rawValue, storage.getActiveToken()?.rawValue)

        // Switch to user_one
        val switched = storage.setActiveAccount("user_one")
        assertTrue(switched)
        assertEquals("user_one", storage.getActiveUsername())
        assertEquals(token1.rawValue, storage.getActiveToken()?.rawValue)

        // Switch to non-existent user
        val nonExistent = storage.setActiveAccount("ghost_user")
        assertFalse(nonExistent)
    }

    @Test
    fun `removeAccount removes account and fallbacks to remaining account or null`() {
        val tokenA = GitHubToken("ghp_tokenA")
        val tokenB = GitHubToken("ghp_tokenB")

        storage.saveToken("alice", tokenA)
        storage.saveToken("bob", tokenB)

        // Remove active user (bob)
        storage.removeAccount("bob")
        assertEquals(listOf("alice"), storage.getAllAccounts())
        assertEquals("alice", storage.getActiveUsername())
        assertEquals(tokenA.rawValue, storage.getActiveToken()?.rawValue)

        // Remove alice
        storage.removeAccount("alice")
        assertTrue(storage.getAllAccounts().isEmpty())
        assertNull(storage.getActiveUsername())
        assertNull(storage.getActiveToken())
        assertNull(storage.activeTokenFlow.value)
    }

    @Test
    fun `clearAll removes all accounts and resets active token`() {
        storage.saveToken("user1", GitHubToken("ghp_111"))
        storage.saveToken("user2", GitHubToken("ghp_222"))

        storage.clearAll()
        assertTrue(storage.getAllAccounts().isEmpty())
        assertNull(storage.getActiveUsername())
        assertNull(storage.getActiveToken())
        assertNull(storage.activeTokenFlow.value)
    }
}
