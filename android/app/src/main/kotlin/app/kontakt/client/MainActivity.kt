package app.kontakt.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kontakt.client.ui.KontaktRoot
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as KontaktApp
        val container = app.container
        if (BuildConfig.VK_ID_CONFIGURED) {
            container.auth.attachLauncher {
                VKID.instance.authorize(this, object : VKIDAuthCallback {
                    override fun onAuth(accessToken: AccessToken) {
                        container.auth.onVkIdAuthSuccess(accessToken.token)
                    }

                    override fun onFail(fail: VKIDAuthFail) {
                        container.auth.onVkIdAuthFailure(fail.toString())
                    }
                })
            }
        }
        val reportFile = File(filesDir, KontaktApp.CRASH_REPORT)
        val report = runCatching { reportFile.takeIf { it.isFile }?.readText() }.getOrNull()

        setContent {
            val showReport = remember { mutableStateOf(!report.isNullOrBlank()) }
            if (showReport.value && !report.isNullOrBlank()) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Kontakt обнаружил ошибку запуска", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Я сохранил технический отчёт на устройстве. Сделай скриншот этого экрана и пришли его сюда — по нему можно определить причину. Отчёт остаётся на телефоне и не отправляется автоматически.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(report!!.take(KontaktApp.MAX_REPORT_CHARS), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                reportFile.delete()
                                showReport.value = false
                            }) {
                                Text("Скрыть отчёт и открыть Kontakt")
                            }
                        }
                    }
                }
            } else {
                KontaktRoot(container)
            }
        }
    }

    override fun onDestroy() {
        (application as? KontaktApp)?.container?.auth?.detachLauncher()
        super.onDestroy()
    }
}
