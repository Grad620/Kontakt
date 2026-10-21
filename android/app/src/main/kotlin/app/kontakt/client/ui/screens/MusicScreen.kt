package app.kontakt.client.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kontakt.client.ui.components.LocalBottomBarPadding
import app.kontakt.client.ui.components.ScreenHeader
import app.kontakt.client.ui.components.SectionCard

/**
 * Музыка VK недоступна через разрешённые интерфейсы, поэтому здесь честное объяснение,
 * а не имитация плеера. Подробности и источники — docs/VK_API_MATRIX.md.
 */
@Composable
fun MusicScreen() {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Музыка")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionCard("Почему здесь нет треков VK") {
                Text(
                    "Публичный API аудиозаписей ВКонтакте закрыт для сторонних приложений: ещё в 2016 году VK " +
                        "объявил об отключении методов audio.*. Kontakt не использует неофициальные API, чужие токены " +
                        "и обход защиты, поэтому не показывает и не воспроизводит музыку VK.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionCard("Что возможно дальше") {
                Text(
                    "Допустимый вариант — плеер для музыки, которая лежит на самом телефоне (Media3, очередь, " +
                        "мини-плеер, управление из шторки уведомлений). В этой версии он ещё не реализован.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(LocalBottomBarPadding.current + 16.dp))
        }
    }
}
