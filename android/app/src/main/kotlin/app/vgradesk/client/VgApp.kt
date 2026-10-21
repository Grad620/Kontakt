package app.vgradesk.client

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.vk.id.VKID

class VgApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Без идентификатора приложения VK ID SDK не инициализируем: экран входа объяснит, что нужно настроить.
        if (BuildConfig.VK_CLIENT_ID.isNotBlank()) {
            VKID.init(this)
        }
    }

    /** Кеш картинок с ограничениями: 20% памяти приложения и 64 МБ на диске. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
