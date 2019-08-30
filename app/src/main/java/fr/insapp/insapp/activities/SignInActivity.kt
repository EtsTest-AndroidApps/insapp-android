package fr.insapp.insapp.activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.insapp.insapp.R
import fr.insapp.insapp.http.ServiceGenerator
import fr.insapp.insapp.models.credentials.LoginCredentials
import fr.insapp.insapp.models.credentials.SessionCredentials
import fr.insapp.insapp.models.credentials.SigninCredentials
import kotlinx.android.synthetic.main.activity_sign_in.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SignInActivity : AppCompatActivity() {

    private val CAS_URL = "https://cas.insa-rennes.fr/cas/login?service=https://insapp.fr/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        setSupportActionBar(toolbar_sign_in)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //refresh_webpage.setOnRefreshListener(this)

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies { res ->
            Log.d("CAS", "Cookie removed: $res")
        }

        webview_conditions.loadUrl(CAS_URL)
        webview_conditions.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progress_bar.visibility = View.VISIBLE

                val id = url.lastIndexOf("?ticket=")
                if (url.contains("?ticket=")) {
                    val ticket = url.substring(id + "?ticket=".length, url.length)

                    Log.d("CAS", "URL: $url")
                    Log.d("CAS", "Ticket: $ticket")

                    signin(ticket)
                    webview_conditions.visibility = View.INVISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progress_bar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // TODO : Gérer l'affichage
                //progress_bar.visibility = View.GONE
            }
        }
    }

    fun signin(ticket: String) {
        val signinCredentials = SigninCredentials(Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID))

        val call = ServiceGenerator.create().signUser(ticket, signinCredentials)
        call.enqueue(object : Callback<LoginCredentials> {
            override fun onResponse(call: Call<LoginCredentials>, response: Response<LoginCredentials>) {
                val loginCredentials = response.body()
                if (response.isSuccessful && loginCredentials != null) {
                    login(loginCredentials)
                } else {
                    Toast.makeText(this@SignInActivity, TAG, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginCredentials>, t: Throwable) {
                Toast.makeText(this@SignInActivity, TAG, Toast.LENGTH_LONG).show()
            }
        })
    }

    fun login(loginCredentials: LoginCredentials) {
        val call = ServiceGenerator.create().logUser(loginCredentials)
        call.enqueue(object : Callback<SessionCredentials> {
            override fun onResponse(call: Call<SessionCredentials>, response: Response<SessionCredentials>) {
                if (response.isSuccessful) {
                    startActivity(Intent(this@SignInActivity, MainActivity::class.java))

                    finish()
                } else {
                    Toast.makeText(this@SignInActivity, TAG, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SessionCredentials>, t: Throwable) {
                Toast.makeText(this@SignInActivity, TAG, Toast.LENGTH_LONG).show()
            }
        })
    }

    companion object {

        const val TAG = "SignInActivity"
    }
}
