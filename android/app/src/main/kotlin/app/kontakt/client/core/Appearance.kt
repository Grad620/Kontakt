package app.kontakt.client.core

enum class ThemeMode { System, Light, Dark }

/** Восемь акцентных цветов. Сами цвета — в ui/theme (здесь только то, что сохраняется). */
enum class AccentId { White, Sky, Blue, Green, DarkGreen, Birch, Violet, Lilac }

/**
 * Интенсивность визуальных эффектов. «Облегчённый» режим: без теней и полупрозрачности панели,
 * без анимаций переходов — для слабых устройств и экономии энергии.
 */
enum class EffectsMode { Full, Light }

/** Всё, что определяет внешний вид и состав вкладок. Приложение ждёт первого значения, чтобы не мигать. */
data class AppearanceState(
    val theme: ThemeMode,
    val accent: AccentId,
    val effects: EffectsMode,
    val tabs: List<TabId>,
)
