package app.kontakt.client.ui

import androidx.compose.ui.graphics.vector.ImageVector
import app.kontakt.client.core.TabId
import app.kontakt.client.ui.theme.KontaktIcons

fun TabId.label(): String = when (this) {
    TabId.Chats -> "Чаты"
    TabId.Feed -> "Лента"
    TabId.Music -> "Музыка"
    TabId.Profile -> "Профиль"
}

fun TabId.icon(): ImageVector = when (this) {
    TabId.Chats -> KontaktIcons.Chat
    TabId.Feed -> KontaktIcons.Feed
    TabId.Music -> KontaktIcons.Music
    TabId.Profile -> KontaktIcons.Person
}
