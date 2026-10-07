package com.gatelms.integration

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class TestAuthSessionInterceptor : ClientHttpRequestInterceptor {

    val cookies = mutableMapOf<String, String>()
    var autoLoginEnabled: Boolean = true

    val currentSessionId: String?
        get() = cookies["GATELMSSESSION"] ?: cookies["SESSION"] ?: cookies["JSESSIONID"]

    val currentXsrfToken: String?
        get() = cookies["XSRF-TOKEN"]

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val path = request.uri.path

        // If this is a login request, pass through and capture cookies
        if (path.endsWith("/api/v1/auth/login")) {
            val response = execution.execute(request, body)
            captureCookies(response.headers)
            return response
        }

        // Attach all captured cookies if Cookie header is not explicitly set
        if (cookies.isNotEmpty() && !request.headers.containsKey(HttpHeaders.COOKIE)) {
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            request.headers.add(HttpHeaders.COOKIE, cookieHeader)
        }

        // Attach X-XSRF-TOKEN header for mutating methods if available and not explicitly set
        val xsrf = currentXsrfToken
        if (xsrf != null && !request.headers.containsKey("X-XSRF-TOKEN")) {
            if (request.method in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
                request.headers.add("X-XSRF-TOKEN", xsrf)
            }
        }

        val response = execution.execute(request, body)
        captureCookies(response.headers)
        return response
    }

    fun captureCookies(headers: HttpHeaders) {
        val setCookieList = headers[HttpHeaders.SET_COOKIE] ?: return
        for (header in setCookieList) {
            val cookiePart = header.substringBefore(";")
            val name = cookiePart.substringBefore("=").trim()
            val value = cookiePart.substringAfter("=").trim()
            if (name.isNotBlank()) {
                cookies[name] = value
            }
        }
    }

    fun clear() {
        cookies.clear()
    }
}
