package app.vgradesk.client.net

import app.vgradesk.client.auth.AuthManager
import app.vgradesk.client.core.DiagnosticsLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Тонкий клиент VK API. Токен передаётся в теле POST-запроса (не в URL) и нигде не логируется.
 * Отмена корутины отменяет сетевой запрос (экран закрыт — запрос не нужен).
 *
 * Бережное отношение к лимитам VK: запросы идут по одному с паузой не меньше [minIntervalMs]
 * (у пользовательских токенов лимит — около 3 запросов в секунду). При сетевом сбое чтение повторяется
 * не более [DEFAULT_RETRIES] раз с нарастающей паузой. Ошибки лимита и капчи не повторяются и не обходятся.
 */
class VkApi(
    private val auth: AuthManager,
    private val diagnostics: DiagnosticsLog,
    private val client: OkHttpClient = defaultClient(),
    private val minIntervalMs: Long = MIN_INTERVAL_MS,
) {
    private val gate = Mutex()
    private var lastCallNanos = 0L

    suspend fun call(
        method: String,
        params: Map<String, String> = emptyMap(),
        retries: Int = DEFAULT_RETRIES,
    ): JsonElement {
        var attempt = 0
        while (true) {
            try {
                return callOnce(method, params)
            } catch (e: VkError.Network) {
                if (attempt >= retries) {
                    diagnostics.record(method, e)
                    throw e
                }
                delay(BACKOFF_MS[minOf(attempt, BACKOFF_MS.size - 1)])
                attempt++
            } catch (e: VkError) {
                diagnostics.record(method, e)
                throw e
            }
        }
    }

    private suspend fun throttle() {
        gate.withLock {
            if (lastCallNanos != 0L) {
                val sinceLastMs = (System.nanoTime() - lastCallNanos) / 1_000_000L
                if (sinceLastMs < minIntervalMs) delay(minIntervalMs - sinceLastMs)
            }
            lastCallNanos = System.nanoTime()
        }
    }

    private suspend fun callOnce(method: String, params: Map<String, String>): JsonElement {
        val token = auth.token()
        if (token == null) {
            auth.onSessionExpired()
            throw VkError.SessionExpired()
        }
        throttle()
        val form = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
            add("v", API_VERSION)
            add("lang", "ru")
            add("access_token", token)
        }.build()
        val request = Request.Builder().url(BASE_URL + method).post(form).build()

        val text = try {
            client.newCall(request).awaitBody()
        } catch (e: IOException) {
            throw VkError.Network(e)
        }

        return try {
            VkResponseParser.parse(text)
        } catch (e: VkError.SessionExpired) {
            auth.onSessionExpired()
            throw e
        }
    }

    private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.use { it.body?.string().orEmpty() }
                    cont.resume(body)
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }
        })
    }

    companion object {
        const val BASE_URL = "https://api.vk.ru/method/"
        const val API_VERSION = "5.199"
        const val MIN_INTERVAL_MS = 340L
        const val DEFAULT_RETRIES = 2
        private val BACKOFF_MS = longArrayOf(600L, 1_800L)

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
