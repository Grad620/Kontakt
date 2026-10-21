package app.kontakt.client

import app.kontakt.client.data.FeedMapping
import app.kontakt.client.data.FilterCounts
import app.kontakt.client.data.PostSource
import app.kontakt.client.net.VkAttachment
import app.kontakt.client.net.VkCount
import app.kontakt.client.net.VkGroup
import app.kontakt.client.net.VkPhoto
import app.kontakt.client.net.VkPhotoSize
import app.kontakt.client.net.VkPost
import app.kontakt.client.net.VkWallPost
import app.kontakt.client.rec.FilterConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedMappingTest {

    private fun feedPost(
        id: Long,
        text: String,
        ads: Int = 0,
        source: Long = -100,
        copy: List<VkWallPost> = emptyList(),
        attachments: List<VkAttachment> = emptyList(),
    ) = VkPost(
        type = "post", sourceId = source, date = 10, postId = id, text = text,
        likes = VkCount(5), comments = VkCount(2), reposts = VkCount(1),
        attachments = attachments, copyHistory = copy, markedAsAds = ads,
    )

    private fun photo(vararg widths: Int) = VkAttachment(
        type = "photo",
        photo = VkPhoto(widths.map { VkPhotoSize("https://img/$it.jpg", it, it) }),
    )

    @Test
    fun adsGoodsAndScamAreDroppedAndCountedOrdinaryPostsStay() {
        val raws = listOf(
            FeedMapping.raw(feedPost(1, "Купите всё", ads = 1)),
            FeedMapping.raw(feedPost(2, "Снюс оптом")),
            FeedMapping.raw(feedPost(3, "Заработок без вложений")),
            FeedMapping.raw(feedPost(4, "Обычная запись о прогулке")),
        )
        val batch = FeedMapping.map(raws, emptyList(), listOf(VkGroup(100, "Сообщество Х")), PostSource.Subscriptions, null, FilterConfig())
        assertEquals(listOf("-100_4"), batch.posts.map { it.key })
        assertEquals(FilterCounts(ads = 1, goods = 1, scam = 1), batch.dropped)
        val post = batch.posts.single()
        assertEquals("Сообщество Х", post.authorName)
        assertEquals(-100L, post.authorId)
        assertEquals(1, post.reposts)
    }

    @Test
    fun postsWithoutTextAndPhotosAreSkippedSilently() {
        val batch = FeedMapping.map(listOf(FeedMapping.raw(feedPost(1, ""))), emptyList(), emptyList(), PostSource.Subscriptions, null, FilterConfig())
        assertTrue(batch.posts.isEmpty())
        assertEquals(0, batch.dropped.total)
    }

    @Test
    fun repostTakesTextAndPhotosFromOriginal() {
        val original = VkWallPost(id = 1, ownerId = -5, text = "Оригинальный текст", attachments = listOf(photo(300)))
        val raw = FeedMapping.raw(feedPost(1, "", copy = listOf(original)))
        assertEquals("Оригинальный текст", raw.text)
        assertEquals(1, FeedMapping.photoUrls(raw.attachments).size)
    }

    @Test
    fun photoPreviewIsSmallerThan800WhenPossible() {
        assertEquals(listOf("https://img/500.jpg"), FeedMapping.photoUrls(listOf(photo(100, 500, 1200))))
        assertEquals(listOf("https://img/1200.jpg"), FeedMapping.photoUrls(listOf(photo(2000, 1200))))
        assertEquals(4, FeedMapping.photoUrls(List(7) { photo(300) }).size)
    }

    @Test
    fun wallPostAuthorRules() {
        val community = VkWallPost(id = 3, ownerId = -5, fromId = 77, text = "а", date = 1)
        assertEquals(-5L, FeedMapping.raw(community)!!.authorId)
        assertEquals("-5_3", FeedMapping.raw(community)!!.key)
        val user = VkWallPost(id = 4, ownerId = 9, fromId = 9, text = "б")
        assertEquals(9L, FeedMapping.raw(user)!!.authorId)
        assertNull(FeedMapping.raw(VkWallPost(id = 5, ownerId = 9, postType = "reply", text = "в")))
    }

    @Test
    fun filterSettingsAreRespected() {
        val raws = listOf(FeedMapping.raw(feedPost(1, "Реклама", ads = 1)), FeedMapping.raw(feedPost(2, "Снюс оптом")))
        val batch = FeedMapping.map(raws, emptyList(), emptyList(), PostSource.Search, "новости", FilterConfig(hideAds = false, hideRestrictedGoods = false))
        assertEquals(2, batch.posts.size)
        assertEquals("новости", batch.posts[0].topic)
        assertTrue("показанная реклама получает штраф ранжирования", batch.posts[0].spam > 0.0)
    }
}
