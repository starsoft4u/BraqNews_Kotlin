package www.barq.news.activity

import android.os.Bundle
import android.util.Patterns
import kotlinx.android.synthetic.main.activity_suggest.*
import www.barq.news.R
import www.barq.news.extensions.dismissKeyboard
import www.barq.news.extensions.message
import www.barq.news.extensions.post
import www.barq.news.extensions.validate

class SuggestActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggest)

        sendButton.setOnClickListener {
            if (!sourceName.validate({ it.isNotBlank() }, R.string.name_empty)) {
                sourceName.requestFocus()
            } else if (!sourceUrl.validate(
                    { it.isNotBlank() && Patterns.WEB_URL.matcher(it).matches() },
                    R.string.url_invalid
                )
            ) {
                sourceUrl.requestFocus()
            } else {
                dismissKeyboard()

                val params = listOf(
                    "name" to sourceName.text.toString(),
                    "url" to sourceUrl.text.toString()
                )
                post("/suggest", params = params) {
                    sourceName.text.clear()
                    sourceUrl.text.clear()

                    message(R.string.suggested_successfully)
                }
            }
        }
    }
}
