package www.barq.news.model

import com.beust.klaxon.Json

data class Category(
    val id: Int = 0,
    @Json(name = "channel_id")
    val channelId: Int = 0,
    val name: String? = null,
    @Json(name = "icon_url")
    val iconUrl: String? = null,
    @Json(name = "image_url")
    val imageUrl: String? = null
)