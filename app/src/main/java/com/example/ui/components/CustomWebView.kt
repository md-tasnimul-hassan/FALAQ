package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

@SuppressLint("SetJavaScriptEnabled")
class CustomWebView(context: Context) : WebView(context) {
    var onScrollStateChange: ((showBars: Boolean) -> Unit)? = null

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setGeolocationEnabled(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadsImagesAutomatically = true
        settings.setSupportMultipleWindows(false)
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        settings.offscreenPreRaster = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Enable hardware rendering for full WebGL, 3D CSS transforms, and CSS view transitions support
        setLayerType(LAYER_TYPE_NONE, null)
        
        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        // Present as clean Chrome Mobile, fixing Daraz, Apple, and complex web apps
        val defaultUa = settings.userAgentString
        settings.userAgentString = defaultUa.replace("Version/4.0 ", "").replace("; wv", "").replace(" wv", "")
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        val wrapped = wrapActionModeCallback(callback)
        val effectiveType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && type == ActionMode.TYPE_PRIMARY) {
            ActionMode.TYPE_FLOATING
        } else {
            type
        }
        return super.startActionMode(wrapped, effectiveType)
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        val wrapped = wrapActionModeCallback(callback)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            super.startActionMode(wrapped, ActionMode.TYPE_FLOATING)
        } else {
            super.startActionMode(wrapped)
        }
    }

    private fun wrapActionModeCallback(originalCallback: ActionMode.Callback?): ActionMode.Callback? {
        if (originalCallback == null) return null
        val oaldItemId = 987654321

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return object : ActionMode.Callback2() {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    val created = originalCallback.onCreateActionMode(mode, menu)
                    if (menu != null && menu.findItem(oaldItemId) == null) {
                        menu.add(Menu.NONE, oaldItemId, Menu.NONE, "OALD 10th")
                    }
                    return created
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    return originalCallback.onPrepareActionMode(mode, menu)
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    if (item?.itemId == oaldItemId) {
                        evaluateJavascript("(function() { return window.getSelection().toString(); })()") { rawText ->
                            val selectedText = rawText?.trim()
                                ?.removeSurrounding("\"")
                                ?.replace("\\n", "\n")
                                ?.replace("\\\"", "\"")
                                ?.trim()
                            if (!selectedText.isNullOrBlank()) {
                                openWithOALD(context, selectedText)
                            } else {
                                Toast.makeText(context, "No text selected", Toast.LENGTH_SHORT).show()
                            }
                            mode?.finish()
                        }
                        return true
                    }
                    return originalCallback.onActionItemClicked(mode, item)
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                    originalCallback.onDestroyActionMode(mode)
                }

                override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
                    if (originalCallback is ActionMode.Callback2) {
                        originalCallback.onGetContentRect(mode, view, outRect)
                    } else {
                        super.onGetContentRect(mode, view, outRect)
                    }
                }
            }
        }

        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                val created = originalCallback.onCreateActionMode(mode, menu)
                if (menu != null && menu.findItem(oaldItemId) == null) {
                    menu.add(Menu.NONE, oaldItemId, Menu.NONE, "OALD 10th")
                }
                return created
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return originalCallback.onPrepareActionMode(mode, menu)
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (item?.itemId == oaldItemId) {
                    evaluateJavascript("(function() { return window.getSelection().toString(); })()") { rawText ->
                        val selectedText = rawText?.trim()
                            ?.removeSurrounding("\"")
                            ?.replace("\\n", "\n")
                            ?.replace("\\\"", "\"")
                            ?.trim()
                        if (!selectedText.isNullOrBlank()) {
                            openWithOALD(context, selectedText)
                        } else {
                            Toast.makeText(context, "No text selected", Toast.LENGTH_SHORT).show()
                        }
                        mode?.finish()
                    }
                    return true
                }
                return originalCallback.onActionItemClicked(mode, item)
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                originalCallback.onDestroyActionMode(mode)
            }
        }
    }

    private fun openWithOALD(context: Context, text: String) {
        val word = text.trim()
        if (word.isBlank()) return

        // 1. Always copy the word to system Clipboard so OALD can auto-detect it on launch
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboard != null) {
                val clip = android.content.ClipData.newPlainText("OALD Word", word)
                clipboard.setPrimaryClip(clip)
            }
        } catch (_: Exception) {}

        var launched = false
        val pm = context.packageManager

        // 2. Direct launch targeting OALD 10 (com.oup.elt.oald10_gp) explicit component
        try {
            val specificComp = android.content.ComponentName(
                "com.oup.elt.oald10_gp",
                "com.paragon_software.splash_screen_manager.BaseLauncherActivity"
            )
            val specificIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = specificComp
                putExtra(Intent.EXTRA_TEXT, word)
                putExtra(Intent.EXTRA_PROCESS_TEXT, word)
                putExtra(android.app.SearchManager.QUERY, word)
                putExtra("query", word)
                putExtra("word", word)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(specificIntent)
            launched = true
        } catch (_: Exception) {}

        val oaldPackages = listOf(
            "com.oup.elt.oald10_gp",
            "com.oup.elt.oald10",
            "com.oup.elt.oald9_gp",
            "com.oup.elt.oald9",
            "com.oup.elt.oald",
            "com.oxford.dictionary.oald"
        )

        if (!launched) {
            val candidatePackages = LinkedHashSet<String>()
            candidatePackages.addAll(oaldPackages)

            try {
                val installedApps = pm.getInstalledPackages(0)
                for (pkg in installedApps) {
                    val pName = pkg.packageName.lowercase()
                    if (pName.contains("oald") || pName.contains("oup.elt")) {
                        candidatePackages.add(pkg.packageName)
                    }
                }
            } catch (_: Exception) {}

            for (pkg in candidatePackages) {
                if (launched) break

                // Try Launch Intent for Package
                try {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.apply {
                            putExtra(Intent.EXTRA_TEXT, word)
                            putExtra(Intent.EXTRA_PROCESS_TEXT, word)
                            putExtra(android.app.SearchManager.QUERY, word)
                            putExtra("query", word)
                            putExtra("word", word)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(launchIntent)
                        launched = true
                        break
                    }
                } catch (_: Exception) {}

                // Try ACTION_PROCESS_TEXT
                try {
                    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                        setType("text/plain")
                        putExtra(Intent.EXTRA_PROCESS_TEXT, word)
                        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                        launched = true
                        break
                    }
                } catch (_: Exception) {}

                // Try ACTION_SEND
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, word)
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                        launched = true
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        if (!launched) {
            // Try general ACTION_PROCESS_TEXT intent across installed apps matching oald
            try {
                val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                    setType("text/plain")
                    putExtra(Intent.EXTRA_PROCESS_TEXT, word)
                    putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resolved = pm.queryIntentActivities(intent, 0)
                val oaldMatch = resolved.find { resolveInfo ->
                    val name = resolveInfo.activityInfo.packageName.lowercase()
                    name.contains("oald") || name.contains("oup.elt")
                }
                if (oaldMatch != null) {
                    intent.setClassName(oaldMatch.activityInfo.packageName, oaldMatch.activityInfo.name)
                    context.startActivity(intent)
                    launched = true
                }
            } catch (_: Exception) {}
        }

        if (launched) {
            Toast.makeText(context, "'$word' copied & opened in OALD", Toast.LENGTH_SHORT).show()
        } else {
            // Web Fallback to Oxford Learner's Dictionaries online definition
            val encodedWord = Uri.encode(word.lowercase())
            val webUrl = "https://www.oxfordlearnersdictionaries.com/definition/english/$encodedWord"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open definition for '$word'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cleanUp() {
        onScrollStateChange = null
        stopLoading()
        webChromeClient = android.webkit.WebChromeClient()
        webViewClient = android.webkit.WebViewClient()
        removeAllViews()
        destroy()
    }

    private var lastIsDarkTheme: Boolean? = null

    fun updateThemeMode(isDarkTheme: Boolean) {
        if (lastIsDarkTheme == isDarkTheme) return
        lastIsDarkTheme = isDarkTheme

        if (isDarkTheme) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }
    }

    private var accumulatedScrollY = 0

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val dy = t - oldt
        val scrollThreshold = (context.resources.displayMetrics.heightPixels / 3).coerceAtLeast(350)
        if (t <= 15) {
            accumulatedScrollY = 0
            onScrollStateChange?.invoke(true)
        } else {
            if ((dy > 0 && accumulatedScrollY < 0) || (dy < 0 && accumulatedScrollY > 0)) {
                accumulatedScrollY = 0
            }
            accumulatedScrollY += dy
            if (accumulatedScrollY >= scrollThreshold) {
                onScrollStateChange?.invoke(false)
                accumulatedScrollY = 0
            } else if (accumulatedScrollY <= -scrollThreshold) {
                onScrollStateChange?.invoke(true)
                accumulatedScrollY = 0
            }
        }
    }
}
