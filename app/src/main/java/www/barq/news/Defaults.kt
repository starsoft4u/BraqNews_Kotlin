package www.barq.news

import com.beust.klaxon.Klaxon
import com.chibatching.kotpref.KotprefModel
import www.barq.news.model.Setting
import java.util.*

object Defaults : KotprefModel() {
    var jwtToken by nullableStringPref()
    var userId by intPref()
    var userName by nullableStringPref()
    var email by nullableStringPref()
    var photoUrl by nullableStringPref()
    var sourcesRaw by stringPref("")
    var notificationRaw by stringPref("")
    var favoritesRaw by stringPref("")
    var settingRaw by nullableStringPref()
    var notifyGlobal by booleanPref(true)
    var notificationSound by nullableStringPref()

    var sources: MutableSet<Int>
        get() = if (sourcesRaw.isBlank()) {
            mutableSetOf()
        } else {
            sourcesRaw.split("_").map { it.toInt() }.toMutableSet()
        }
        set(value) {
            sourcesRaw = value.joinToString("_")
        }

    var notificaitons: MutableSet<Int>
        get() = if (notificationRaw.isBlank()) {
            mutableSetOf()
        } else {
            notificationRaw.split("_").map { it.toInt() }.toMutableSet()
        }
        set(value) {
            notificationRaw = value.joinToString("_")
        }

    var favorites: MutableSet<Long>
        get() = if (favoritesRaw.isBlank()) {
            mutableSetOf()
        } else {
            favoritesRaw.split("_").map { it.toLong() }.toMutableSet()
        }
        set(value) {
            favoritesRaw = value.joinToString("_")
        }

    var setting: Setting
        get() = if (settingRaw == null) {
            Setting()
        } else {
            Klaxon().parse<Setting>(settingRaw!!)!!
        }
        set(value) {
            settingRaw = Klaxon().toJsonString(value)
        }

}
