package com.nanaseditor.nanas.core.network

import com.nanaseditor.nanas.core.common.SecureLogger
import okhttp3.Interceptor
import okhttp3.Response

class RedactingLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sanitizedUrl = request.url.toString()
        SecureLogger.d("OkHttp", "--> ${request.method} $sanitizedUrl")

        val t1 = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            SecureLogger.e("OkHttp", "<-- HTTP FAILED: ${e.message}")
            throw e
        }
        val t2 = System.nanoTime()

        val tookMs = (t2 - t1) / 1e6
        SecureLogger.d("OkHttp", "<-- ${response.code} ${response.message} $sanitizedUrl (${tookMs.toLong()}ms)")

        return response
    }
}
