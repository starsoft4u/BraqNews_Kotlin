package www.barq.news.model

import com.beust.klaxon.Json
import com.beust.klaxon.TypeFor
import www.barq.news.custom.JsonTypeAdapterForSource

data class News(
    @TypeFor("source", adapter = JsonTypeAdapterForSource::class)
    var source: Source = Source(),
    var id: Long = 0,
    var title: String? = null,
    @Json(name = "image_url")
    var imageUrl: String? = null,
    @Json(name = "image_ratio")
    var imageRatio: Float? = null,
    var url: String? = null,
    var comment: Int = 0,
    var favorited: Boolean = false,
    @Json(name = "issued_at")
    var time: Long = 0L
)
