package app.kontakt.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kontakt.client.AppContainer
import app.kontakt.client.core.UiError
import app.kontakt.client.core.toUiError
import app.kontakt.client.data.Profile
import app.kontakt.client.data.ProfileRepository
import app.kontakt.client.ui.common.vmFactory
import app.kontakt.client.ui.components.Avatar
import app.kontakt.client.ui.components.ErrorState
import app.kontakt.client.ui.components.LoadingBox
import app.kontakt.client.ui.components.LocalBottomBarPadding
import app.kontakt.client.ui.components.LocalOpenSettings
import app.kontakt.client.ui.components.ScreenHeader
import app.kontakt.client.ui.components.SectionCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {
    sealed interface State {
        data object Loading : State
        data class Content(val profile: Profile) : State
        data class Failed(val error: UiError) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load()
    }

    fun load() {
        if (job?.isActive == true) return
        _state.value = State.Loading
        job = viewModelScope.launch {
            _state.value = try {
                State.Content(repo.load())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                State.Failed(e.toUiError("VK не разрешил приложению получить данные профиля."))
            }
        }
    }
}

@Composable
fun ProfileScreen(container: AppContainer) {
    val vm: ProfileViewModel = viewModel(factory = vmFactory { ProfileViewModel(container.profileRepo) })
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Профиль")
        when (val current = state) {
            is ProfileViewModel.State.Loading -> LoadingBox(Modifier.weight(1f))
            is ProfileViewModel.State.Failed -> ErrorState(current.error, vm::load, Modifier.weight(1f))
            is ProfileViewModel.State.Content -> ProfileContent(current.profile, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileContent(profile: Profile, modifier: Modifier) {
    val openSettings = LocalOpenSettings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(profile.avatar, profile.name, 68.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        profile.status ?: "Профиль ВКонтакте",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionCard("Друзья") {
            Text(
                "Список друзей появится, когда VK предоставит Kontakt доступ к соответствующему методу API.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard("Настройки") {
            Text(
                "Тема, акцентный цвет, навигация и фильтры ленты.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (openSettings != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = openSettings) { Text("Открыть настройки") }
            }
        }
        Spacer(Modifier.height(LocalBottomBarPadding.current + 16.dp))
    }
}
