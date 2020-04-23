package www.barq.news.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import www.barq.news.extensions.navigateClear

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val newsId = intent.getStringExtra("newsId")
        if (newsId != null) {
            navigateClear(MainActivity::class.java, "newsId" to newsId)
        } else {
            navigateClear(MainActivity::class.java)
        }
    }
}
