package app.vgradesk.client.rec

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Локальное хранилище обучения: один JSON-файл в закрытой папке приложения, запись через временный файл. */
class RecommenderStore(private val file: File) : RecommenderStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun encode(state: StoredState): String = json.encodeToString(StoredState.serializer(), state)

    override fun load(): StoredState =
        try {
            if (file.exists()) json.decodeFromString(StoredState.serializer(), file.readText()) else StoredState()
        } catch (e: Exception) {
            StoredState()
        }

    override fun write(text: String) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(text)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun delete() {
        file.delete()
    }
}
