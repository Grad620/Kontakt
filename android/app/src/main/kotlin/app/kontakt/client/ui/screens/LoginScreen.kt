package app.kontakt.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kontakt.client.AppContainer
import app.kontakt.client.BuildConfig
import app.kontakt.client.auth.AuthState
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.ui.components.SectionCard
import app.kontakt.client.ui.theme.Kontakt
import app.kontakt.client.ui.theme.VgSpacing

@Composable
fun LoginScreen(container: AppContainer) {
    val auth = container.auth
    val state by auth.state.collectAsStateWithLifecycle()
    val busy by auth.busy.collectAsStateWithLifecycle()
    val message = (state as? AuthState.SignedOut)?.message
    // В облегчённом режиме эффектов логотип неподвижен: бесконечная анимация тратит заряд.
    val logoPulse = if (Kontakt.effects == EffectsMode.Light) {
        1f
    } else {
        val pulse by rememberInfiniteTransition(label = "kontaktLogoPulse").animateFloat(
            initialValue = 0.985f,
            targetValue = 1.015f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "logoPulse",
        )
        pulse
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = Kontakt.colors.glassShadow, spotColor = Kontakt.colors.glassShadow)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = app.kontakt.client.R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = logoPulse
                            scaleY = logoPulse
                        },
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Kontakt",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.1).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ВКонтакте. Только нужное.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Чистая лента, удобные сообщения и спокойный интерфейс. Войди через официальную авторизацию VK, чтобы начать.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(30.dp))

            if (!auth.isConfigured) {
                SectionCard("Нужно подключить приложение VK Standalone", titleTextAlign = TextAlign.Center) {
                    Text(
                        "Для входа нужно приложение типа Standalone в кабинете VK. Добавьте его числовой ID в GitHub Actions → Settings → Secrets and variables → Actions → Variables под именем VK_STANDALONE_APP_ID. В кабинете укажите Android-пакет com.kontakt.app и SHA-1 подписи APK.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Button(
                    onClick = auth::signIn,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = if (busy) "Подключаемся к VK ID…" else "Войти через VK ID",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Откроется официальное окно авторизации VK. Пароль вводится на стороне VK — Kontakt его не видит и не сохраняет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (message != null) {
                Spacer(Modifier.height(VgSpacing.lg))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Kontakt — независимый клиент, не связанный с VK.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Kontakt · версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
