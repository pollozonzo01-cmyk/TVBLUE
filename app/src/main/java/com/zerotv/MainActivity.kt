package com.zerotv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    companion object {
        // Carga del archivo index.html local privado desde assets
        const val TARGET_URL = "file:///android_asset/index.html"

        // User-agent de Chrome desktop — mejor compatibilidad con players de streaming
        const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    // INTERFAZ JAVASCRIPT: Conecta el HTML local de forma 100% estable y segura con Kotlin
    class WebAppInterface(private val activity: MainActivity) {
        @JavascriptInterface
        fun playChannel(url1: String, url2: String, name: String) {
            activity.runOnUiThread {
                val intent = Intent(activity, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_STREAM_URL, url1)
                    putExtra(PlayerActivity.EXTRA_STREAM_URL_2, url2)
                    putExtra(PlayerActivity.EXTRA_REFERER, "https://www.crack-stream.top/")
                    putExtra(PlayerActivity.EXTRA_USER_AGENT, USER_AGENT)
                }
                activity.startActivity(intent)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla siempre encendida + fullscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setImmersiveMode()
        setContentView(R.layout.activity_main)

        container    = findViewById(R.id.container)
        progressBar  = findViewById(R.id.progressBar)
        webView      = findViewById(R.id.webView)

        setupWebView()
        clearPrivateData()
        
        // Cargar el index.html privado local en la pantalla de inicio
        webView.loadUrl(TARGET_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        // ── JavaScript y medios ──────────────────────────────────────────────
        settings.javaScriptEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // ── Rendimiento / hardware ───────────────────────────────────────────
        @Suppress("DEPRECATION")
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // ── Optimizar Caché y almacenamiento ─────────────────────────────────
        settings.cacheMode = WebSettings.LOAD_DEFAULT // Cargar desde caché local para velocidad máxima
        settings.domStorageEnabled   = true
        settings.databaseEnabled     = true

        // ── Layout y Enfoque de Smart TV ─────────────────────────────────────
        settings.useWideViewPort     = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls  = false
        settings.displayZoomControls  = false

        // ── User-Agent desktop Chrome ────────────────────────────────────────
        settings.userAgentString = USER_AGENT

        // ── Mixed content (HTTP dentro de HTTPS — necesario para streams) ────
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // ── Anti-popup: bloquear ventanas emergentes de páginas externas ─────
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        webView.scrollBarStyle       = View.SCROLLBARS_INSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true

        // Configuración de foco para el control remoto
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        // Registrar la interfaz JavaScript para comunicación directa
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // ════════════════════════════════════════════════════════════════════
        //  WebViewClient — Filtros de red y seguridad
        // ════════════════════════════════════════════════════════════════════
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                injectAdBlockCSS(view)
                optimizeForTV(view)
            }

            // Ignorar errores de certificados SSL viejos/caducados en la TV Box
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()
                AdBlocker.shouldBlock(url)?.let { return it }
                return super.shouldInterceptRequest(view, request)
            }

            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                AdBlocker.shouldBlock(url)?.let { return it }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                AdBlocker.shouldBlock(url)?.let { return true }
                return false
            }

            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                AdBlocker.shouldBlock(url)?.let { return true }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) { onHideCustomView(); return }
                customView         = view
                customViewCallback = callback
                originalSystemUiVisibility = window.decorView.systemUiVisibility
                container.addView(
                    customView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                webView.visibility = View.GONE
                setImmersiveMode()
            }

            override fun onHideCustomView() {
                customView?.let { container.removeView(it) }
                customView = null
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility
                setImmersiveMode()
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean,
                isUserGesture: Boolean, resultMsg: android.os.Message?
            ): Boolean = false

            override fun getVideoLoadingProgressView(): View? = null
        }
    }

    private fun injectAdBlockCSS(view: WebView?) {
        val css = AdBlocker.COSMETIC_CSS
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
            .replace("\r", "")
        val js = """
            (function() {
                var existing = document.getElementById('_mspp_adblock');
                if (existing) return;
                var s = document.createElement('style');
                s.id = '_mspp_adblock';
                s.innerHTML = '$css';
                document.head && document.head.appendChild(s);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun optimizeForTV(view: WebView?) {
        val js = """
            (function() {
                var s = document.createElement('style');
                s.innerHTML =
                    'video { width:100%!important; height:auto!important; max-height:100vh!important; }' +
                    'body { overflow-x:hidden!important; }';
                document.head && document.head.appendChild(s);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun clearPrivateData() {
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
    }

    private fun setImmersiveMode() {
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
        if (hasFocus) setImmersiveMode()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                when {
                    customView != null -> {
                        webView.webChromeClient?.onHideCustomView(); true
                    }
                    webView.canGoBack() -> { webView.goBack(); true }
                    else -> false
                }
            }
            KeyEvent.KEYCODE_MENU -> { webView.reload(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
        setImmersiveMode()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
