package www.barq.news.model

import com.beust.klaxon.Json

data class Source(
    var id: Int = 0,
    var categoryId: Int = 0,
    var name: String? = null,
    var account: String? = null,
    @Json(name = "image_url")
    var imageUrl: String? = null,
    @Json(name = "background_url")
    var backgroundUrl: String? = null,
    var description: String? = null,
    @Json(name = "checked")
    var isMySource: Boolean = false,
    @Json(name = "followers")
    var followerCount: Int = 0
)