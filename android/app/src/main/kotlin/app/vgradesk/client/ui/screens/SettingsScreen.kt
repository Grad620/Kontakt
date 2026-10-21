package app.vgradesk.client.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vgradesk.client.AppContainer
import app.vgradesk.client.BuildConfig
import app.vgradesk.client.core.AccentId
import app.vgradesk.client.core.AppearanceState
import app.vgradesk.client.core.EffectsMode
import app.vgradesk.client.core.NavConfig
import app.vgradesk.client.core.TabId
import app.vgradesk.client.core.ThemeMode
import app.vgradesk.client.ui.common.formatClock
import app.vgradesk.client.ui.components.SectionCard
import app.vgradesk.client.ui.components.SubScreenHeader
import app.vgradesk.client.ui.icon
import app.vgradesk.client.ui.label
import app.vgradesk.client.ui.theme.AccentPalette
import app.vgradesk.client.ui.theme.VgIcons
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Настройки Kontakt. В списке только то, что реально работает: каждый переключатель влияет на приложение.
 * Разделы без возможностей (например, музыка VK) содержат честное пояснение вместо неработающих кнопок.
 */
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings = container.settings
    val appearance by settings.appearance.collectAsStateWithLifecycle(
        initialValue = AppearanceState(ThemeMode.System, AccentId.Blue, EffectsMode.Full, NavConfig.DEFAULT),
    )
    val filter by container.filterConfig.collectAsStateWithLifecycle()
    val counts by container.filterStats.counts.collectAsStateWithLifecycle()
    val summary by container.recommender.summary.collectAsStateWithLifecycle()
    val events by container.diagnostics.events.collectAsStateWithLifecycle()
    val online by container.network.online.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("Настройки", onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Внешний вид") {
                Text("Тема", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == appearance.theme,
                            onClick = { scope.launch { settings.setThemeMode(mode) } },
                            label = { Text(mode.themeLabel()) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Акцентный цвет", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                AccentGrid(selected = appearance.accent, onSelect = { scope.launch { settings.setAccent(it) } })
                Text(
                    "В светлой теме белый акцент рисуется графитовым: иначе он сливается с фоном.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text("Визуальные эффекты", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EffectsMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == appearance.effects,
                            onClick = { scope.launch { settings.setEffects(mode) } },
                            label = { Text(mode.effectsLabel()) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Облегчённый режим: панель навигации непрозрачная, без тени, без анимаций переходов. " +
                        "Подходит для экономии заряда и слабых устройств.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard("Навигация") {
                Text(
                    "Какие разделы показывать в нижней панели и в каком порядке. Должен остаться хотя бы один.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                val tabs = appearance.tabs
                (tabs + NavConfig.hidden(tabs)).forEach { id ->
                    TabSettingRow(
                        id = id,
                        index = tabs.indexOf(id),
                        count = tabs.size,
                        onMove = { from, delta -> scope.launch { settings.setTabs(NavConfig.move(tabs, from, delta)) } },
                        onToggle = { scope.launch { settings.setTabs(NavConfig.toggle(tabs, id)) } },
                    )
                }
                TextButton(onClick = { scope.launch { settings.resetTabs() } }) { Text("Вернуть стандартный порядок") }
            }

            SectionCard("Лента и рекомендации") {
                SwitchRow(
                    title = "Скрывать рекламу VK",
                    description = "Публикации с официальной меткой рекламы.",
                    checked = filter.hideAds,
                    onChange = { scope.launch { settings.setFilterConfig(filter.copy(hideAds = it)) } },
                )
                SwitchRow(
                    title = "Скрывать мошенничество и спам",
                    description = "Эвристика, она может ошибаться. Обычные сборы на помощь людям и животным не скрываются.",
                    checked = filter.hideScam,
                    onChange = { scope.launch { settings.setFilterConfig(filter.copy(hideScam = it)) } },
                )
                SwitchRow(
                    title = "Скрывать снюс, вейпы, казино и ставки",
                    description = "Нежелательные товары и услуги.",
                    checked = filter.hideRestrictedGoods,
                    onChange = { scope.launch { settings.setFilterConfig(filter.copy(hideRestrictedGoods = it)) } },
                )
                Text(
                    "Изменения применяются при следующем обновлении ленты. Не вся реклама имеет метку, " +
                        "поэтому часть можно пропустить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "С запуска приложения скрыто: реклама ${counts.ads}, нежелательные товары ${counts.goods}, " +
                        "мошенничество ${counts.scam}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(learningText(summary.signals, summary.topInterests), style = MaterialTheme.typography.bodyMedium)

                if (summary.hiddenSources.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Скрытые источники", style = MaterialTheme.typography.labelLarge)
                    summary.hiddenSources.forEach { source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(source.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { container.recommender.unhideSource(source.id) }) { Text("Вернуть") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { confirmReset = true }) { Text("Сбросить рекомендации") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Всё, чему научилась лента, хранится только на этом телефоне и никуда не отправляется. " +
                        "Лайки и подписки VK не учитываются: без прав VK приложение их не получает.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard("Музыка") {
                Text(
                    "Настроек плеера и пункта «Показывать музыку в профиле» пока нет: VK не даёт сторонним приложениям " +
                        "доступ к аудиозаписям и статусу музыки, а неработающие переключатели Kontakt не добавляет. " +
                        "Подробности — на вкладке «Музыка».",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard("Аккаунт") {
                Text(
                    "Вход выполнен через VK ID: пароль Kontakt не видит и не хранит. Несколько аккаунтов пока не " +
                        "поддерживаются. При выходе токен, обучение ленты и кеш изображений удаляются с телефона.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { confirmSignOut = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Выйти из аккаунта") }
            }

            SectionCard("Сеть и диагностика") {
                Text(
                    if (online) "Сеть: подключение есть." else "Сеть: подключения нет.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Kontakt ${BuildConfig.VERSION_NAME}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text("Последние ошибки запросов", style = MaterialTheme.typography.labelLarge)
                if (events.isEmpty()) {
                    Text("Ошибок нет.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    events.take(8).forEach { event ->
                        val code = event.code?.let { " ($it)" }.orEmpty()
                        Text(
                            "${formatClock(event.timeMs)} · ${event.method} · ${event.kind}$code",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { container.diagnostics.clear() }) { Text("Очистить журнал") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Журнал хранится только в памяти и исчезает при закрытии приложения. В нём нет токенов, " +
                        "сообщений и содержимого ответов VK. Повторить загрузку можно на самом экране кнопкой «Повторить» " +
                        "или жестом вниз.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard("О приложении") {
                Text(
                    "Kontakt ${BuildConfig.VERSION_NAME}\n" +
                        "Неофициальный клиент ВКонтакте. Не связан с VK и Telegram. Без рекламы и аналитики.\n" +
                        "Иконки: Material Icons (лицензия Apache 2.0).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить рекомендации?") },
            text = { Text("Лента забудет ваши интересы и просмотренное. Скрытые источники останутся скрытыми.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        container.recommender.reset()
                    },
                ) { Text("Сбросить") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Токен доступа, обучение ленты и кеш изображений будут удалены с телефона.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        scope.launch {
                            val loader = context.imageLoader
                            withContext(Dispatchers.IO) { loader.diskCache?.clear() }
                            loader.memoryCache?.clear()
                            container.signOutAndWipe()
                        }
                    },
                ) { Text("Выйти") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Отмена") } },
        )
    }
}

private fun learningText(signals: Int, interests: List<String>): String = when {
    signals == 0 ->
        "Kontakt пока ничего не узнал о ваших интересах. Он учится по времени просмотра публикаций и по кнопкам " +
            "«Интересно» и «Не интересно»."
    interests.isEmpty() -> "Учтено сигналов: $signals. Заметных интересов пока нет."
    else -> "Учтено сигналов: $signals. Сейчас лента считает интересным: ${interests.joinToString(", ")}."
}

private fun ThemeMode.themeLabel(): String = when (this) {
    ThemeMode.System -> "Система"
    ThemeMode.Light -> "Светлая"
    ThemeMode.Dark -> "Тёмная"
}

private fun EffectsMode.effectsLabel(): String = when (this) {
    EffectsMode.Full -> "Полные"
    EffectsMode.Light -> "Облегчённые"
}

@Composable
private fun AccentGrid(selected: AccentId, onSelect: (AccentId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentId.entries.chunked(4).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowItems.forEach { id -> AccentSwatch(id, id == selected) { onSelect(id) } }
            }
        }
    }
}

@Composable
private fun AccentSwatch(id: AccentId, selected: Boolean, onClick: () -> Unit) {
    val color = Color(0xFF000000.toInt() or AccentPalette.nominal(id))
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .padding(5.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            AccentPalette.label(id),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun TabSettingRow(
    id: TabId,
    index: Int,
    count: Int,
    onMove: (from: Int, delta: Int) -> Unit,
    onToggle: () -> Unit,
) {
    val visible = index >= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(id.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(id.label(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (visible) {
            IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) {
                Icon(VgIcons.ChevronUp, contentDescription = "Выше")
            }
            IconButton(onClick = { onMove(index, 1) }, enabled = index < count - 1) {
                Icon(VgIcons.ChevronDown, contentDescription = "Ниже")
            }
        }
        Switch(checked = visible, onCheckedChange = { onToggle() }, enabled = !(visible && count == 1))
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
