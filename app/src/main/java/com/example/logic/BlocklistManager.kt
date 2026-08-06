package com.example.logic

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

object BlocklistManager {
    private const val PREFS_NAME = "FALAQ_PREFS"
    private const val KEY_LAST_UPDATED = "last_updated_hosts"
    private const val HOSTS_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn-only/hosts"
    private const val HOSTS_FILENAME = "blocked_hosts.txt"

    @Volatile
    private var blockedDomainsArray: Array<String> = emptyArray()

    @Volatile
    private var isInitializing = false

    fun init(context: Context) {
        if (isInitializing) return
        isInitializing = true
        CoroutineScope(Dispatchers.IO).launch {
            loadLocalBlocklist(context)
            updateBlocklistIfNeeded(context)
        }
    }

    private suspend fun updateBlocklistIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastUpdated > TimeUnit.DAYS.toMillis(7) || !File(context.filesDir, HOSTS_FILENAME).exists()) {
            try {
                val url = URL(HOSTS_URL)
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val hostsContent = connection.getInputStream().bufferedReader().use { it.readText() }
                
                val file = File(context.filesDir, HOSTS_FILENAME)
                file.writeText(hostsContent)
                
                prefs.edit().putLong(KEY_LAST_UPDATED, currentTime).apply()
                loadLocalBlocklist(context)
            } catch (e: Exception) {
                // If download fails, just rely on existing file
            }
        }
    }

    private fun loadLocalBlocklist(context: Context) {
        val file = File(context.filesDir, HOSTS_FILENAME)
        if (file.exists()) {
            try {
                val newList = ArrayList<String>(60000)
                file.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val firstSpace = trimmed.indexOfFirst { it == ' ' || it == '\t' }
                            if (firstSpace != -1) {
                                val domain = trimmed.substring(firstSpace).trim().lowercase()
                                if (domain.isNotEmpty() && domain != "localhost") {
                                    newList.add(domain)
                                }
                            }
                        }
                    }
                }
                val array = newList.toTypedArray()
                array.sort()
                blockedDomainsArray = array
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isDomainBlocked(url: String): Boolean {
        val array = blockedDomainsArray
        if (array.isEmpty()) return false
        try {
            val host = getHostFromUrlFast(url) ?: return false
            
            var currentHost = host
            while (currentHost.contains(".")) {
                if (array.binarySearch(currentHost) >= 0) {
                    return true
                }
                val firstDot = currentHost.indexOf('.')
                if (firstDot == -1 || firstDot == currentHost.length - 1) break
                currentHost = currentHost.substring(firstDot + 1)
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun getHostFromUrlFast(url: String): String? {
        if (url.isEmpty()) return null
        val schemeEnd = url.indexOf("://")
        val start = if (schemeEnd != -1) schemeEnd + 3 else 0
        var end = url.indexOf('/', start)
        if (end == -1) end = url.length
        val queryStart = url.indexOf('?', start)
        if (queryStart != -1 && queryStart < end) end = queryStart
        val portStart = url.indexOf(':', start)
        if (portStart != -1 && portStart < end) end = portStart
        
        if (start >= end) return null
        return url.substring(start, end).lowercase()
    }
}
