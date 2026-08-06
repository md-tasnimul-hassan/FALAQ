package com.example.data

import kotlinx.coroutines.flow.Flow

class ShortcutRepository(private val shortcutDao: ShortcutDao) {
    val allShortcuts: Flow<List<Shortcut>> = shortcutDao.getAllShortcuts()

    suspend fun insert(shortcut: Shortcut) = shortcutDao.insertShortcut(shortcut)
    
    suspend fun update(shortcut: Shortcut) = shortcutDao.updateShortcut(shortcut)

    suspend fun deleteById(id: Int) = shortcutDao.deleteShortcutById(id)
}
