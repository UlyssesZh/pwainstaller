package io.github.ulysseszh.pwainstaller

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.webkit.CookieManager
import io.github.ulysseszh.pwainstaller.browser.BrowserCredentials
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Private IPC keeps every installer WebView API call in the temporary browser process. */
class LoginProvider : ContentProvider() {
    override fun onCreate() = true
    private fun <T> main(task: () -> T): T {
        val future = FutureTask(task)
        Handler(Looper.getMainLooper()).post(future)
        return future.get(20, TimeUnit.SECONDS)
    }
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        check(Binder.getCallingUid() == Process.myUid()) { "Installer required" }
        val context = requireNotNull(context)
        val token = requireNotNull(arg)
        LoginSessions.requireSession(context, token)
        LoginProfile.acquire(context)
        LoginSessions.requireSession(context, token)
        val url = extras?.getString("url").orEmpty()
        return Bundle().apply {
            when (method) {
                "cookie" -> putString("value", main { CookieManager.getInstance().getCookie(url) })
                "setCookie" -> {
                    val done = CountDownLatch(1)
                    main { CookieManager.getInstance().setCookie(url, extras!!.getString("value")) { done.countDown() } }
                    check(done.await(10, TimeUnit.SECONDS)) { "Cookie update timed out" }
                }
                "authorization" -> putString("value", BrowserCredentials.authorization(url, extras?.getString("value").orEmpty()))
                "flush" -> main { CookieManager.getInstance().flush() }
                "finish" -> main { LoginActivity.active?.finishSignIn() }
                else -> error("Unknown sign-in operation")
            }
        }
    }
    override fun getType(uri: Uri) = null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? = throw UnsupportedOperationException()
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = throw UnsupportedOperationException()
}
