package com.zerotv

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * AdBlocker — sistema de bloqueo de anuncios tipo Brave
 * Intercepta peticiones de red antes de que se descarguen.
 * Compatible con Android 5.0+ (API 21). Sin dependencias externas.
 */
object AdBlocker {

    // ─── Dominios bloqueados ────────────────────────────────────────────────
    // Publicidad, tracking, analytics, popups, telemetría
    private val BLOCKED_DOMAINS = setOf(
        // Google Ads / DoubleClick
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "googletagservices.com", "googletagmanager.com", "google-analytics.com",
        "analytics.google.com", "adservice.google.com", "pagead2.googlesyndication.com",
        // Facebook / Meta ads
        "facebook.com/tr", "connect.facebook.net", "graph.facebook.com",
        "an.facebook.com", "pixel.facebook.com",
        // Taboola / Outbrain / redes de contenido patrocinado
        "taboola.com", "trc.taboola.com", "cdn.taboola.com",
        "outbrain.com", "widgets.outbrain.com", "odb.outbrain.com",
        // Amazon Ads
        "amazon-adsystem.com", "aax.amazon.com", "assoc-amazon.com",
        // Redes de publicidad generales
        "adnxs.com", "ads.yahoo.com", "yimg.com",
        "adsafeprotected.com", "adsrvr.org", "adtechus.com",
        "advertising.com", "adform.net", "adition.com",
        "adfox.ru", "adfox.net", "adriver.ru",
        "admixer.net", "admob.com", "ads.mopub.com",
        "applovin.com", "inmobi.com", "mobvista.com",
        "flurry.com", "mopub.com", "verizonmedia.com",
        "oath.com", "brx.io", "criteo.com", "criteo.net",
        "pubmatic.com", "rubiconproject.com", "openx.net",
        "openx.com", "appnexus.com", "contextweb.com",
        "casalemedia.com", "indexexchange.com", "lijit.com",
        "sovrn.com", "sharethrough.com", "triplelift.com",
        "conversantmedia.com", "emxdgt.com", "rhythmone.com",
        "33across.com", "yieldmo.com", "undertone.com",
        "spotxchange.com", "spotx.tv", "springserve.com",
        "bidswitch.net", "districtm.ca", "smartadserver.com",
        "smaato.com", "smaato.net", "media.net",
        // Popups / redirects agresivos (comunes en sitios de streaming)
        "popcash.net", "popads.net", "popunder.net",
        "exoclick.com", "exosrv.com", "ero-advertising.com",
        "trafficjunky.net", "trafficstars.com", "juicyads.com",
        "adsterra.com", "hilltopads.net", "propellerads.com",
        "plugrush.com", "clickadu.com", "adcash.com",
        "revcontent.com", "mgid.com", "zergnet.com",
        // Trackers / fingerprinting / analytics
        "hotjar.com", "mouseflow.com", "fullstory.com",
        "loggly.com", "newrelic.com", "nr-data.net",
        "pingdom.net", "pingdom.com", "segment.com",
        "segment.io", "mixpanel.com", "amplitude.com",
        "heap.io", "optimizely.com", "evergage.com",
        "quantserve.com", "scorecardresearch.com", "comscore.com",
        "chartbeat.com", "chartbeat.net", "parsely.com",
        "nielsen.com", " nielsen-online.com",
        // Crypto miners en web
        "coinhive.com", "coin-hive.com", "minero.pw",
        "webminepool.com", "cryptoloot.pro", "deepminer.js",
        // CDNs de anuncios
        "cdn.adnxs.com", "ib.adnxs.com",
        "servedby.flashtalking.com", "flashtalking.com",
        "mediaplex.com", "pointroll.com",
        // Redes específicas de sitios de streaming pirata
        "tra.cx", "trc.samesites.net", "svtrdomain.com",
        "s.ad.sndcdn.com", "cdn.syndication.twimg.com"
    )

    // ─── Patrones de URL bloqueados ─────────────────────────────────────────
    // Rutas que típicamente sirven anuncios independientemente del dominio
    private val BLOCKED_URL_PATTERNS = listOf(
        "/ads/", "/ad/", "/adv/", "/advertisement/",
        "/adserver/", "/adservice/", "/adserving/",
        "/popup/", "/popunder/", "/pop.js", "/pop.php",
        "pagead", "doubleclick", "/banner/", "/banners/",
        "/tracking/", "/tracker/", "/track.php",
        "/beacon/", "/pixel/", "/pixels/",
        "googletag", "adsbygoogle", "adsense",
        "/affiliate/", "/sponsored/",
        "/click.php", "/click.js",
        "/stats/collect", "/collect?",
        "prebid.js", "prebid/",
        "/vast/", "vast.xml", "vast.php",  // Video Ad Serving Template
        "/vpaid/", "vpaid.js"               // Video ads
    )

    // ─── CSS cosmético ──────────────────────────────────────────────────────
    // Oculta elementos residuales que pasaron el filtro de red
    val COSMETIC_CSS = """
        /* Bloques de anuncios por clase/id comunes */
        [class*="adsbygoogle"], [class*="ad-container"], [class*="ad-wrapper"],
        [class*="ad-slot"], [class*="ad-unit"], [class*="ads-container"],
        [class*="advertisement"], [class*="banner-ad"], [class*="sponsored"],
        [id*="google_ads"], [id*="div-gpt-ad"], [id*="adslot"],
        [id*="ad-container"], [id*="ad-banner"], [id*="advertisement"],
        .adsbygoogle, .ads-wrapper, .ad-block, .ad_block,
        .popup-ad, .popup-overlay, .pop-ad,
        /* Overlay/interstitial de popups */
        [class*="interstitial"], [class*="modal-ad"], [class*="overlay-ad"],
        /* Taboola / Outbrain */
        [class*="taboola"], [class*="outbrain"], [id*="taboola"], [id*="outbrain"],
        /* Elementos de video ad */
        .vast-container, .vpaid-container,
        /* Botones de "cierra este anuncio" que no cierran nada */
        [class*="close-ad"], [id*="close-ad"] {
            display: none !important;
            visibility: hidden !important;
            pointer-events: none !important;
            height: 0 !important;
            overflow: hidden !important;
        }
        /* Evitar scroll-hijack y overlays que bloquean contenido */
        body {
            overflow-x: hidden !important;
        }
        /* Video player — siempre al frente */
        video, iframe[src*="player"], [class*="player-container"],
        [id*="player-container"], [class*="jwplayer"] {
            z-index: 9999 !important;
            position: relative !important;
        }
    """.trimIndent()

    // ─── Respuesta vacía reutilizable ────────────────────────────────────────
    private fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))
    )

    /**
     * Llama esto desde shouldInterceptRequest().
     * Devuelve una respuesta vacía si la URL debe bloquearse, null si debe pasar.
     */
    fun shouldBlock(url: String?): WebResourceResponse? {
        if (url.isNullOrEmpty()) return null
        val lower = url.lowercase()

        // 1. Verificar dominios bloqueados
        for (domain in BLOCKED_DOMAINS) {
            if (lower.contains(domain)) return emptyResponse()
        }

        // 2. Verificar patrones de URL
        for (pattern in BLOCKED_URL_PATTERNS) {
            if (lower.contains(pattern)) return emptyResponse()
        }

        return null // dejar pasar
    }
}
