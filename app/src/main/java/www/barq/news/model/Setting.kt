package www.barq.news.model

import com.beust.klaxon.Json

data class Setting(
    val facebook: String? = null,
    val twitter: String? = null,
    val instagram: String? = null,
    @Json(name = "linkedin")
    val linkedIn: String? = null,
    @Json(name = "contact_us_email")
    val contactUsEmail: String? = null,
    @Json(name = "suggestion_email")
    val suggestionEmail: String? = null,
    @Json(name = "ads_last_news")
    val adsLastNews: Boolean = false,
    @Json(name = "ads_newspaper")
    val adsNewspaper: Boolean = false,
    @Json(name = "ads_magazine")
    val adsMagazine: Boolean = false,
    @Json(name = "ads_news_detail")
    val adsNewsDetail: Boolean = false
)