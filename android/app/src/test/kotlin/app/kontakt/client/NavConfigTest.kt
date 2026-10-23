package app.kontakt.client

import app.kontakt.client.core.NavConfig
import app.kontakt.client.core.TabId
import org.junit.Assert.assertEquals
import org.junit.Test

class NavConfigTest {
    @Test
    fun emptyOrBrokenValueGivesDefault() {
        assertEquals(NavConfig.DEFAULT, NavConfig.parse(null))
        assertEquals(NavConfig.DEFAULT, NavConfig.parse(""))
        assertEquals(NavConfig.DEFAULT, NavConfig.parse("video,clips"))
    }

    @Test
    fun parseKeepsOrderDropsUnknownAndDuplicates() {
        assertEquals(
            listOf(TabId.Music, TabId.Chats),
            NavConfig.parse("music, video, chats, music"),
        )
    }

    @Test
    fun serializeRoundTrip() {
        val tabs = listOf(TabId.Profile, TabId.Feed)
        assertEquals(tabs, NavConfig.parse(NavConfig.serialize(tabs)))
    }

    @Test
    fun moveShiftsAndIgnoresOutOfRange() {
        val tabs = NavConfig.DEFAULT
        assertEquals(listOf(TabId.Feed, TabId.Chats, TabId.Music, TabId.Profile), NavConfig.move(tabs, 0, 1))
        assertEquals(tabs, NavConfig.move(tabs, 0, -1))
        assertEquals(tabs, NavConfig.move(tabs, 3, 1))
    }

    @Test
    fun lastTabCannotBeRemovedAndReturnedTabGoesLast() {
        assertEquals(listOf(TabId.Chats), NavConfig.toggle(listOf(TabId.Chats), TabId.Chats))
        assertEquals(listOf(TabId.Chats, TabId.Music), NavConfig.toggle(listOf(TabId.Chats), TabId.Music))
        assertEquals(listOf(TabId.Feed, TabId.Music, TabId.Profile), NavConfig.toggle(NavConfig.DEFAULT, TabId.Chats))
        assertEquals(listOf(TabId.Chats), NavConfig.hidden(listOf(TabId.Feed, TabId.Music, TabId.Profile)))
    }
}
