package com.helmetheroes.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * Loads the official Helmet Heroes website in a WebView, locked to landscape,
 * with optional "fit game to screen" and on-screen touch controls that send
 * key presses to the game (see AppConfig for the key mapping).
 */
class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private var webView: WebView? = null
    private lateinit var progress: ProgressBar
    private lateinit var errorView: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var keys: KeySender
    private val controlViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("helmet_heroes", MODE_PRIVATE)
        keys = KeySender { webView }

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
        addControls()
        root.addView(errorView)
        errorView.visibility = View.GONE
        addMenuButton()
    }

    // ---------------------------------------------------------------- WebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(savedInstanceState: Bundle?) {
        webView?.let {
            root.removeView(it)
            it.destroy()
        }
        val wv = WebView(this)
        webView = wv
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true
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
                scheduleFit()
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

    /** Games often finish loading after the page does, so try a few times. */
    private fun scheduleFit() {
        if (!prefs.getBoolean(PREF_FIT, true)) return
        for (delay in longArrayOf(800L, 2500L, 6000L, 12000L)) {
            handler.postDelayed({ webView?.evaluateJavascript(FIT_JS, null) }, delay)
        }
    }

    private fun loadGame() {
        errorView.visibility = View.GONE
        webView?.loadUrl(AppConfig.GAME_URL)
    }

    // --------------------------------------------------------------- Controls

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun addControl(view: View, size: Int, gravity: Int, startOrEnd: Int, bottom: Int) {
        val lp = FrameLayout.LayoutParams(dp(size), dp(size), gravity)
        if (gravity and Gravity.END == Gravity.END) lp.rightMargin = dp(startOrEnd)
        else lp.leftMargin = dp(startOrEnd)
        lp.bottomMargin = dp(bottom)
        root.addView(view, lp)
        controlViews.add(view)
    }

    private fun addControls() {
        val bottomLeft = Gravity.BOTTOM or Gravity.START
        val bottomRight = Gravity.BOTTOM or Gravity.END

        addControl(JoystickView(this, keys), 150, bottomLeft, 28, 24)

        addControl(KeyButton(this, "ATK", AppConfig.KEY_ATTACK, keys), 88, bottomRight, 28, 24)
        addControl(KeyButton(this, "M", AppConfig.KEY_SKILL_M, keys), 64, bottomRight, 128, 24)
        addControl(KeyButton(this, "N", AppConfig.KEY_SKILL_N, keys), 64, bottomRight, 118, 100)
        addControl(KeyButton(this, "B", AppConfig.KEY_SKILL_B, keys), 64, bottomRight, 40, 124)
        addControl(KeyButton(this, "E", AppConfig.KEY_PICKUP, keys), 56, bottomRight, 208, 24)
        addControl(KeyButton(this, "HEAL", AppConfig.KEY_HEAL, keys), 56, bottomRight, 208, 90)

        applyControlsVisibility()
    }

    private fun applyControlsVisibility() {
        val show = prefs.getBoolean(PREF_CONTROLS, true)
        controlViews.forEach { it.visibility = if (show) View.VISIBLE else View.GONE }
        if (!show) keys.releaseAll()
    }

    private fun addMenuButton() {
        val btn = TextView(this).apply {
            text = "☰"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(110, 0, 0, 0))
                setStroke(2, Color.argb(160, 255, 255, 255))
            }
            setOnClickListener { showMenu() }
        }
        val lp = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.START)
        lp.leftMargin = dp(8)
        lp.topMargin = dp(8)
        root.addView(btn, lp)
    }

    private fun showMenu() {
        val controlsOn = prefs.getBoolean(PREF_CONTROLS, true)
        val fitOn = prefs.getBoolean(PREF_FIT, true)
        val items = arrayOf(
            "Reload game",
            if (controlsOn) "Hide touch controls" else "Show touch controls",
            if (fitOn) "Fit game to screen: ON (tap to turn off)" else "Fit game to screen: OFF (tap to turn on)",
            "Exit app"
        )
        AlertDialog.Builder(this)
            .setTitle("Helmet Heroes")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> loadGame()
                    1 -> {
                        prefs.edit().putBoolean(PREF_CONTROLS, !controlsOn).apply()
                        applyControlsVisibility()
                    }
                    2 -> {
                        prefs.edit().putBoolean(PREF_FIT, !fitOn).apply()
                        loadGame() // reload so the page layout is reset or re-fitted
                    }
                    3 -> finish()
                }
            }
            .show()
    }

    // ------------------------------------------------------------ Error screen

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

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // --------------------------------------------------------------- Lifecycle

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
        keys.releaseAll()
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
        handler.removeCallbacksAndMessages(null)
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val PREF_CONTROLS = "show_controls"
        private const val PREF_FIT = "fit_game"

        /**
         * Best-effort: finds the largest canvas/embed/object/iframe on the page and
         * scales it to fill the screen while keeping its aspect ratio.
         * NOT VERIFIED against the real page.
         */
        private val FIT_JS = """
            (function(){
              if (window.__hhFit) return;
              var els = [].slice.call(document.querySelectorAll('canvas,embed,object,iframe'));
              els = els.filter(function(e){ return e.offsetWidth * e.offsetHeight > 20000; });
              if (!els.length) return;
              els.sort(function(a,b){ return b.offsetWidth*b.offsetHeight - a.offsetWidth*a.offsetHeight; });
              var g = els[0];
              var w = g.offsetWidth, h = g.offsetHeight;
              window.__hhFit = true;
              document.documentElement.style.overflow = 'hidden';
              document.body.style.overflow = 'hidden';
              document.body.style.background = '#000';
              function fit(){
                var s = Math.min(window.innerWidth / w, window.innerHeight / h);
                g.style.position = 'fixed';
                g.style.zIndex = '2147483647';
                g.style.margin = '0';
                g.style.width = w + 'px';
                g.style.height = h + 'px';
                g.style.maxWidth = 'none';
                g.style.transformOrigin = '0 0';
                g.style.transform = 'scale(' + s + ')';
                g.style.left = ((window.innerWidth - w * s) / 2) + 'px';
                g.style.top = ((window.innerHeight - h * s) / 2) + 'px';
                g.style.boxShadow = '0 0 0 100vmax #000';
              }
              fit();
              window.addEventListener('resize', fit);
            })();
        """.trimIndent()
    }
}
