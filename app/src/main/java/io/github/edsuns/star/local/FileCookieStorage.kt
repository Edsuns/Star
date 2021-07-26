package io.github.edsuns.star.local

import android.content.Context
import io.github.edsuns.chaoxing.CXing
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
class FileCookieStorage(context: Context) : CXing.CookieStorage {

    private val file: File = File(context.filesDir, "cookies.json")

    private var tempCookies: Map<String, String> = emptyMap()

    init {
        if (hasData()) {
            tempCookies = Json.decodeFromString(file.readText())
        }
    }

    override fun saveCookies(username: String, cookies: Map<String, String>) {
        tempCookies = cookies
        val encodeToString = Json.encodeToString(cookies)
        file.writeText(encodeToString)
    }

    override fun getCookies(username: String): Map<String, String> = tempCookies

    fun hasData(): Boolean = file.exists()

    fun clear() {
        tempCookies = emptyMap()
        if (!file.delete()) {
            throw IOException("Failed to clear cookies!")
        }
    }
}