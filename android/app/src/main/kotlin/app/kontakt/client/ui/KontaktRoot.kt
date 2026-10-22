package app.kontakt.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kontakt.client.AppContainer
import app.kontakt.client.auth.AuthState
import app.kontakt.client.ui.components.StatusBarBackdrop
import app.kontakt.client.ui.screens.LoginScreen
import app.kontakt.client.ui.theme.VgTheme

@Composable
fun KontaktRoot(container: AppContainer) {
    val appearance by container.settings.appearance.collectAsStateWithLifecycle(initialValue = null)
    val current = appearance
    if (current == null) {
        // Настройки читаются за доли секунды; до этого виден фон окна, без мигания чужой темой.
        Box(Modifier.fillMaxSize())
        return
    }
    VgTheme(current.theme, current.accent, current.effects, current.style) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val auth by container.auth.state.collectAsStateWithLifecycle()
            // В VK Classic цвет шапки продолжается под строкой состояния на всех экранах, включая вход.
            StatusBarBackdrop {
                when (auth) {
                    is AuthState.SignedIn -> MainShell(container, current.tabs, current.effects)
                    is AuthState.SignedOut -> LoginScreen(container)
                }
            }
        }
    }
}
