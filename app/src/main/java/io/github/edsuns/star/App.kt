package io.github.edsuns.star

import android.app.Application

/**
 * Created by Edsuns@qq.com on 2021/6/27.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
    }
}
