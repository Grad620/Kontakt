package app.kontakt.client.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Compact geometric glyphs drawn locally, with no extra icon dependency. */
object KontaktIcons {
    /** Two offset message shapes: distinct at small navigation sizes. */
    val Chat: ImageVector by lazy {
        icon(
            "Chat",
            "M5 3.5h12.5A3.5 3.5 0 0 1 21 7v6.5a3.5 3.5 0 0 1-3.5 3.5H11l-4.5 3v-3.1A3.5 3.5 0 0 1 3 13.5V7A3.5 3.5 0 0 1 5 3.5zM6 6.5A1.5 1.5 0 0 0 5 8v5.5A1.5 1.5 0 0 0 6.5 15H8v1.3l2-1.3h7.5a1.5 1.5 0 0 0 1.5-1.5V7A1.5 1.5 0 0 0 17.5 5.5H6z",
        )
    }
    /** Feed: a page with a strong lead line and two quieter rows. */
    val Feed: ImageVector by lazy {
        icon(
            "Feed",
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zm0 2v14h14V5H5zm2 2h10v2H7V7zm0 4h10v1.8H7V11zm0 4h7v1.8H7V15z",
        )
    }
    /** Music: note with a slightly angled stem and a compact rounded head. */
    val Music: ImageVector by lazy {
        icon(
            "Music",
            "M19 3v12.1a4.2 4.2 0 1 1-2-3.6V7.1L9 8.8v9.3a4.2 4.2 0 1 1-2-3.6V7.2L19 3zM5 18.1a2.2 2.2 0 1 0 4.4 0 2.2 2.2 0 0 0-4.4 0zm8 0a2.2 2.2 0 1 0 4.4 0 2.2 2.2 0 0 0-4.4 0z",
        )
    }
    /** Profile: circular head and a broad, simple shoulder silhouette. */
    val Person: ImageVector by lazy {
        icon(
            "Person",
            "M12 3.2a4.6 4.6 0 1 1 0 9.2 4.6 4.6 0 0 1 0-9.2zm0 2a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2zM12 14c4.7 0 8 2.3 8 5.1V21H4v-1.9C4 16.3 7.3 14 12 14zm0 2c-3.2 0-5.7 1.4-6 3h12c-.3-1.6-2.8-3-6-3z",
        )
    }

    val Back: ImageVector by lazy {
        icon("Back", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z")
    }
    /** Geometric sliders instead of a bulky gear. */
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
