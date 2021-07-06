package io.github.edsuns.star.local

import android.content.Context
import androidx.core.content.edit
import io.github.edsuns.star.App

object SettingsStorage {
    private val pref = App.instance.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var username: String?
        get() = pref.getString("username", null)
        set(value) {
            pref.edit { putString("username", value) }
        }

    var address: String?
        get() = pref.getString("address", null)
        set(value) {
            pref.edit { putString("address", value) }
        }

    var latitude: String?
        get() = pref.getString("latitude", null)
        set(value) {
            pref.edit { putString("latitude", value) }
        }

    var longitude: String?
        get() = pref.getString("longitude", null)
        set(value) {
            pref.edit { putString("longitude", value) }
        }
}