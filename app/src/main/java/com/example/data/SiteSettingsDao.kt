package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SiteSettingsDao {
    @Query("SELECT * FROM site_settings WHERE domain = :domain")
    suspend fun getSettingsForDomain(domain: String): SiteSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SiteSettings)

    @Query("DELETE FROM site_settings")
    suspend fun clearAllSettings()
}
