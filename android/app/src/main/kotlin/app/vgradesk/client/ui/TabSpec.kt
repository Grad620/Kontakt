package app.vgradesk.client.ui

import androidx.compose.ui.graphics.vector.ImageVector
import app.vgradesk.client.core.TabId
import app.vgradesk.client.ui.theme.VgIcons

fun TabId.label(): String = when (this) {
    TabId.Chats -> "Чаты"
    TabId.Feed -> "Лента"
    TabId.Music -> "Музыка"
    TabId.Profile -> "Профиль"
}

fun TabId.icon(): ImageVector = when (this) {
    TabId.Chats -> VgIcons.Chat
    TabId.Feed -> VgIcons.Feed
    TabId.Music -> VgIcons.Music
    TabId.Profile -> VgIcons.Person
}
