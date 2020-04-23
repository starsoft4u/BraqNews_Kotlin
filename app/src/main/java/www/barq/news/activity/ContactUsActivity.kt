package www.barq.news.activity

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_contact_us.*
import www.barq.news.R
import www.barq.news.extensions.*

class ContactUsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_us)

        sendButton.setOnClickListener {
            if (!name.validate({ it.isNotBlank() }, R.string.name_empty)) {
                name.requestFocus()
            } else if (!email.validate({ it.isNotBlank() && it.isValidEmail() }, R.string.email_invalid)) {
                email.requestFocus()
            } else if (!message.validate({ it.isNotBlank() }, R.string.message_empty)) {
                message.requestFocus()
            } else {
                dismissKeyboard()

                val params = listOf(
                    "name" to name.text.toString(),
                    "email" to email.text.toString(),
                    "message" to message.text.toString()
                )
                post("/contact_us", params = params) {
                    name.text.clear()
                    email.text.clear()
                    message.text.clear()

                    message(R.string.message_sent_successfully)
                }
            }
        }
    }
}
