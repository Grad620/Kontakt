package app.kontakt.client.core

enum class ThemeMode { System, Light, Dark }

/** Восемь акцентных цветов. Сами цвета — в ui/theme (здесь только то, что сохраняется). */
enum class AccentId { White, Sky, Blue, Green, DarkGreen, Birch, Violet, Lilac }

/**
 * Интенсивность визуальных эффектов. «Облегчённый» режим: без теней и полупрозрачности панели,
 * без анимаций переходов — для слабых устройств и экономии энергии.
 */
enum class EffectsMode { Full, Light }

/** Выбираемый пользователем визуальный стиль интерфейса. */
enum class KontaktStyleId {
    Kontakt, VkClassic;

    companion object {
        /** Неизвестное или пустое сохранённое значение безопасно превращается в стиль Kontakt. */
        fun parse(raw: String?): KontaktStyleId = entries.firstOrNull { it.name == raw } ?: Kontakt
    }
}

/** Всё, что определяет внешний вид и состав вкладок. Приложение ждёт первого значения, чтобы не мигать. */
data class AppearanceState(
    val theme: ThemeMode,
    val accent: AccentId,
    val effects: EffectsMode,
    val tabs: List<TabId>,
    val style: KontaktStyleId = KontaktStyleId.Kontakt,
)
