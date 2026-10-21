package app.kontakt.client

import android.app.Application
import android.os.Process
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.vk.id.VKID
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class KontaktApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashRecorder()
        container = AppContainer(this)
        // Ошибка инициализации VK ID не должна закрывать всё приложение.
        if (BuildConfig.VK_ID_CONFIGURED) {
            runCatching { VKID.init(this) }
                .onFailure { error ->
                    runCatching {
                        File(filesDir, CRASH_REPORT).writeText(
                            "VKID_INIT_ERROR\n" + error.stackTraceToString().take(MAX_REPORT_CHARS),
                        )
                    }
                }
        }
    }

    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val writer = StringWriter()
                error.printStackTrace(PrintWriter(writer))
                File(filesDir, CRASH_REPORT).writeText(
                    "UNCAUGHT_EXCEPTION on thread ${thread.name}\n" +
                        writer.toString().take(MAX_REPORT_CHARS),
                )
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
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

    companion object {
        const val CRASH_REPORT = "last_crash_report.txt"
        const val MAX_REPORT_CHARS = 6000
    }
}
