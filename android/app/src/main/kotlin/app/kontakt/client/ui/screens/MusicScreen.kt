package app.kontakt.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
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
import app.kontakt.client.ui.theme.Kontakt
import app.kontakt.client.ui.theme.KontaktIcons

/**
 * Музыка VK недоступна через разрешённые интерфейсы, поэтому здесь честное объяснение,
 * а не имитация плеера. Подробности и источники — docs/VK_API_MATRIX.md.
 */
@Composable
fun MusicScreen() {
    val style = Kontakt.style
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Музыка")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = style.screenPadding),
            verticalArrangement = Arrangement.spacedBy(style.postGap),
        ) {
            Spacer(Modifier.height(style.postGap / 2))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = style.cardShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Kontakt.colors.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            KontaktIcons.Music,
                            contentDescription = null,
                            tint = Kontakt.colors.accentText,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Музыки VK здесь нет",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "VK не даёт сторонним приложениям доступ к аудиозаписям, а Kontakt не обходит такие ограничения.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            SectionCard("Почему так") {
                Text(
                    "Публичный API аудиозаписей ВКонтакте закрыт для сторонних приложений: ещё в 2016 году VK " +
                        "объявил об отключении методов audio.*. Kontakt не использует неофициальные API, чужие токены " +
                        "и обход защиты, поэтому не показывает и не воспроизводит музыку VK.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
