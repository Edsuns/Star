package io.github.edsuns.star.local

import io.github.edsuns.chaoxing.CXing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
class FileCookieStorage : CXing.CookieStorage {

    private var tempCookies: MutableMap<String, String> = mutableMapOf()

    override fun saveCookies(username: String, cookies: MutableMap<String, String>) {
        tempCookies = cookies
        var encodeToString = Json.encodeToString(cookies)
    }

    override fun getCookies(username: String): MutableMap<String, String> = tempCookies

}