package io.github.edsuns.star

import io.github.edsuns.chaoxing.CXing
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.ext.logE
import io.github.edsuns.star.local.FileCookieStorage
import io.github.edsuns.star.local.SettingsStorage
import io.github.edsuns.star.util.Result
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
object Repository {
    data class TimingConfig(
        val enc: String = "",
        val address: String = "",
        val longitude: Float = -1.0f,
        val latitude: Float = -1.0f,
        val imageInput: InputStream? = null,
        val photoProvider: CXing.PhotoProvider? = null
    )

    private val cookieStorage = FileCookieStorage(App.instance)

    private var xing: CXing? = null

    /**
     * Try to initialize.
     * @return true if initialized
     */
    fun init(): Boolean {
        if (xing != null) {
            return true
        }
        val username = SettingsStorage.username ?: return false
        if (cookieStorage.hasData()) {
            xing = CXing(username, cookieStorage)
            return true
        }
        return false
    }

    suspend fun fetchAllActiveTiming(): Result<List<Timing>> = try {
        withContext(Dispatchers.IO) {
            if (xing == null) {
                return@withContext Result.Success(emptyList())
            }
            val activeTimingList = mutableListOf<Timing>()
            val allCourses = xing!!.allCourses
            val taskList = mutableListOf<Deferred<List<Timing>>>()
            for (course in allCourses) {
                taskList.add(async { xing!!.getActiveTimingList(course) })
            }
            for (task in taskList) {
                activeTimingList.addAll(task.await())
            }
            return@withContext Result.Success(activeTimingList)
        }
    } catch (err: IOException) {
        logE("FetchAllActiveTiming", err)
        Result.Error(err)
    }

    suspend fun onTimingClicked(
        timing: Timing,
        timingConfig: TimingConfig? = null
    ): Result<Boolean> = try {
        withContext(Dispatchers.IO) {
            if (xing == null || timing.state == Timing.State.SUCCESS) {
                return@withContext Result.Success(false)
            }
            // timings doesn't need config
            if (timing.type == Timing.Type.GESTURE || timing.type == Timing.Type.CODE) {
                return@withContext Result.Success(xing!!.gestureOrCodeTiming(timing))
            }

            // timings need config
            if (timingConfig != null) {
                if (timing.type == Timing.Type.QRCODE) {
                    return@withContext Result.Success(xing!!.qrcodeTiming(timing, timingConfig.enc))
                }
                if (timing.type == Timing.Type.LOCATION) {
                    return@withContext Result.Success(
                        xing!!.locationTiming(
                            timing,
                            timingConfig.address,
                            timingConfig.latitude.toString(),
                            timingConfig.longitude.toString()
                        )
                    )
                }
                if (timing.type == Timing.Type.NORMAL_OR_PHOTO) {
                    return@withContext Result.Success(
                        xing!!.normalOrPhotoTiming(timing) { timingConfig.imageInput!! }
                    )
                }
            }
            return@withContext Result.Success(false)
        }
    } catch (err: IOException) {
        logE("FetchAllActiveTiming", err)
        Result.Error(err)
    }

    suspend fun login(username: String, password: String): Result<Boolean> = try {
        withContext(Dispatchers.IO) {
            if (username.isEmpty() || password.isEmpty()) {
                return@withContext Result.Success(false)
            }
            val usernameTrim = username.trim()
            val remote = CXing(usernameTrim, cookieStorage)
            val valid = remote.login(password)
            if (valid) {
                SettingsStorage.username = usernameTrim
                xing = remote
            }
            return@withContext Result.Success(valid)
        }
    } catch (err: IOException) {
        logE("Login", err)
        Result.Error(err)
    }

    suspend fun validateLogin(): Boolean {
        if (xing == null) {
            return false
        }
        if (!xing!!.validateLogin()) {
            logout()
            return false
        }
        return true
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        xing = null
        cookieStorage.clear()
    }
}