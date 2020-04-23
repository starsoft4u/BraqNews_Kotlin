package www.barq.news.extensions

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import www.barq.news.Constants
import www.barq.news.Defaults
import www.barq.news.R
import java.text.SimpleDateFormat
import java.util.*

val Boolean.intValue: Int
    get() = if (this) 1 else 0
val Int.isOdd: Boolean
    get() = this % 2 == 1
val Int.isEven: Boolean
    get() = this % 2 == 0

// dp -> pixel (Float)
fun View.toPixel(dp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
}

// pixel -> dp (Int)
fun View.toDP(pixel: Float): Float {
    return pixel * DisplayMetrics.DENSITY_DEFAULT / context.resources.displayMetrics.xdpi
}

// Validate email
fun String.isValidEmail(): Boolean = this.isNotBlank() and Patterns.EMAIL_ADDRESS.matcher(this).matches()

// Validate EditText, alert(String) & return
fun EditText.validate(validator: (String) -> Boolean, resId: Int): Boolean {
    if (validator(text.toString())) {
        return true
    }

    context.message(resId)
    return false
}

// Date & Time
fun Long.formattedString(context: Context): String {
    val diff = Date().time - this
    return when {
        diff >= Constants.ONE_WEEK -> {
            val df = SimpleDateFormat("dd MMM yyyy", Locale("ar", "SA"))
            val cal = Calendar.getInstance()
            cal.timeInMillis = this
            df.format(cal.time)
        }
        diff >= Constants.ONE_DAY -> {
            val day = diff / Constants.ONE_DAY
            if (day == 1L) context.getString(R.string.day_ago)
            else context.getString(R.string._day_ago, day.toString())
        }
        diff >= Constants.ONE_HOUR -> {
            val hour = diff / Constants.ONE_HOUR
            if (hour == 1L) context.getString(R.string.hour_ago)
            else context.getString(R.string._hour_ago, hour.toString())
        }
        diff >= Constants.ONE_MINUTE -> {
            val min = diff / Constants.ONE_MINUTE
            if (min == 1L) context.getString(R.string.minute_ago)
            else context.getString(R.string._minute_ago, min.toString())
        }
        else -> context.getString(R.string.just_ago)
    }
}

// App only
fun logout() {
    Defaults.jwtToken = null
    Defaults.userId = 0
    Defaults.userName = null
    Defaults.email = null
    Defaults.photoUrl = null
    Defaults.sources = mutableSetOf()
    Defaults.notificaitons = mutableSetOf()
    Defaults.favorites = mutableSetOf()
    unsubscribeNotification()
}

val isLoggedIn: Boolean
    get() = Defaults.jwtToken != null

fun unsubscribeNotification() {
    Defaults.notificaitons.forEach {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(it.toString())
    }
    Thread(Runnable {
        FirebaseInstanceId.getInstance().deleteInstanceId()
    }).start()
}

fun subscribeNotification(topic: String?, subscribe: Boolean) {
    if (!Defaults.notifyGlobal) {
        return
    }

    if (subscribe) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(Constants.APP_KEY, "Subscribed success to: $topic")
            } else {
                Log.d(Constants.APP_KEY, "Subscribed failed to: $topic")
            }
        }
    } else {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(Constants.APP_KEY, "Unsubscribed success from: $topic")
            } else {
                Log.d(Constants.APP_KEY, "Unsubscribed failed from: $topic")
            }
        }
    }
}
