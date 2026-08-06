package com.example.logic

import android.net.Uri

object UrlHandler {
    private val blockedDomains = listOf(
        // Reddit
        "reddit.com", "redd.it", "redditmedia.com",
        // Quora
        "quora.com", "qr.ae",
        // Wikipedia
        "wikipedia.org",
        // YouTube
        "youtube.com", "youtu.be", "youtube-nocookie.com", "ytimg.com",
        // Non-Google Search Engines
        "bing.com",
        "duckduckgo.com",
        "ecosia.org",
        "qwant.com",
        "brave.com",
        "startpage.com",
        "yahoo.com",
        "yandex.com", "yandex.ru", "yandex.by", "yandex.kz", "yandex.uz", "yandex.net", "yandex.org",
        "ask.com",
        "baidu.com",
        "naver.com",
        "sogou.com",
        "you.com",
        "dogpile.com",
        "gibiru.com",
        "mojeek.com",
        "swisscows.com",
        "searx.me", "searx.be", "searxng.org",
        "metacrawler.com",
        "webcrawler.com",
        "info.com",
        "exalead.com",
        "gigablast.com",
        "lycos.com",
        // Adult/Explicit domains
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com", "youporn.com"
    )

    fun isBlocked(url: String): Boolean {
        if (url.isEmpty() || url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:")) {
            return false
        }
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()
            if (host != null) {
                if (blockedDomains.any { host == it || host.endsWith(".$it") }) return true
            }
        } catch (e: Exception) {
            // fallback
        }
        return BlocklistManager.isDomainBlocked(url)
    }

    fun injectSafeSearch(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return url
            
            if (host.contains("google.") && (uri.path?.contains("/search") == true || uri.query?.contains("q=") == true)) {
                if (uri.getQueryParameter("safe") == null) {
                    val delimiter = if (url.contains("?")) "&" else "?"
                    return "$url${delimiter}safe=active"
                }
            }
            url
        } catch (e: Exception) {
            url
        }
    }
}

