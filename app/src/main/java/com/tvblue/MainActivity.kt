package com.tvblue

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import java.io.ByteArrayInputStream

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    // Referencias a los botones de favoritos
    private lateinit var btnCanal5: Button
    private lateinit var btnAzteca7: Button
    private lateinit var btnEspnMx: Button
    private lateinit var btnLasEstrellas: Button
    private lateinit var btnAztecaUno: Button
    private lateinit var btnForoTv: Button
    private lateinit var btnMilenioTv: Button
    private lateinit var btnTelemundo: Button
    private lateinit var btnTudnUsa: Button
    private lateinit var btnFoxSports: Button
    private lateinit var btnUnivision: Button

    companion object {
        // Enlaces de inicio de los 11 Canales Favoritos
        const val CANAL_5_URL = "https://telefullhd.net/en-vivo/canal-5/"
        const val AZTECA_7_URL = "https://telefullhd.net/en-vivo/azteca-7/"
        const val ESPN_MX_URL = "https://www.gabotv.com/?movies=espn"
        const val LAS_ESTRELLAS_URL = "https://telefullhd.net/en-vivo/las-estrellas/"
        const val AZTECA_UNO_URL = "https://www.gabotv.com/?movies=azteca-uno"
        const val FORO_TV_URL = "https://www.canalesdemexicoenvivo.com/canal-foro-tv/"
        const val MILENIO_TV_URL = "https://www.canalesdemexicoenvivo.com/canal-milenio-television/"
        const val TELEMUNDO_URL = "https://www.gabotv.com/?movies=telemundo"
        const val TUDN_USA_URL = "https://telefullhd.net/en-vivo/tudn-usa/"
        const val FOX_SPORTS_MX_URL = "https://www.gabotv.com/?movies=fox-sport"
        const val UNIVISION_URL = "https://www.gabotv.com/?movies=univision"

        // User-Agent de iPad Safari (Apple Tablet) - Máxima compatibilidad, obliga a reproducir HLS nativo sin scripts pesados de Flash/Desktop
        const val USER_AGENT = "Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/605.1.15"
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

        // Enlazar los 11 botones favoritos
        btnCanal5 = findViewById(R.id.btn_canal_5)
        btnAzteca7 = findViewById(R.id.btn_azteca_7)
        btnEspnMx = findViewById(R.id.btn_espn_mx)
        btnLasEstrellas = findViewById(R.id.btn_las_estrellas)
        btnAztecaUno = findViewById(R.id.btn_azteca_uno)
        btnForoTv = findViewById(R.id.btn_foro_tv)
        btnMilenioTv = findViewById(R.id.btn_milenio_tv)
        btnTelemundo = findViewById(R.id.btn_telemundo)
        btnTudnUsa = findViewById(R.id.btn_tudn_usa)
        btnFoxSports = findViewById(R.id.btn_fox_sports)
        btnUnivision = findViewById(R.id.btn_univision)

        setupWebView()
        setupFavorites()
        clearPrivateData()
        
        // Cargar por defecto Canal 5
        btnCanal5.isSelected = true
        webView.loadUrl(CANAL_5_URL)
    }

    private fun setupFavorites() {
        val buttons = listOf(
            btnCanal5, btnAzteca7, btnEspnMx, btnLasEstrellas, btnAztecaUno,
            btnForoTv, btnMilenioTv, btnTelemundo, btnTudnUsa, btnFoxSports, btnUnivision
        )

        fun selectFavorite(selectedButton: Button, url: String) {
            buttons.forEach { it.isSelected = false }
            selectedButton.isSelected = true
            webView.loadUrl(url)
            webView.requestFocus() // Devolver foco D-Pad para scroll inmediato
        }

        btnCanal5.setOnClickListener { selectFavorite(btnCanal5, CANAL_5_URL) }
        btnAzteca7.setOnClickListener { selectFavorite(btnAzteca7, AZTECA_7_URL) }
        btnEspnMx.setOnClickListener { selectFavorite(btnEspnMx, ESPN_MX_URL) }
        btnLasEstrellas.setOnClickListener { selectFavorite(btnLasEstrellas, LAS_ESTRELLAS_URL) }
        btnAztecaUno.setOnClickListener { selectFavorite(btnAztecaUno, AZTECA_UNO_URL) }
        btnForoTv.setOnClickListener { selectFavorite(btnForoTv, FORO_TV_URL) }
        btnMilenioTv.setOnClickListener { selectFavorite(btnMilenioTv, MILENIO_TV_URL) }
        btnTelemundo.setOnClickListener { selectFavorite(btnTelemundo, TELEMUNDO_URL) }
        btnTudnUsa.setOnClickListener { selectFavorite(btnTudnUsa, TUDN_USA_URL) }
        btnFoxSports.setOnClickListener { selectFavorite(btnFoxSports, FOX_SPORTS_MX_URL) }
        btnUnivision.setOnClickListener { selectFavorite(btnUnivision, UNIVISION_URL) }
    }

    private fun isDomainAllowed(url: String?): Boolean {
        if (url == null) return false
        if (url.startsWith("file://") || url.startsWith("data:")) return true
        
        val allowedDomains = listOf(
            "telefullhd.net", "gabotv.com", "canalesdemexicoenvivo.com", 
            "zonatv.store", "sudamericaplay", "bozztv.com", "ssh101.com", 
            "sharethis.com", "google.com", "googletagmanager.com"
        )
        
        val lower = url.lowercase()
        for (domain in allowedDomains) {
            if (lower.contains(domain)) {
                return true
            }
        }
        return false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        @Suppress("DEPRECATION")
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        settings.cacheMode = WebSettings.LOAD_DEFAULT // Optimizar velocidad con caché local
        settings.domStorageEnabled   = true
        settings.databaseEnabled     = true

        settings.useWideViewPort     = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls  = false
        settings.displayZoomControls  = false

        settings.userAgentString = USER_AGENT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        webView.scrollBarStyle       = View.SCROLLBARS_INSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                blockWindowOpen(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                blockWindowOpen(view)
                injectAdBlockCSS(view)
                optimizeForTV(view)
            }

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
                
                // Bloquear dominios de publicidad externa
                if (url != null && !isDomainAllowed(url)) {
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
                
                AdBlocker.shouldBlock(url)?.let { return it }
                return super.shouldInterceptRequest(view, request)
            }

            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                if (url != null && !isDomainAllowed(url)) {
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
                AdBlocker.shouldBlock(url)?.let { return it }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (!isDomainAllowed(url)) {
                    return true // Bloquear redirecciones maliciosas
                }
                return false
            }

            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && !isDomainAllowed(url)) {
                    return true
                }
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

    private fun blockWindowOpen(view: WebView?) {
        val js = """
            (function() {
                window.open = function() { return null; };
                window.alert = function() { return null; };
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
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
                    'body { overflow-x:hidden!important; }' +
                    'header, footer, .sidebar, .widget-area, #comments, .comments-area, .movie-info, ' +
                    '.entry-info, .related-posts, .dt_related, .top-page, .copy, .fbox, .logo, .nav, ' +
                    '.main-header, #nav-dropdown { display: none !important; }' +
                    'iframe[src*="player"],iframe[src*="embed"],iframe[src*="stream"]' +
                    '{ width:100%!important; min-height:450px!important; }';
                document.head && document.head.appendChild(s);
                
                var overlays = document.querySelectorAll(
                    '[class*="overlay"],[class*="modal"],[class*="interstitial"],[class*="popup"]'
                );
                overlays.forEach(function(el) {
                    var z = parseInt(window.getComputedStyle(el).zIndex);
                    if (z > 100) el.style.display = 'none';
                });
                
                // Desmuteo y reproducción automática robótica
                var attempts = 0;
                var interval = setInterval(function() {
                    attempts++;
                    if (attempts > 40) { clearInterval(interval); return; }
                    
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(v) {
                        v.muted = false;
                        v.volume = 1.0;
                        if (v.paused) {
                            v.play().catch(function(e){});
                        }
                    });

                    var playBtns = document.querySelectorAll('.play-btn, .doo_player_play, [class*="play"], .jw-icon-volume, .jw-icon-play');
                    playBtns.forEach(function(btn) {
                        if (btn.offsetWidth > 0 && btn.offsetHeight > 0) {
                            btn.click();
                        }
                    });
                }, 500);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun clearPrivateData() {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            when {
                customView != null -> {
                    webView.webChromeClient?.onHideCustomView()
                    return true
                }
                webView.canGoBack() -> {
                    webView.goBack()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
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
private var btnParrillaTv: Button? = null
private var btnCxtvLive: Button? = null
private var btnTudnMex: Button? = null
