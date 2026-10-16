package com.nanascode.app.core.network

import com.nanascode.app.core.model.GitHubToken
import com.nanascode.app.core.security.TokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var mockTokenManager: TokenManager
    private lateinit var networkStateHolder: NetworkStateHolder
    private lateinit var interceptor: AuthInterceptor
    private lateinit var mockChain: Interceptor.Chain

    @Before
    fun setup() {
        mockTokenManager = mockk(relaxed = true)
        networkStateHolder = NetworkStateHolder()
        interceptor = AuthInterceptor(mockTokenManager, networkStateHolder)
        mockChain = mockk()
    }

    @Test
    fun `guest mode without token sends request without Authorization header`() {
        every { mockTokenManager.getActiveToken() } returns null

        val originalRequest = Request.Builder()
            .url("https://api.github.com/repos/octocat/Hello-World")
            .build()

        val capturedRequest = slot<Request>()
        every { mockChain.request() } returns originalRequest
        every { mockChain.proceed(capture(capturedRequest)) } returns Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()

        interceptor.intercept(mockChain)

        val sentRequest = capturedRequest.captured
        assertNull("Dalam mode tamu (tanpa login), header Authorization tidak boleh dikirim", sentRequest.header("Authorization"))
        assertEquals("application/vnd.github+json", sentRequest.header("Accept"))
        assertEquals("2022-11-28", sentRequest.header("X-GitHub-Api-Version"))
    }

    @Test
    fun `logged in user with token sends Bearer token header`() {
        val testToken = GitHubToken("ghp_validActiveTokenSecret123")
        every { mockTokenManager.getActiveToken() } returns testToken

        val originalRequest = Request.Builder()
            .url("https://api.github.com/user")
            .build()

        val capturedRequest = slot<Request>()
        every { mockChain.request() } returns originalRequest
        every { mockChain.proceed(capture(capturedRequest)) } returns Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()

        interceptor.intercept(mockChain)

        val sentRequest = capturedRequest.captured
        assertEquals("Bearer ghp_validActiveTokenSecret123", sentRequest.header("Authorization"))
    }
}
