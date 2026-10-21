package app.kontakt.client

import android.content.Context
import app.kontakt.client.auth.AuthManager
import app.kontakt.client.auth.TokenStore
import app.kontakt.client.core.DiagnosticsLog
import app.kontakt.client.core.NetworkMonitor
import app.kontakt.client.core.SettingsStore
import app.kontakt.client.data.ChatsRepository
import app.kontakt.client.data.FeedRepository
import app.kontakt.client.data.FilterStats
import app.kontakt.client.data.ForYouRepository
import app.kontakt.client.data.ProfileRepository
import app.kontakt.client.data.VkChunkLoader
import app.kontakt.client.net.VkApi
import app.kontakt.client.rec.FilterConfig
import app.kontakt.client.rec.Recommender
import app.kontakt.client.rec.RecommenderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Простая ручная сборка зависимостей: без DI-фреймворка, чтобы не раздувать запуск и сборку. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Живёт столько же, сколько процесс: сохранение обучения, наблюдение за настройками. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val diagnostics = DiagnosticsLog()
    val network = NetworkMonitor(appContext)

    val tokenStore = TokenStore(appContext)
    val settings = SettingsStore(appContext)
    val auth = AuthManager(tokenStore, appContext)
    val api = VkApi(auth, diagnostics)

    val filterStats = FilterStats()

    /** Актуальные настройки фильтра; пока DataStore читается, действуют безопасные значения по умолчанию. */
    val filterConfig: StateFlow<FilterConfig> =
        settings.filterConfig.stateIn(appScope, SharingStarted.Eagerly, FilterConfig())

    /** Обучение хранится только на устройстве, в закрытой папке приложения. */
    val recommender = Recommender(RecommenderStore(File(appContext.filesDir, "recommender.json")))

    private val chunkLoader = VkChunkLoader(api) { filterConfig.value }

    val profileRepo = ProfileRepository(api)
    val chatsRepo = ChatsRepository(api)
    val feedRepo = FeedRepository(chunkLoader, recommender, filterStats)
    val forYouRepo = ForYouRepository(chunkLoader, recommender, filterStats)

    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        // Небольшой файл (десятки килобайт) читается один раз при запуске, до показа интерфейса.
        recommender.load()
        recommender.onChanged = { saveRequests.trySend(Unit) }
        appScope.launch(Dispatchers.IO) {
            for (request in saveRequests) {
                delay(SAVE_DELAY_MS)
                recommender.flush()
            }
        }
    }

    /** Сохранить обучение немедленно (например, когда приложение уходит в фон). */
    fun persistNow() {
        appScope.launch(Dispatchers.IO) { recommender.flush() }
    }

    /**
     * Выход из аккаунта: токен удаляется, а обучение рекомендаций, счётчики и журнал очищаются,
     * чтобы данные разных аккаунтов не смешивались.
     */
    fun signOutAndWipe() {
        auth.signOut()
        filterStats.reset()
        diagnostics.clear()
        appScope.launch(Dispatchers.IO) { recommender.wipeStorage() }
    }

    private companion object {
        const val SAVE_DELAY_MS = 1_500L
    }
}
