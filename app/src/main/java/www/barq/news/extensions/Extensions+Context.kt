package www.barq.news.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.beust.klaxon.Klaxon
import www.barq.news.R

// Create view from context
fun <T : View> Context.create(clazz: Class<T>, init: T.() -> Unit): T {
    val constructor = clazz.getConstructor(Context::class.java)
    val view = constructor.newInstance(this)
    view.init()
    return view
}

// Create view and add to parent
fun <T : View> ViewGroup.create(clazz: Class<T>, init: T.() -> Unit): T {
    return context.create(clazz, init).also { addView(it) }
}

// Hide keyboard
fun AppCompatActivity.dismissKeyboard() {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

// Toast
fun Context.message(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_LONG).show()

fun Context.message(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

// Navigate Activity
fun <T : AppCompatActivity> Context.navigate(activity: Class<T>, vararg args: Pair<String, Any>) {
    val intent = Intent(this, activity)
    args.forEach {
        val value = it.second
        when (value) {
            is Int -> intent.putExtra(it.first, value)
            is String -> intent.putExtra(it.first, value)
            is Boolean -> intent.putExtra(it.first, value)
            is Long -> intent.putExtra(it.first, value)
            is Float -> intent.putExtra(it.first, value)
            is Double -> intent.putExtra(it.first, value)
            is Array<*> -> intent.putExtra(it.first, value)
            else -> intent.putExtra(it.first, Klaxon().toJsonString(value))
        }
    }

    startActivity(intent)
}

fun <T : AppCompatActivity> Context.navigateClear(activity: Class<T>, vararg args: Pair<String, Any>) {
    val intent = Intent(this, activity)
    args.forEach {
        val value = it.second
        when (value) {
            is Int -> intent.putExtra(it.first, value)
            is String -> intent.putExtra(it.first, value)
            is Boolean -> intent.putExtra(it.first, value)
            is Long -> intent.putExtra(it.first, value)
            is Float -> intent.putExtra(it.first, value)
            is Double -> intent.putExtra(it.first, value)
            is Array<*> -> intent.putExtra(it.first, value)
            else -> intent.putExtra(it.first, Klaxon().toJsonString(value))
        }
    }

    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

    startActivity(intent)
}

// Center acitonbar
fun AppCompatActivity.setTitleCenter(title: String?) {
    val textView = layoutInflater.inflate(R.layout.action_bar, null, false) as TextView
    supportActionBar?.apply {
        setDisplayShowTitleEnabled(false)
        setDisplayShowCustomEnabled(true)
        setCustomView(
            textView,
            ActionBar.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        textView.text = title
    }
}

// Share
fun Context.share(url: String? = null, content: String? = null, title: String? = null) {
    val intent = if (url == null) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
    } else {
        val uri = when {
            url.startsWith("https://t.co/") -> url.replace("https://t.co/", "https://barq.news/n/")
            url.startsWith("https://twitter.com/") -> {
                val id = url.substring(url.lastIndexOf('/') + 1)
                "https://barq.news/t/$id"
            }
            else -> url
        }

        val theContent = "$title\n\n$uri\n\n$content"
        Intent(Intent.ACTION_SEND, Uri.parse(uri)).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, theContent)
        }
    }

    startActivity(Intent.createChooser(intent, getString(R.string.share_with)))
}