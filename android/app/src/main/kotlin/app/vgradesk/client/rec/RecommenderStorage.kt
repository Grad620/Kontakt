package app.vgradesk.client.rec

import kotlinx.serialization.Serializable

@Serializable
data class StoredState(
    val profile: InterestProfile = InterestProfile(),
    val seen: List<String> = emptyList(),
    val liked: List<String> = emptyList(),
    /** Источники, скрытые пользователем: идентификатор автора → название на момент скрытия. */
    val hidden: Map<Long, String> = emptyMap(),
)

/** Где хранится обучение. В приложении — файл, в тестах — память. */
interface RecommenderStorage {
    fun load(): StoredState
    fun encode(state: StoredState): String
    fun write(text: String)
    fun delete()
}
