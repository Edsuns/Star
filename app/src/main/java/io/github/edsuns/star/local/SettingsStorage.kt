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
}