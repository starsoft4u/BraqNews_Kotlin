package www.barq.news.extensions

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.greenrobot.eventbus.EventBus
import www.barq.news.BarqNewsApp
import www.barq.news.Constants.Companion.APP_KEY
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import kotlin.concurrent.thread

// Handle API
fun Context.get(
    url: String,
    params: List<Pair<String, Any>>? = null,
    indicator: Boolean = true,
    refreshView: SwipeRefreshLayout? = null,
    complete: ((json: JsonObject) -> Unit)? = null
) {
    if (indicator) {
        BarqNewsApp.showHUD(this)
    }
    thread {
        url.httpGet(params).header("Authentication", Defaults.jwtToken ?: "").responseString { request, _, result ->
            handleResponse(request, result) { complete?.invoke(it) }

            if (indicator) {
                BarqNewsApp.hideHUD()
            }
            refreshView?.isRefreshing = false
        }
    }
}

fun Context.post(
    url: String,
    params: List<Pair<String, Any>>? = null,
    indicator: Boolean = true,
    refreshView: SwipeRefreshLayout? = null,
    complete: ((json: JsonObject) -> Unit)? = null
) {
    if (indicator) {
        BarqNewsApp.showHUD(this)
    }
    thread {
        url.httpPost(params).header("Authentication", Defaults.jwtToken ?: "").responseString { request, _, result ->
            handleResponse(request, result) { complete?.invoke(it) }

            if (indicator) {
                BarqNewsApp.hideHUD()
            }
            refreshView?.isRefreshing = false
        }
    }
}

private fun Context.handleResponse(
    request: Request,
    result: Result<String, FuelError>,
    complete: ((json: JsonObject) -> Unit)?
) {
    result.fold<Unit>({ res ->
        Log.i(APP_KEY, "Request Success: $request")
        val json = Parser.default().parse(StringBuilder(res)) as JsonObject
        if (json.boolean("success") == true) {
            complete?.invoke(json)
        } else {
            message(json.string("error") ?: "Something went wrong.")
        }
    }, { error ->
        Log.i(APP_KEY, "Request failed: $request with (${error.response.statusCode}) ${error.response.responseMessage}")
        when (error.response.statusCode) {
            401 -> {
                message(R.string.you_have_to_login_or_signup)
                logout()
                EventBus.getDefault().post(Events.LoggedOut())
            }
            else -> {
                thread {
                    val body = error.errorData.toString(Charsets.UTF_8)
                    val json = Parser.default().parse(StringBuilder(body)) as JsonObject
                    Handler(mainLooper).post {
                        json.string("error")?.let { message(it) }
                    }
                }
            }
        }
    })
}
