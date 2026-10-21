package app.vgradesk.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vgradesk.client.AppContainer
import app.vgradesk.client.BuildConfig
import app.vgradesk.client.auth.AuthState
import app.vgradesk.client.ui.components.SectionCard
import app.vgradesk.client.ui.theme.VgSpacing

@Composable
fun LoginScreen(container: AppContainer) {
    val auth = container.auth
    val state by auth.state.collectAsStateWithLifecycle()
    val busy by auth.busy.collectAsStateWithLifecycle()
    val message = (state as? AuthState.SignedOut)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(VgSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("K", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(VgSpacing.lg))
        Text("Kontakt", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Неофициальный клиент ВКонтакте",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(VgSpacing.xl))

        if (!auth.isConfigured) {
            SectionCard("Нужна настройка") {
                Text(
                    "Приложение VK для Kontakt ещё не подключено: при сборке не передан VK_CLIENT_ID. " +
                        "Как это сделать со смартфона — в README проекта (раздел «Подключить VK ID»).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Button(onClick = auth::signIn, enabled = !busy) {
                Text(if (busy) "Ждём ответ VK ID…" else "Войти через VK ID")
            }
            Spacer(Modifier.height(VgSpacing.sm))
            Text(
                if (BuildConfig.VK_SCOPES.isBlank()) {
                    "Запрашивается только базовый доступ к профилю."
                } else {
                    "Запрашиваемые права: ${BuildConfig.VK_SCOPES}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (message != null) {
            Spacer(Modifier.height(VgSpacing.lg))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(VgSpacing.xl))
        Text(
            "Пароль вводится только на странице VK ID. Kontakt его не видит и не хранит.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
