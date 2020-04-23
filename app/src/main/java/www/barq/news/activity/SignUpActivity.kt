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
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import com.twitter.sdk.android.core.models.User
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.social_button_facebook.*
import kotlinx.android.synthetic.main.social_button_google.*
import kotlinx.android.synthetic.main.social_button_twitter.*
import org.greenrobot.eventbus.EventBus
import www.barq.news.Constants.Companion.APP_KEY
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.extensions.*

class SignUpActivity : BaseActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var authClient: TwitterAuthClient

    private val googleCode = 2020

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        setTitleCenter(getString(R.string.register))

        signUpButton.setOnClickListener { signUp() }

        // Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, googleCode)
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

    }

    private fun requestFacebookProfile(token: AccessToken) {
        GraphRequest.newMeRequest(token) { obj, response ->
            Log.w(APP_KEY, "Facebook fetch profile successfully! $response")
            val name = obj.getString("name")
            val email = obj.getString("email")
            val photo = obj.getJSONObject("picture").getJSONObject("data").getString("url")
            socialSignUp(name, email, photo, "facebook")

            LoginManager.getInstance().logOut()
        }.apply {
            parameters = Bundle().apply { putString("fields", "email,name,picture") }
        }.executeAsync()
    }

    private fun requestTwitterProfile(session: TwitterSession) {
        authClient.requestEmail(session, object : Callback<String>() {
            override fun success(result: Result<String>?) {
                Log.w(APP_KEY, "Twitter requested email successfully!")
                requestTwitterPhoto(session.userName, result!!.data)
            }

            override fun failure(exception: TwitterException?) {
                Log.w(APP_KEY, "Twitter request email failed", exception)
            }
        })
    }


    private fun requestTwitterPhoto(name: String, email: String) {
        TwitterCore.getInstance().apiClient.accountService
            .verifyCredentials(false, false, false)
            .enqueue(object : Callback<User>() {
                override fun success(result: Result<User>?) {
                    Log.w(APP_KEY, "Twitter request photo success!")
                    val photo = result!!.data.profileImageUrlHttps
                    socialSignUp(name, email, photo, "twitter")
                    TwitterCore.getInstance().sessionManager.clearActiveSession()
                }

                override fun failure(exception: TwitterException?) {
                    Log.w(APP_KEY, "Twitter request phot failed", exception)
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
        authClient.onActivityResult(requestCode, resultCode, data)
        if (requestCode == googleCode) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    socialSignUp(account.displayName!!, account.email!!, account.photoUrl!!.toString(), "google")
                    googleSignInClient.signOut()
                } else {
                    Log.w(APP_KEY, "Google sign in failed")
                }
            } catch (e: ApiException) {
                Log.w(APP_KEY, "Google sign in failed", e)
            }
        }
    }

    private fun signUp() {
        if (!fullNameEdit.validate({ it.isNotBlank() }, R.string.name_empty)) {
            fullNameEdit.requestFocus()
        } else if (!emailEdit.validate({ it.isValidEmail() }, R.string.email_invalid)) {
            emailEdit.requestFocus()
        } else if (!passwordEdit.validate({ it.isNotEmpty() }, R.string.password_empty)) {
            passwordEdit.requestFocus()
        } else {
            dismissKeyboard()

            val params = mutableListOf(
                "username" to fullNameEdit.text.toString(),
                "email" to emailEdit.text.toString(),
                "password" to passwordEdit.text.toString()
            )
            if (Defaults.sources.isNotEmpty()) {
                params.add("source_ids" to Defaults.sourcesRaw)

                val notifications = Defaults.notificaitons
                val block = Defaults.sources.filterNot { notifications.contains(it) }
                if (block.isNotEmpty()) {
                    params.add("block_ids" to block.joinToString("_"))
                }
            }
            if (Defaults.favorites.isNotEmpty()) {
                params.add("favorites" to Defaults.favoritesRaw)
            }

            post("/signup", params = params) { handleResponse(it) }
        }
    }

    private fun socialSignUp(name: String, email: String, photo: String, type: String) {
        val params = mutableListOf(
            "name" to name,
            "email" to email,
            "photo" to photo,
            "type" to type
        )
        if (Defaults.sources.isNotEmpty()) {
            params.add("source_ids" to Defaults.sourcesRaw)

            val notifications = Defaults.notificaitons
            val block = Defaults.sources.filterNot { notifications.contains(it) }
            if (block.isNotEmpty()) {
                params.add("block_ids" to block.joinToString("_"))
            }
        }
        if (Defaults.favorites.isNotEmpty()) {
            params.add("favorite_ids" to Defaults.favoritesRaw)
        }

        post("/social_signup", params = params) { handleResponse(it) }
    }

    private fun handleResponse(res: JsonObject) {
        val json = res.obj("data")!!

        Defaults.bulk {
            jwtToken = json.string("token")
            userId = json.int("id")!!
            email = json.string("email")
            userName = json.string("name")
            photoUrl = json.string("photo_url")
        }

        EventBus.getDefault().post(Events.SignedUp())
        finish()
    }

}
