package www.barq.news

import android.app.Application
import android.content.Context
import com.chibatching.kotpref.Kotpref
import com.github.kittinunf.fuel.core.FuelManager
import com.kaopiz.kprogresshud.KProgressHUD
import com.twitter.sdk.android.core.Twitter

class BarqNewsApp : Application() {
    // Activity Indicator
    companion object {
        private var hud: KProgressHUD? = null

        fun showHUD(context: Context) {
            hud = KProgressHUD.create(context, KProgressHUD.Style.SPIN_INDETERMINATE)
                .setCancellable(false)
                .setDimAmount(0.5f)
                .show()
        }

        fun hideHUD() {
            hud?.dismiss()
        }
    }


    override fun onCreate() {
        super.onCreate()

        // SharedPreference
        Kotpref.init(this)

        // Http
        FuelManager.instance.basePath = Constants.BASE_URL

        // Twitter
        Twitter.initialize(this)
    }

}