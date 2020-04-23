package www.barq.news.activity

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_report_bug.*
import www.barq.news.R
import www.barq.news.extensions.*

class ReportBugActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_bug)

        setTitleCenter(getString(R.string.report_bug))

        sendButton.setOnClickListener {
            if (!deviceEdit.validate({ it.isNotBlank() }, R.string.device_empty)) {
                deviceEdit.requestFocus()
            } else if (!errorMessage.validate({ it.isNotBlank() }, R.string.message_empty)) {
                errorMessage.requestFocus()
            } else if (!errorDetails.validate({ it.isNotBlank() }, R.string.details_empty)) {
                errorDetails.requestFocus()
            } else {
                dismissKeyboard()

                val params = listOf(
                    "device" to deviceEdit.text.toString(),
                    "message" to errorMessage.text.toString(),
                    "details" to errorDetails.text.toString()
                )
                post("/report", params = params) {
                    deviceEdit.text.clear()
                    errorMessage.text.clear()
                    errorDetails.text.clear()

                    message(R.string.message_sent_successfully)
                }
            }
        }
    }
}
