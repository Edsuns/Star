package io.github.edsuns.star

import io.github.edsuns.chaoxing.CXing
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.ext.logE
import io.github.edsuns.star.local.FileCookieStorage
import io.github.edsuns.star.local.SettingsStorage
import io.github.edsuns.star.util.Result
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
object Repository {
    data class TimingConfig(
        val enc: String = "",
        val address: String = "",
        val imageInput: InputStream? = null,
        val photoProvider: CXing.PhotoProvider? = null
    )

    private val cookieStorage = FileCookieStorage(App.instance)

    private var _xing: CXing? = null
    val xing: CXing
        get() = _xing!!

    fun initialized(): Boolean = _xing != null

    fun init(): Boolean {
        val username = SettingsStorage.username ?: return false
        if (cookieStorage.hasData()) {
            _xing = CXing(username, cookieStorage)
            return true
        }
        return false
    }

    @Throws(IOException::class)
    suspend fun fetchAllActiveTiming(): Result<List<Timing>> = withContext(Dispatchers.IO) {
        val activeTimingList = mutableListOf<Timing>()
        val allCourses = xing.allCourses
        val taskList = mutableListOf<Deferred<List<Timing>>>()
        for (course in allCourses) {
            taskList.add(async { xing.getActiveTimingList(course) })
        }
        for (task in taskList) {
            activeTimingList.addAll(task.await())
        }
        return@withContext Result.Success(activeTimingList)
    }

    @Throws(IOException::class)
    suspend fun onTimingClicked(timing: Timing, timingConfig: TimingConfig? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (timing.state == Timing.State.SUCCESS) {
                return@withContext false
            }
//            if (timing.type == Timing.Type.QRCODE) {
//                return@withContext xing.qrcodeTiming(timing, timingConfig!!.enc)
//            }
            if (timing.type == Timing.Type.GESTURE) {
                return@withContext xing.gestureTiming(timing)
            }
//            if (timing.type == Timing.Type.LOCATION) {
//                return@withContext xing.locationTiming(timing, timingConfig!!.address, null, null)
//            }
//            if (timing.type == Timing.Type.NORMAL_OR_PHOTO) {
//                return@withContext xing.normalOrPhotoTiming(timing, timingConfig!!.photoProvider!!)
//            }
            return@withContext false
        }

    suspend fun login(username: String, password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            if (username.isEmpty() || password.isEmpty()) {
                return@withContext Result.Success(false)
            }
            try {
                val remote = CXing(username, cookieStorage)
                val valid = remote.login(password)
                if (valid) {
                    SettingsStorage.username = username
                    _xing = remote
                }
                return@withContext Result.Success(valid)
            } catch (err: IOException) {
                logE("Login", err)
                return@withContext Result.Error(err)
            }
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        _xing = null
        cookieStorage.clear()
    }
}