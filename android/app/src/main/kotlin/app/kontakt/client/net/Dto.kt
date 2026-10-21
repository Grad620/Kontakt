package app.kontakt.client.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VkUser(
    val id: Long,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("photo_100") val photo100: String? = null,
    @SerialName("photo_200") val photo200: String? = null,
    @SerialName("screen_name") val screenName: String? = null,
    val status: String? = null,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Serializable
data class VkGroup(
    val id: Long,
    val name: String = "",
    @SerialName("photo_100") val photo100: String? = null,
)

@Serializable
data class VkPeer(val id: Long, val type: String = "")

@Serializable
data class VkChatPhoto(@SerialName("photo_100") val photo100: String? = null)

@Serializable
data class VkChatSettings(val title: String? = null, val photo: VkChatPhoto? = null)

@Serializable
data class VkConversation(
    val peer: VkPeer,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("chat_settings") val chatSettings: VkChatSettings? = null,
)

@Serializable
data class VkPhotoSize(val url: String = "", val width: Int = 0, val height: Int = 0)

@Serializable
data class VkPhoto(val sizes: List<VkPhotoSize> = emptyList())

@Serializable
data class VkAttachment(val type: String = "", val photo: VkPhoto? = null)

@Serializable
data class VkMessage(
    val id: Long = 0,
    val date: Long = 0,
    @SerialName("from_id") val fromId: Long = 0,
    val text: String = "",
    val out: Int = 0,
    val attachments: List<VkAttachment> = emptyList(),
)

@Serializable
data class VkConversationItem(
    val conversation: VkConversation,
    @SerialName("last_message") val lastMessage: VkMessage? = null,
)

@Serializable
data class ConversationsResponse(
    val count: Int = 0,
    val items: List<VkConversationItem> = emptyList(),
    val profiles: List<VkUser> = emptyList(),
    val groups: List<VkGroup> = emptyList(),
)

@Serializable
data class VkCount(val count: Int = 0)

@Serializable
data class VkPost(
    val type: String = "",
    @SerialName("source_id") val sourceId: Long = 0,
    val date: Long = 0,
    @SerialName("post_id") val postId: Long = 0,
    val text: String = "",
    val likes: VkCount? = null,
    val comments: VkCount? = null,
    val reposts: VkCount? = null,
    val attachments: List<VkAttachment> = emptyList(),
    @SerialName("copy_history") val copyHistory: List<VkWallPost> = emptyList(),
    @SerialName("marked_as_ads") val markedAsAds: Int = 0,
)

/** Запись со стены (wall.get, newsfeed.search). */
@Serializable
data class VkWallPost(
    val id: Long = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    @SerialName("from_id") val fromId: Long = 0,
    val date: Long = 0,
    val text: String = "",
    @SerialName("post_type") val postType: String = "",
    val likes: VkCount? = null,
    val comments: VkCount? = null,
    val reposts: VkCount? = null,
    val attachments: List<VkAttachment> = emptyList(),
    @SerialName("copy_history") val copyHistory: List<VkWallPost> = emptyList(),
    @SerialName("marked_as_ads") val markedAsAds: Int = 0,
)

@Serializable
data class FeedResponse(
    val items: List<VkPost> = emptyList(),
    val profiles: List<VkUser> = emptyList(),
    val groups: List<VkGroup> = emptyList(),
    @SerialName("next_from") val nextFrom: String? = null,
)

/** Ответ wall.get и newsfeed.search (при extended=1). */
@Serializable
data class WallResponse(
    val count: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    val items: List<VkWallPost> = emptyList(),
    val profiles: List<VkUser> = emptyList(),
    val groups: List<VkGroup> = emptyList(),
    @SerialName("next_from") val nextFrom: String? = null,
)
