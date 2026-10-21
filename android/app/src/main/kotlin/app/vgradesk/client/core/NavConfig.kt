package app.vgradesk.client.core

/** Разделы, которые реально реализованы. Нереализованные (Видео, Клипы, Звонки…) сюда не добавляются. */
enum class TabId(val id: String) {
    Chats("chats"),
    Feed("feed"),
    Music("music"),
    Profile("profile"),
}

/** Настройка нижней панели: какие вкладки видны и в каком порядке. Чистая логика, проверяется тестами. */
object NavConfig {
    val DEFAULT: List<TabId> = listOf(TabId.Chats, TabId.Feed, TabId.Music, TabId.Profile)

    fun parse(raw: String?): List<TabId> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = raw.split(',')
            .mapNotNull { part -> TabId.entries.firstOrNull { it.id == part.trim() } }
            .distinct()
        return parsed.ifEmpty { DEFAULT }
    }

    fun serialize(tabs: List<TabId>): String = tabs.joinToString(",") { it.id }

    fun move(tabs: List<TabId>, from: Int, delta: Int): List<TabId> {
        val to = from + delta
        if (from !in tabs.indices || to !in tabs.indices) return tabs
        val result = tabs.toMutableList()
        val item = result.removeAt(from)
        result.add(to, item)
        return result
    }

    /** Убрать вкладку нельзя, если она последняя. Возвращённая вкладка встаёт в конец. */
    fun toggle(tabs: List<TabId>, tab: TabId): List<TabId> =
        if (tab in tabs) {
            if (tabs.size <= 1) tabs else tabs - tab
        } else {
            tabs + tab
        }

    fun hidden(tabs: List<TabId>): List<TabId> = TabId.entries.filter { it !in tabs }
}
