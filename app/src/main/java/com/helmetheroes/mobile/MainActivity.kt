package com.helmetheroes.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Loads the official Helmet Heroes website in a WebView.
 * Whether the game itself runs depends on the site (see README: Flash caveat).
 */
class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private var webView: WebView? = null
    private lateinit var progress: ProgressBar
    private lateinit var errorView: LinearLayout
    private lateinit var errorText: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        errorView = buildErrorView()

        createWebView(savedInstanceState)

        root.addView(
            progress,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8, Gravity.TOP)
        )
        root.addView(errorView)
        errorView.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(savedInstanceState: Bundle?) {
        webView?.let {
            root.removeView(it)
            it.destroy()
        }
        val wv = WebView(this)
        webView = wv
        root.addView(
            wv, 0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    showError("Could not load the game.\n(${error.description})")
                }
            }

            override fun onReceivedHttpError(
                view: WebView, request: WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                if (request.isForMainFrame) {
                    showError("Server returned HTTP ${errorResponse.statusCode}.")
                }
            }

            override fun onRenderProcessGone(
                view: WebView, detail: RenderProcessGoneDetail
            ): Boolean {
                showError("The game process crashed. Tap Retry to restart it.")
                createWebView(null)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.GONE
            }
        }

        wv.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }

        if (savedInstanceState != null && wv.restoreState(savedInstanceState) != null) {
            // state restored
        } else {
            loadGame()
        }
    }

    private fun buildErrorView(): LinearLayout {
        errorText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        }
        val retry = Button(this).apply {
            text = "Retry"
            setOnClickListener { loadGame() }
        }
        val exit = Button(this).apply {
            text = "Exit"
            setOnClickListener { finish() }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(48, 48, 48, 48)
            addView(errorText)
            addView(retry)
            addView(exit)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun showError(message: String) {
        val offline = if (isOnline()) "" else "\nNo internet connection."
        errorText.text = message + offline
        errorView.visibility = View.VISIBLE
    }

    private fun loadGame() {
        errorView.visibility = View.GONE
        webView?.loadUrl(AppConfig.GAME_URL)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Suppress("DEPRECATION")
    private fun enterImmersive() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersive()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
