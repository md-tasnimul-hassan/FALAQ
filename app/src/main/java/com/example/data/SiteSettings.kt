package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_settings")
data class SiteSettings(
    @PrimaryKey val domain: String,
    val textZoom: Int = 100
)
