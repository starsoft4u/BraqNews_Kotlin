package www.barq.news.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.beust.klaxon.Klaxon
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_setting.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.Constants.Companion.REQUEST_CODE_NOTIFICATION
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.extensions.*

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginButton.setOnClickListener {
            context!!.navigate(LoginActivity::class.java)
        }
        signUpButton.setOnClickListener {
            context!!.navigate(SignUpActivity::class.java)
        }
        settingNotification.isChecked = Defaults.notifyGlobal
        settingNotification.setOnCheckedChangeListener { _, checked ->
            Defaults.notifyGlobal = checked
            if (checked) {
                Defaults.notificaitons.forEach { subscribeNotification(it.toString(), true) }
            } else {
                unsubscribeNotification()
            }
        }
        settingNotificationSound.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_choose))
                Defaults.notificationSound?.let {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                }
            }
            startActivityForResult(intent, REQUEST_CODE_NOTIFICATION)
        }
        settingModifySource.setOnClickListener {
            context!!.navigate(EditSourceActivity::class.java)
        }
        settingContact.setOnClickListener {
            context!!.navigate(ContactUsActivity::class.java)
        }
        settingSuggest.setOnClickListener {
            context!!.navigate(SuggestActivity::class.java)
        }
        settingReportBug.setOnClickListener {
            context!!.navigate(ReportBugActivity::class.java)
        }
        settingFacebook.setOnClickListener {
            context!!.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://facebook.com/${Defaults.setting.facebook}")
            })
        }
        settingTwitter.setOnClickListener {
            context!!.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://twitter.com/${Defaults.setting.twitter}")
            })
        }
        settingInstagram.setOnClickListener {
            context!!.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://instagram.com/${Defaults.setting.instagram}")
            })
        }
        settingLinkedIn.setOnClickListener {
            context!!.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://linkedin.com/${Defaults.setting.linkedIn}")
            })
        }
        settingShare.setOnClickListener {
            context!!.share(content = getString(R.string.share_content))
        }
        settingLogout.setOnClickListener {
            logout()
            updateLogin()
            scrollView.scrollTo(0, 0)
        }

        updateLogin()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_NOTIFICATION && resultCode == Activity.RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            Defaults.notificationSound = uri.toString()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateLogin() {
        if (isLoggedIn) {
            loginPanel.visibility = GONE
            userPanel.visibility = VISIBLE
            logoutPanel.visibility = VISIBLE
            settingLogout.visibility = VISIBLE

            Picasso.get().load(Defaults.photoUrl).placeholder(R.drawable.ic_user).into(userImage)
            userName.text = Defaults.userName
            userEmail.text = Defaults.email
        } else {
            loginPanel.visibility = VISIBLE
            userPanel.visibility = GONE
            logoutPanel.visibility = GONE
            settingLogout.visibility = GONE

            userImage.setImageResource(R.drawable.ic_user)
            userName.text = ""
            userEmail.text = ""
        }
    }

    private fun loadData() {
        context!!.get("/settings") { res ->
            val json = res.obj("data")!!
            Defaults.setting = Klaxon().parseFromJsonObject(json)!!
        }
    }

    @Subscribe
    public fun viewWillAppear(event: Events.SelectTab) {
        if (event.index == 4) {
            loadData()
        }
    }

    @Subscribe
    public fun onLoggedIn(event: Events.LoggedIn) {
        updateLogin()
    }

    @Subscribe
    public fun onSigedUp(event: Events.SignedUp) {
        updateLogin()
    }

    @Subscribe
    public fun onLoggedOut(event: Events.LoggedOut) {
        updateLogin()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        EventBus.getDefault().register(this)
    }

    override fun onDetach() {
        EventBus.getDefault().unregister(this)
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingFragment()
    }
}
