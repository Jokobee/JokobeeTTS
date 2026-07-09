package com.jokobee.tts.core

import java.net.HttpURLConnection
import java.net.URL

/** Default HTTP client (HttpURLConnection). Handles resume via the Range header / 206 response. */
public class HttpUrlConnectionClient(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) : HttpClient {
    override fun open(url: String, rangeStart: Long): HttpResponse {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            requestMethod = "GET"
            if (rangeStart > 0) setRequestProperty("Range", "bytes=$rangeStart-")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw ModelResolutionException("HTTP $code sur $url")
        }
        val startsAt = if (code == HttpURLConnection.HTTP_PARTIAL) rangeStart else 0L
        val len = conn.contentLengthLong   // body of this response (-1 if unknown)
        return HttpResponse(conn.inputStream, len, startsAt, onClose = { conn.disconnect() })
    }
}
