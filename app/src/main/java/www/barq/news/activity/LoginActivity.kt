package www.barq.news.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.beust.klaxon.JsonObject
import com.chibatching.kotpref.bulk
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.messaging.FirebaseMessaging
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.social_button_facebook.*
import kotlinx.android.synthetic.main.social_button_google.*
import kotlinx.android.synthetic.main.social_button_twitter.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.Constants.Companion.APP_KEY
import www.barq.news.Constants.Companion.REQUEST_CODE_GOOGLE
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.extensions.*

class LoginActivity : BaseActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var authClient: TwitterAuthClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setTitleCenter(getString(R.string.login))

        loginButton.setOnClickListener { login() }
        signUpButton.setOnClickListener { navigate(SignUpActivity::class.java) }

        // Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE)
        }

        // Facebook
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                Log.w(APP_KEY, "Facebook login success")
                val token = result!!.accessToken!!
                requestFacebookProfile(token)
            }

            override fun onCancel() {
                Log.w(APP_KEY, "Facebook login cancelled")
            }

            override fun onError(error: FacebookException?) {
                Log.w(APP_KEY, "Facebook login failed", error)

            }
        })
        facebookButton.setOnClickListener {
            val token = AccessToken.getCurrentAccessToken()
            if (token != null && !token.isExpired) {
                requestFacebookProfile(token)
            } else {
                LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
            }
        }

        // Twitter
        authClient = TwitterAuthClient()
        twitterButton.setOnClickListener {
            val session = TwitterCore.getInstance().sessionManager.activeSession
            if (session != null) {
                requestTwitterProfile(session)
            } else {
                authClient.authorize(this, object : Callback<TwitterSession>() {
                    override fun success(result: Result<TwitterSession>?) {
                        Log.w(APP_KEY, "Twitter login success!")
                        requestTwitterProfile(result!!.data)
                    }

                    override fun failure(exception: TwitterException?) {
                        Log.w(APP_KEY, "Twitter login failed", exception)
                    }
                })
            }
        }

        // Eventbus
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun requestFacebookProfile(token: AccessToken) {
        GraphRequest.newMeRequest(token) { obj, response ->
            Log.w(APP_KEY, "Facebook fetch profile successfully! $response")
            val email = obj.getString("email")
            socialLogin(email, "facebook")

            LoginManager.getInstance().logOut()
        }.apply {
            parameters = Bundle().apply { putString("fields", "email") }
        }.executeAsync()
    }

    private fun requestTwitterProfile(session: TwitterSession) {
        authClient.requestEmail(session, object : Callback<String>() {
            override fun success(result: Result<String>?) {
                Log.w(APP_KEY, "Twitter requested email successfully!")
                socialLogin(result!!.data, "twitter")
                TwitterCore.getInstance().sessionManager.clearActiveSession()
            }

            override fun failure(exception: TwitterException?) {
                Log.w(APP_KEY, "Twitter request email failed", exception)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
        authClient.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GOOGLE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    socialLogin(account.email!!, "google")
                    googleSignInClient.signOut()
                } else {
                    Log.w(APP_KEY, "Google sign in failed")
                }
            } catch (e: ApiException) {
                Log.w(APP_KEY, "Google sign in failed", e)
            }
        }
    }

    private fun login() {
        if (!emailEdit.validate({ it.isValidEmail() }, R.string.email_invalid)) {
            emailEdit.requestFocus()
        } else if (!passwordEdit.validate({ it.isNotEmpty() }, R.string.password_empty)) {
            passwordEdit.requestFocus()
        } else {
            dismissKeyboard()
            val params = mapOf(
                "email" to emailEdit.text.toString(),
                "password" to passwordEdit.text.toString()
            ).toList()
            post("/login", params = params) { handleResponse(it) }
        }
    }

    private fun socialLogin(email: String, type: String) {
        val params = listOf(
            "email" to email,
            "type" to type
        )
        post("/social_login", params = params) { handleResponse(it) }
    }

    private fun handleResponse(res: JsonObject) {
        unsubscribeNotification()

        val json = res.obj("data")!!

        Defaults.bulk {
            jwtToken = json.string("token")
            userId = json.int("id")!!
            email = json.string("email")
            userName = json.string("name")
            photoUrl = json.string("photo_url")
            sources = json.array<Int>("source").orEmpty().toMutableSet()
            val block = json.array<Int>("blockNotification").orEmpty().toSet()
            notificaitons = sources.filterNot { block.contains(it) }.toMutableSet()
            favorites = mutableSetOf()
        }

        if (Defaults.notifyGlobal) {
            Defaults.notificaitons.forEach { subscribeNotification(it.toString(), true) }
        }

        EventBus.getDefault().post(Events.LoggedIn())
        finish()
    }

    @Subscribe
    public fun onSignedUp(event: Events.SignedUp) {
        finish()
    }
}
