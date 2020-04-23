package www.barq.news.model

import com.beust.klaxon.Json

data class Comment(
    var id: Int = 0,
    @Json(name = "user_id")
    var userId: Int = 0,
    @Json(name = "username")
    var userName: String? = null,
    var userPhoto: String? = null,
    var comment: String? = null,
    var liked: String? = null,
    var disliked: String? = null,
    var flagged: String? = null,
    var issuedAt: Long = 0L
) {
    @Json(ignored = true)
    val likedArray: List<Int>
        get() = if (liked.isNullOrBlank()) {
            listOf()
        } else {
            liked!!.split(",").map { it.toInt() }
        }

    @Json(ignored = true)
    val dislikedArray: List<Int>
        get() = if (disliked.isNullOrBlank()) {
            listOf()
        } else {
            disliked!!.split(",").map { it.toInt() }
        }

    @Json(ignored = true)
    val flaggedArray: List<Int>
        get() = if (flagged.isNullOrBlank()) {
            listOf()
        } else {
            flagged!!.split(",").map { it.toInt() }
        }
}