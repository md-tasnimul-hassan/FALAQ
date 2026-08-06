package com.example.ui

import androidx.compose.foundation.layout.systemBarsPadding
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logic.UrlHandler
import com.example.ui.components.CustomWebView
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progressAnim"
    )
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val historyItems by viewModel.history.collectAsStateWithLifecycle()
    var showTabsDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var longPressMenuData by remember { mutableStateOf<LongPressMenuData?>(null) }
    var urlInput by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    
    var filePathCallback by remember { mutableStateOf<android.webkit.ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            filePathCallback?.onReceiveValue(uris)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    LaunchedEffect(activeTabId, currentUrl) {
        urlInput = TextFieldValue(currentUrl)
        isSearchFocused = false
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pauseAllWebViews()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.resumeActiveWebView()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // Auto-hide bar visibility state
    var isBarsVisible by remember { mutableStateOf(true) }

    // Full screen video support
    var customVideoView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val webChromeClient = remember {
        object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.setProgress(newProgress / 100f)
                if (newProgress >= 30) {
                    com.example.ui.components.NavigationAnimator.onContentReady(view)
                }
                if (newProgress == 100) {
                    viewModel.setLoading(false)
                } else {
                    viewModel.setLoading(true)
                }
            }

            override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
            }

            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                if (customVideoView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customVideoView = view
                customViewCallback = callback
                isBarsVisible = false
            }

            override fun onHideCustomView() {
                customViewCallback?.onCustomViewHidden()
                customVideoView = null
                customViewCallback = null
                isBarsVisible = true
            }

            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                try {
                    val intent = fileChooserParams?.createIntent()
                    if (intent != null) {
                        fileChooserLauncher.launch(intent)
                    } else {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                        return false
                    }
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    return false
                }
                return true
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                return false
            }
        }
    }

    // Manage system navigation bar visibility for full-screen video
    val activity = context as? android.app.Activity
    val window = activity?.window
    LaunchedEffect(customVideoView) {
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (customVideoView != null) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val topBarHeight = 42.dp
    val bottomBarHeight = 44.dp
    val isNewTab = currentUrl.isEmpty()

    val animatedTopHeight by animateDpAsState(
        targetValue = if (isBarsVisible) topBarHeight else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "topBarAnim"
    )

    val animatedBottomHeight by animateDpAsState(
        targetValue = if (isBarsVisible) bottomBarHeight else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "bottomBarAnim"
    )

    LaunchedEffect(currentUrl) {
        urlInput = TextFieldValue(currentUrl)
        isBarsVisible = true
        isSearchFocused = false
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val canGoBackInWebView = viewModel.currentActiveWebView?.canGoBack() == true
    BackHandler(enabled = customVideoView != null || canGoBackInWebView || currentUrl.isNotEmpty() || isSearchFocused) {
        if (customVideoView != null) {
            webChromeClient.onHideCustomView()
        } else if (isSearchFocused && currentUrl.isEmpty()) {
            isSearchFocused = false
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            viewModel.goBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (customVideoView != null) {
            androidx.compose.runtime.key(customVideoView) {
                Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        (customVideoView!!.parent as? android.view.ViewGroup)?.removeView(customVideoView)
                        android.widget.FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            addView(
                                customVideoView,
                                android.widget.FrameLayout.LayoutParams(
                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    onRelease = {
                        (customVideoView?.parent as? android.view.ViewGroup)?.removeView(customVideoView)
                    }
                )
            }
        }
    } else {
        Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Compact Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTopHeight)
                    .clipToBounds()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (urlInput.text.isEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Search or type web address",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1
                                        )
                                    }
                                }
                                BasicTextField(
                                    value = urlInput,
                                    onValueChange = { newValue -> 
                                        if (newValue.text.contains("\n")) {
                                            isSearchFocused = false
                                            viewModel.loadUrl(newValue.text.replace("\n", ""))
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        } else {
                                            urlInput = newValue
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                isSearchFocused = true
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(100)
                                                    urlInput = urlInput.copy(selection = TextRange(0, urlInput.text.length))
                                                }
                                            }
                                        },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(
                                        onGo = { 
                                            isSearchFocused = false
                                            viewModel.loadUrl(urlInput.text)
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                        onSearch = {
                                            isSearchFocused = false
                                            viewModel.loadUrl(urlInput.text)
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                        onDone = {
                                            isSearchFocused = false
                                            viewModel.loadUrl(urlInput.text)
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
                
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(activeTabId) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            android.widget.FrameLayout(ctx).apply {
                                tag = "WEB_VIEW_CONTAINER"
                            }
                        },
                        update = { frameLayout ->
                            val currentBoundTabId = activeTabId
                            var webView = viewModel.getOrCreateWebView(frameLayout.context, currentBoundTabId) { newWebView ->
                                newWebView.apply {
                                    updateThemeMode(isDarkTheme)
                                    this.webChromeClient = webChromeClient
                                    
                                    onScrollStateChange = { show ->
                                        if (isBarsVisible != show) {
                                            isBarsVisible = show
                                        }
                                    }

                                    setOnLongClickListener { v ->
                                        val webView = v as? WebView ?: return@setOnLongClickListener false
                                        val result = webView.hitTestResult
                                        val type = result.type
                                        val extra = result.extra

                                        when (type) {
                                            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                                if (!extra.isNullOrBlank()) {
                                                    longPressMenuData = LongPressMenuData(
                                                        title = extra,
                                                        linkUrl = extra,
                                                        imageUrl = null
                                                    )
                                                    true
                                                } else false
                                            }
                                            WebView.HitTestResult.IMAGE_TYPE -> {
                                                if (!extra.isNullOrBlank()) {
                                                    longPressMenuData = LongPressMenuData(
                                                        title = extra,
                                                        linkUrl = null,
                                                        imageUrl = extra
                                                    )
                                                    true
                                                } else false
                                            }
                                            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                                val handler = object : android.os.Handler(android.os.Looper.getMainLooper()) {
                                                    override fun handleMessage(msg: android.os.Message) {
                                                        val linkUrl = msg.data.getString("url")
                                                        val imageUrl = msg.data.getString("src") ?: extra
                                                        val title = linkUrl ?: imageUrl ?: extra ?: ""
                                                        if (title.isNotBlank()) {
                                                            longPressMenuData = LongPressMenuData(
                                                                title = title,
                                                                linkUrl = linkUrl,
                                                                imageUrl = imageUrl
                                                            )
                                                        }
                                                    }
                                                }
                                                val msg = handler.obtainMessage()
                                                webView.requestFocusNodeHref(msg)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            if (url != "about:blank" && currentBoundTabId == viewModel.activeTabId.value) {
                                                urlInput = TextFieldValue(url)
                                            }
                                        }

                                        override fun onPageCommitVisible(view: WebView, url: String) {
                                            super.onPageCommitVisible(view, url)
                                            com.example.ui.components.NavigationAnimator.onContentReady(view)
                                        }

                                        override fun onPageFinished(view: WebView, url: String) {
                                            super.onPageFinished(view, url)
                                            com.example.ui.components.NavigationAnimator.onContentReady(view)
                                            if (currentBoundTabId == viewModel.activeTabId.value) {
                                                viewModel.setLoading(false)
                                            }
                                            if (url != "about:blank") {
                                                viewModel.addHistory(view.title ?: "Unknown", url)
                                                viewModel.updateTabUrlAndTitle(currentBoundTabId, url, view.title ?: "New Tab")
                                            }
                                        }

                                        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                            super.onReceivedError(view, request, error)
                                            com.example.ui.components.NavigationAnimator.onContentReady(view)
                                            if (currentBoundTabId == viewModel.activeTabId.value) {
                                                viewModel.setLoading(false)
                                            }
                                        }

                                        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                                            super.doUpdateVisitedHistory(view, url, isReload)
                                            if (url != "about:blank") {
                                                viewModel.updateTabUrlAndTitle(currentBoundTabId, url)
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                            val url = request.url.toString()
                                            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:") && !url.startsWith("data:") && !url.startsWith("blob:")) {
                                                try {
                                                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                                    if (intent != null) {
                                                        frameLayout.context.startActivity(intent)
                                                        return true
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                return true
                                            }

                                            if (UrlHandler.isBlocked(url)) {
                                                val html = """
                                                    <html>
                                                    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                                                    <body style="background-color: #121212; color: #ff5252; text-align: center; font-family: sans-serif; padding: 40px 20px; margin: 0; box-sizing: border-box;">
                                                        <div style="max-width: 600px; margin: 0 auto; display: flex; flex-direction: column; justify-content: center; min-height: 70vh;">
                                                            <h2 style="font-size: 24px; margin-bottom: 20px;">Blocked by FALAQ</h2>
                                                            <p style="font-size: 20px; line-height: 1.6;">"Tell the believing men to lower their gaze and guard their private parts. That is purer for them. Indeed, Allah is Acquainted with what they do."</p>
                                                            <p style="font-size: 16px; margin-top: 10px;">— Quran 24:30 (Sahih International)</p>
                                                        </div>
                                                    </body>
                                                    </html>
                                                """.trimIndent()
                                                view.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
                                                return true
                                            }
                                            
                                            if (request.isForMainFrame) {
                                                val container = view.parent as? android.widget.FrameLayout
                                                if (container != null) {
                                                    com.example.ui.components.NavigationAnimator.startNavigation(view, container, com.example.ui.components.NavigationAnimator.NavDirection.FORWARD)
                                                }
                                                urlInput = TextFieldValue(url)
                                            }
                                            return false
                                        }

                                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                            val url = request.url.toString()
                                            if (request.isForMainFrame && UrlHandler.isBlocked(url)) {
                                                val html = """
                                                    <html>
                                                    <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                                                    <body style="background-color: #121212; color: #ff5252; text-align: center; font-family: sans-serif; padding: 40px 20px; margin: 0; box-sizing: border-box;">
                                                        <div style="max-width: 600px; margin: 0 auto; display: flex; flex-direction: column; justify-content: center; min-height: 70vh;">
                                                            <h2 style="font-size: 24px; margin-bottom: 20px;">Blocked by FALAQ</h2>
                                                            <p style="font-size: 20px; line-height: 1.6;">"Tell the believing men to lower their gaze and guard their private parts. That is purer for them. Indeed, Allah is Acquainted with what they do."</p>
                                                            <p style="font-size: 16px; margin-top: 10px;">— Quran 24:30 (Sahih International)</p>
                                                        </div>
                                                    </body>
                                                    </html>
                                                """.trimIndent()
                                                return WebResourceResponse("text/html", "UTF-8", java.io.ByteArrayInputStream(html.toByteArray()))
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }
                                    }

                                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                        if (mimetype == "application/vnd.android.package-archive" || url.lowercase().endsWith(".apk")) {
                                            Toast.makeText(frameLayout.context, "APK Downloads Blocked by FALAQ", Toast.LENGTH_LONG).show()
                                        } else {
                                            try {
                                                val request = DownloadManager.Request(Uri.parse(url))
                                                request.setMimeType(mimetype)
                                                val cookies = CookieManager.getInstance().getCookie(url)
                                                request.addRequestHeader("cookie", cookies)
                                                request.addRequestHeader("User-Agent", userAgent)
                                                request.setDescription("Downloading file...")
                                                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                                                
                                                val dm = frameLayout.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                dm.enqueue(request)
                                                Toast.makeText(frameLayout.context, "Downloading File...", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(frameLayout.context, "Download failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (frameLayout.getChildAt(0) != webView) {
                                (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                                frameLayout.removeAllViews()
                                frameLayout.addView(webView)
                                viewModel.onWebViewAttached(webView)
                            }
                            webView.updateThemeMode(isDarkTheme)
                        }
                    )
                }
                if (currentUrl.isEmpty()) {
                    HomeScreen(
                        shortcuts = shortcuts,
                        onShortcutClick = { 
                            isSearchFocused = false
                            viewModel.loadUrl(it) 
                        },
                        onAddShortcut = { title, url -> viewModel.addShortcut(title, url) },
                        onDeleteShortcut = { viewModel.deleteShortcut(it) },
                        onEditShortcut = { viewModel.updateShortcut(it) }
                    )
                }
            }
            
            // Compact Bottom Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedBottomHeight)
                    .clipToBounds()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomBarHeight),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.goForward() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.reload() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { 
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, currentUrl)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share URL"))
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { showTabsDialog = true }, modifier = Modifier.size(36.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${tabs.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
        
        if (showMenu) {
            val currentTextSize by viewModel.textSize.collectAsStateWithLifecycle()
            ModalBottomSheet(onDismissRequest = { showMenu = false }) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Menu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Text(
                        text = "Text Size: $currentTextSize%", 
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Slider(
                        value = currentTextSize.toFloat(),
                        onValueChange = { viewModel.setTextSize(it.toInt()) },
                        valueRange = 50f..200f,
                        steps = 14,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    ListItem(
                        headlineContent = { Text("History") },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        modifier = Modifier.clickable { showMenu = false; showHistoryDialog = true }
                    )
                    ListItem(
                        headlineContent = { Text("Settings") },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                        modifier = Modifier.clickable { showMenu = false; onNavigateToSettings() }
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showTabsDialog,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.activity.compose.BackHandler(enabled = showTabsDialog) {
                showTabsDialog = false
            }
            Surface(
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showTabsDialog = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tabs (${tabs.size})",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.addNewTab()
                                    showTabsDialog = false
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Tab")
                            }
                        }
                        
                        HorizontalDivider()

                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = tabs.size,
                                key = { index -> tabs[index].id }
                            ) { index ->
                                val tab = tabs[index]
                                val isSelected = (tab.id == activeTabId)

                                val dismissState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance * 0.45f },
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.closeTab(tab.id)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = androidx.compose.animation.core.tween(300),
                                        fadeOutSpec = androidx.compose.animation.core.tween(300),
                                        placementSpec = androidx.compose.animation.core.spring(
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                                        )
                                    ),
                                    backgroundContent = {
                                        val isDismissing = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled
                                        val color = if (isDismissing) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(color)
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDismissing) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Swipe to close",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    },
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true
                                ) {
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                            .clickable {
                                                viewModel.switchTab(tab.id)
                                                showTabsDialog = false
                                            },
                                        shape = RoundedCornerShape(20.dp),
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        },
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            }
                                        )
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tab.title.ifEmpty { "New Tab" },
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 3,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.closeTab(tab.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Close Tab",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tab.url.ifEmpty { "Homepage" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 4,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = { Text("History") },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(historyItems.size) { index ->
                            val item = historyItems[index]
                            ListItem(
                                headlineContent = { Text(item.title, maxLines = 1) },
                                supportingContent = { Text(item.url, maxLines = 1) },
                                modifier = Modifier.clickable {
                                    viewModel.loadUrl(item.url)
                                    showHistoryDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear All")
                    }
                }
            )
        }

        if (longPressMenuData != null) {
            val data = longPressMenuData!!
            ModalBottomSheet(
                onDismissRequest = { longPressMenuData = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = data.title,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!data.linkUrl.isNullOrBlank()) {
                        val link = data.linkUrl
                        ListItem(
                            headlineContent = { Text("Open in new tab") },
                            leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                            modifier = Modifier.clickable {
                                longPressMenuData = null
                                viewModel.addNewTab(link)
                                Toast.makeText(context, "Opened in new tab", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Open in background tab") },
                            leadingContent = { Icon(Icons.Default.List, contentDescription = null) },
                            modifier = Modifier.clickable {
                                longPressMenuData = null
                                viewModel.openBackgroundTab(link)
                                Toast.makeText(context, "Opened in background tab", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Copy link address") },
                            leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                            modifier = Modifier.clickable {
                                longPressMenuData = null
                                copyToClipboard(context, "Link address", link)
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Share link") },
                            leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                            modifier = Modifier.clickable {
                                longPressMenuData = null
                                shareText(context, link, "Share link")
                            }
                        )
                    }

                    if (!data.imageUrl.isNullOrBlank()) {
                        val img = data.imageUrl
                        ListItem(
                            headlineContent = { Text("Save image") },
                            leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            modifier = Modifier.clickable {
                                longPressMenuData = null
                                downloadImage(context, img)
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Copy image address") },
                            leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                            modifier = Modifier.clickable {
                                longPressMenuData = null
                                copyToClipboard(context, "Image address", img)
                            }
                        )
                        if (data.linkUrl == null) {
                            ListItem(
                                headlineContent = { Text("Open image in new tab") },
                                leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    longPressMenuData = null
                                    viewModel.addNewTab(img)
                                    Toast.makeText(context, "Image opened in new tab", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LongPressMenuData(
    val title: String,
    val linkUrl: String? = null,
    val imageUrl: String? = null
)

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun downloadImage(context: Context, imageUrl: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(imageUrl)).apply {
            setMimeType("image/*")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val fileName = URLUtil.guessFileName(imageUrl, null, "image/*")
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading image...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareText(context: Context, text: String, title: String = "Share link") {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
