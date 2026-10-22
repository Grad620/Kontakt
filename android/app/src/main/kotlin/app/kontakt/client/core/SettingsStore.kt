package app.kontakt.client.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.kontakt.client.rec.FilterConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.vgDataStore by preferencesDataStore(name = "vg_settings")

/** Настройки Kontakt (не секреты). Токен хранится отдельно, в Android Keystore. */
class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent")
    private val effectsKey = stringPreferencesKey("effects")
    private val tabsKey = stringPreferencesKey("tabs")
    private val styleKey = stringPreferencesKey("interface_style")
    private val hideAdsKey = booleanPreferencesKey("filter_ads")
    private val hideScamKey = booleanPreferencesKey("filter_scam")
    private val hideGoodsKey = booleanPreferencesKey("filter_goods")

    val themeMode: Flow<ThemeMode> = context.vgDataStore.data
        .map { prefs -> ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.System }
        .distinctUntilChanged()

    val accent: Flow<AccentId> = context.vgDataStore.data
        .map { prefs ->
            when (prefs[accentKey]) {
                AccentId.White.name -> AccentId.White
                AccentId.Green.name -> AccentId.Green
                AccentId.Sky.name, AccentId.Blue.name, AccentId.DarkGreen.name, AccentId.Birch.name,
                AccentId.Violet.name, AccentId.Lilac.name -> AccentId.Sky
                else -> AccentId.Sky
            }
        }
        .distinctUntilChanged()

    val effects: Flow<EffectsMode> = context.vgDataStore.data
        .map { prefs -> EffectsMode.entries.firstOrNull { it.name == prefs[effectsKey] } ?: EffectsMode.Full }
        .distinctUntilChanged()

    val style: Flow<KontaktStyleId> = context.vgDataStore.data
        .map { prefs -> KontaktStyleId.entries.firstOrNull { it.name == prefs[styleKey] } ?: KontaktStyleId.Kontakt }
        .distinctUntilChanged()

    val tabs: Flow<List<TabId>> = context.vgDataStore.data
        .map { prefs -> NavConfig.parse(prefs[tabsKey]) }
        .distinctUntilChanged()

    val appearance: Flow<AppearanceState> =
        combine(themeMode, accent, effects, tabs, style) { theme, accent, effects, tabs, style ->
            AppearanceState(theme, accent, effects, tabs, style)
        }

    val filterConfig: Flow<FilterConfig> = context.vgDataStore.data
        .map { prefs ->
            FilterConfig(
                hideAds = prefs[hideAdsKey] ?: true,
                hideScam = prefs[hideScamKey] ?: true,
                hideRestrictedGoods = prefs[hideGoodsKey] ?: true,
            )
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.vgDataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setAccent(accent: AccentId) {
        context.vgDataStore.edit { it[accentKey] = accent.name }
    }

    suspend fun setEffects(mode: EffectsMode) {
        context.vgDataStore.edit { it[effectsKey] = mode.name }
    }

    suspend fun setStyle(style: KontaktStyleId) {
        context.vgDataStore.edit { it[styleKey] = style.name }
    }

    suspend fun setTabs(tabs: List<TabId>) {
        context.vgDataStore.edit { it[tabsKey] = NavConfig.serialize(tabs) }
    }

    suspend fun resetTabs() {
        context.vgDataStore.edit { it.remove(tabsKey) }
    }

    suspend fun setFilterConfig(config: FilterConfig) {
        context.vgDataStore.edit {
            it[hideAdsKey] = config.hideAds
            it[hideScamKey] = config.hideScam
            it[hideGoodsKey] = config.hideRestrictedGoods
        }
    }
}
