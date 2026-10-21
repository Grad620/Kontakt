package app.vgradesk.client.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Иконки, нарисованные путями (без тяжёлой библиотеки material-icons-extended). Контуры — Material Icons, лицензия Apache 2.0. */
object VgIcons {
    val Chat: ImageVector by lazy {
        icon("Chat", "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z")
    }
    val Feed: ImageVector by lazy {
        icon("Feed", "M4 5h16v2H4zM4 11h16v2H4zM4 17h10v2H4z")
    }
    val Music: ImageVector by lazy {
        icon("Music", "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z")
    }
    val Person: ImageVector by lazy {
        icon(
            "Person",
            "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
        )
    }

    val Back: ImageVector by lazy {
        icon("Back", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z")
    }
    /** Геометричные «ползунки» вместо шестерёнки: проще и чище. */
    val Settings: ImageVector by lazy {
        icon("Settings", "M4 6h10v2H4zM18 6h2v2h-2zM14 4h4v6h-4zM4 16h2v2H4zM10 16h10v2H10zM6 14h4v6H6z")
    }
    val ChevronUp: ImageVector by lazy {
        icon("ChevronUp", "M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z")
    }
    val ChevronDown: ImageVector by lazy {
        icon("ChevronDown", "M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z")
    }
    val Star: ImageVector by lazy {
        icon("Star", "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z")
    }
    val StarOutline: ImageVector by lazy {
        icon(
            "StarOutline",
            "M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z",
        )
    }
    val Close: ImageVector by lazy {
        icon("Close", "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")
    }
    val More: ImageVector by lazy {
        icon("More", "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z")
    }
    val Info: ImageVector by lazy {
        icon(
            "Info",
            "M11 7h2v2h-2zM11 11h2v6h-2zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
        ).build()
}
