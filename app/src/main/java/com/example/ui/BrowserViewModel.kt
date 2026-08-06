package com.example.ui

import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.History
import com.example.data.Shortcut
import com.example.data.TabEntity
import com.example.data.toBundle
import com.example.data.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    var url: String = "",
    var title: String = "New Tab",
    var state: Bundle? = null
)

class BrowserViewModel(
    private val appContext: Context,
    private val database: AppDatabase
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

    private val _tabs = MutableStateFlow<List<BrowserTab>>(listOf(BrowserTab()))
    val tabs: StateFlow<List<BrowserTab>> = _tabs

    private val _activeTabId = MutableStateFlow<String>(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _textSize = MutableStateFlow(100)
    val textSize: StateFlow<Int> = _textSize

    val shortcuts: StateFlow<List<Shortcut>> = database.shortcutDao().getAllShortcuts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<History>> = database.historyDao().getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val webViewCache = mutableMapOf<String, com.example.ui.components.CustomWebView>()

    val currentActiveWebView: com.example.ui.components.CustomWebView?
        get() = webViewCache[_activeTabId.value]

    private var isDbLoaded = false
    private var pendingExternalUrl: String? = null

    init {
        viewModelScope.launch {
            val savedTabs = database.tabDao().getAllTabs()
            isDbLoaded = true
            val pending = pendingExternalUrl
            if (savedTabs.isNotEmpty()) {
                val newTabs = savedTabs.map { entity ->
                    BrowserTab(
                        id = entity.id,
                        url = entity.url,
                        title = entity.title,
                        state = entity.state?.toBundle()
                    )
                }

                val lastActiveId = prefs.getString("last_active_tab_id", null)
                val targetTab = newTabs.find { it.id == lastActiveId } ?: newTabs.first()

                if (!pending.isNullOrEmpty()) {
                    val updatedTabs = newTabs.map { tab ->
                        if (tab.id == targetTab.id) {
                            tab.copy(url = pending, title = pending, state = null)
                        } else {
                            tab
                        }
                    }
                    _tabs.value = updatedTabs
                    _activeTabId.value = targetTab.id
                    _currentUrl.value = pending
                    applySettingsForUrl(pending, targetTab.id)
                    pendingExternalUrl = null

                    val wv = webViewCache[targetTab.id]
                    wv?.loadUrl(pending)
                } else {
                    _tabs.value = newTabs
                    _activeTabId.value = targetTab.id
                    _currentUrl.value = targetTab.url
                    applySettingsForUrl(targetTab.url, targetTab.id)
                }
            } else {
                if (!pending.isNullOrEmpty()) {
                    val currentTabId = _activeTabId.value
                    _tabs.value = _tabs.value.map { tab ->
                        if (tab.id == currentTabId) {
                            tab.copy(url = pending, title = pending, state = null)
                        } else {
                            tab
                        }
                    }
                    _currentUrl.value = pending
                    applySettingsForUrl(pending, currentTabId)
                    pendingExternalUrl = null

                    val wv = webViewCache[currentTabId]
                    wv?.loadUrl(pending)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webViewCache.values.forEach { it.cleanUp() }
        webViewCache.clear()
    }

    private fun persistActiveTabId(id: String) {
        prefs.edit().putString("last_active_tab_id", id).apply()
    }

    fun updateTabUrlAndTitle(tabId: String, url: String, title: String? = null) {
        val currentTabs = _tabs.value
        var updated = false
        val newTabs = currentTabs.map { tab ->
            if (tab.id == tabId) {
                var tabCopy = tab
                if (url != "about:blank" && tab.url != url) {
                    tabCopy = tabCopy.copy(url = url)
                    updated = true
                }
                if (!title.isNullOrEmpty() && title != "about:blank" && tab.title != title) {
                    tabCopy = tabCopy.copy(title = title)
                    updated = true
                }
                tabCopy
            } else {
                tab
            }
        }
        if (updated) {
            _tabs.value = newTabs
            if (tabId == _activeTabId.value) {
                val activeTab = newTabs.find { it.id == tabId }
                if (activeTab != null) {
                    _currentUrl.value = activeTab.url
                }
            }
            applySettingsForUrl(url, tabId)
            saveTabsToDb()
        }
    }

    fun loadUrl(url: String) {
        val trimmedUrl = url.trim()
        val currentTabId = _activeTabId.value

        if (trimmedUrl.isEmpty()) {
            webViewCache[currentTabId]?.let { webView ->
                val bundle = Bundle()
                webView.saveState(bundle)
                updateTabState(currentTabId, bundle, "")
            }
            _currentUrl.value = ""
            updateTabUrlAndTitle(currentTabId, "")
            webViewCache[currentTabId]?.loadUrl("about:blank")
            return
        }

        var finalUrl = trimmedUrl
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            if (trimmedUrl.contains(".") && !trimmedUrl.contains(" ")) {
                finalUrl = "https://$trimmedUrl"
            } else {
                val encoded = android.net.Uri.encode(trimmedUrl)
                finalUrl = "https://www.google.com/search?q=$encoded"
            }
        }
        finalUrl = com.example.logic.UrlHandler.injectSafeSearch(finalUrl)

        if (!isDbLoaded) {
            pendingExternalUrl = finalUrl
        }

        _currentUrl.value = finalUrl
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == currentTabId) {
                tab.copy(url = finalUrl, title = finalUrl, state = null)
            } else {
                tab
            }
        }
        applySettingsForUrl(finalUrl, currentTabId)
        saveTabsToDb()

        val wv = webViewCache[currentTabId]
        if (wv != null) {
            val container = wv.parent as? android.widget.FrameLayout
            if (container != null) {
                com.example.ui.components.NavigationAnimator.startNavigation(wv, container, com.example.ui.components.NavigationAnimator.NavDirection.FORWARD)
            }
            wv.loadUrl(finalUrl)
        }
    }

    private fun updateTabState(tabId: String, stateBundle: Bundle, url: String) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(state = stateBundle, url = if (url.isNotEmpty()) url else tab.url)
            } else {
                tab
            }
        }
        saveTabsToDb()
    }

    fun goBack() {
        val currentTabId = _activeTabId.value
        val wv = webViewCache[currentTabId]
        if (wv?.canGoBack() == true) {
            val container = wv.parent as? android.widget.FrameLayout
            if (container != null) {
                com.example.ui.components.NavigationAnimator.startNavigation(wv, container, com.example.ui.components.NavigationAnimator.NavDirection.BACK)
            }
            wv.goBack()
        } else {
            _currentUrl.value = ""
            updateTabUrlAndTitle(currentTabId, "")
            wv?.loadUrl("about:blank")
        }
    }

    fun goForward() {
        webViewCache[_activeTabId.value]?.let { wv ->
            if (wv.canGoForward()) {
                val container = wv.parent as? android.widget.FrameLayout
                if (container != null) {
                    com.example.ui.components.NavigationAnimator.startNavigation(wv, container, com.example.ui.components.NavigationAnimator.NavDirection.FORWARD)
                }
                wv.goForward()
            }
        }
    }

    fun reload() {
        webViewCache[_activeTabId.value]?.reload()
    }

    private fun getDomain(url: String): String? {
        return try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                android.net.Uri.parse(url).host
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun applySettingsForUrl(url: String, tabId: String) {
        val domain = getDomain(url)
        if (domain != null) {
            viewModelScope.launch {
                val settings = database.siteSettingsDao().getSettingsForDomain(domain)
                val size = settings?.textZoom ?: 100
                if (tabId == _activeTabId.value) {
                    _textSize.value = size
                }
                webViewCache[tabId]?.settings?.textZoom = size
            }
        } else {
            if (tabId == _activeTabId.value) {
                _textSize.value = 100
            }
            webViewCache[tabId]?.settings?.textZoom = 100
        }
    }

    fun clearSearchHistory() {
        webViewCache[_activeTabId.value]?.clearHistory()
    }

    fun clearAllData() {
        webViewCache.values.forEach { wv ->
            wv.clearCache(true)
            wv.clearHistory()
            wv.clearFormData()
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        viewModelScope.launch {
            database.siteSettingsDao().clearAllSettings()
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setProgress(p: Float) {
        _progress.value = p
    }

    fun setTextSize(size: Int) {
        val currentTabId = _activeTabId.value
        val domain = getDomain(_currentUrl.value)
        _textSize.value = size
        webViewCache[currentTabId]?.settings?.textZoom = size
        
        if (domain != null) {
            viewModelScope.launch {
                database.siteSettingsDao().insertOrUpdate(
                    com.example.data.SiteSettings(domain = domain, textZoom = size)
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.historyDao().clearHistory()
        }
    }

    fun addHistory(title: String, url: String) {
        viewModelScope.launch {
            database.historyDao().insert(History(title = title, url = url, timestamp = System.currentTimeMillis()))
        }
    }

    fun addNewTab(url: String = "") {
        val currentTabId = _activeTabId.value
        webViewCache[currentTabId]?.let { wv ->
            val bundle = Bundle()
            wv.saveState(bundle)
            _tabs.value.find { it.id == currentTabId }?.state = bundle
        }

        val newTab = BrowserTab(url = url)
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _currentUrl.value = url
        _isLoading.value = false
        _progress.value = 0f

        applySettingsForUrl(url, newTab.id)
        persistActiveTabId(newTab.id)
        saveTabsToDb()
    }

    fun openBackgroundTab(url: String) {
        val newTab = BrowserTab(url = url, title = url)
        _tabs.value = _tabs.value + newTab
        applySettingsForUrl(url, newTab.id)
        saveTabsToDb()
    }

    fun switchTab(tabId: String) {
        if (_activeTabId.value == tabId) return

        val currentTabId = _activeTabId.value
        webViewCache[currentTabId]?.let { wv ->
            val bundle = Bundle()
            wv.saveState(bundle)
            _tabs.value.find { it.id == currentTabId }?.state = bundle
        }

        _activeTabId.value = tabId
        persistActiveTabId(tabId)

        val targetTab = _tabs.value.find { it.id == tabId }
        if (targetTab != null) {
            _currentUrl.value = targetTab.url
            applySettingsForUrl(targetTab.url, tabId)
        }
        saveTabsToDb()
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value.toMutableList()
        webViewCache.remove(tabId)?.cleanUp()

        if (currentTabs.size <= 1) {
            val newTab = BrowserTab()
            _tabs.value = listOf(newTab)
            _activeTabId.value = newTab.id
            _currentUrl.value = ""
            applySettingsForUrl("", newTab.id)
            persistActiveTabId(newTab.id)
            saveTabsToDb()
            return
        }

        val index = currentTabs.indexOfFirst { it.id == tabId }
        if (index != -1) {
            currentTabs.removeAt(index)
            _tabs.value = currentTabs

            if (_activeTabId.value == tabId) {
                val nextTab = if (index < currentTabs.size) currentTabs[index] else currentTabs.last()
                _activeTabId.value = nextTab.id
                _currentUrl.value = nextTab.url
                applySettingsForUrl(nextTab.url, nextTab.id)
                persistActiveTabId(nextTab.id)
            }
            saveTabsToDb()
        }
    }

    fun getOrCreateWebView(
        context: Context,
        tabId: String,
        setup: (com.example.ui.components.CustomWebView) -> Unit
    ): com.example.ui.components.CustomWebView {
        return webViewCache.getOrPut(tabId) {
            val webView = com.example.ui.components.CustomWebView(context)
            webView.settings.textZoom = _textSize.value
            setup(webView)
            val currentTab = _tabs.value.find { it.id == tabId }
            if (currentTab?.state != null) {
                webView.restoreState(currentTab.state!!)
            } else if (currentTab != null && currentTab.url.isNotEmpty() && currentTab.url != "about:blank") {
                webView.loadUrl(currentTab.url)
            } else {
                webView.loadUrl("about:blank")
            }
            webView
        }
    }

    fun onWebViewAttached(webView: com.example.ui.components.CustomWebView) {
        webViewCache.values.forEach { if (it != webView) it.onPause() }
        webView.onResume()
    }

    fun pauseAllWebViews() {
        webViewCache.values.forEach { wv ->
            wv.onPause()
            wv.pauseTimers()
        }
    }

    fun resumeActiveWebView() {
        val activeWv = webViewCache[_activeTabId.value]
        if (activeWv != null) {
            activeWv.resumeTimers()
            activeWv.onResume()
        } else {
            android.webkit.WebView(appContext).resumeTimers()
        }
    }

    fun saveTabsToDb() {
        val currentTabs = _tabs.value.toList()
        val currentActiveId = _activeTabId.value
        persistActiveTabId(currentActiveId)

        // Capture webview states on main thread
        currentTabs.forEach { tab ->
            webViewCache[tab.id]?.let { wv ->
                val bundle = Bundle()
                wv.saveState(bundle)
                tab.state = bundle
            }
        }

        viewModelScope.launch {
            try {
                database.tabDao().deleteAllTabs()
                currentTabs.forEach { tab ->
                    database.tabDao().insertTab(
                        TabEntity(
                            id = tab.id,
                            url = tab.url,
                            title = tab.title,
                            state = tab.state?.toByteArray()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Shortcut Operations
    fun addShortcut(title: String, url: String) {
        viewModelScope.launch {
            database.shortcutDao().insertShortcut(Shortcut(title = title, url = url))
        }
    }

    fun updateShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            database.shortcutDao().updateShortcut(shortcut)
        }
    }

    fun deleteShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            database.shortcutDao().deleteShortcutById(shortcut.id)
        }
    }
}

class BrowserViewModelFactory(
    private val context: Context,
    private val database: AppDatabase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(context, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
