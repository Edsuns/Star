package io.github.edsuns.star

import io.github.edsuns.chaoxing.CXing
import io.github.edsuns.chaoxing.model.Timing
import io.github.edsuns.star.ext.ioScope
import io.github.edsuns.star.local.FileCookieStorage
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
object Repository {
    var xing: CXing? = null

    fun initialized(): Boolean = xing != null

    fun init(username: String) {
        xing = CXing(username, FileCookieStorage())
    }

    fun fetchAllActiveTiming(): List<Timing> {
        val activeTimingList = mutableListOf<Timing>()
        val allCourses = xing!!.allCourses
        for (course in allCourses) {
            ioScope.launch {
                try {
                    activeTimingList.addAll(xing!!.getActiveTimingList(course))
                } catch (err: IOException) {
                }
            }
        }
        return activeTimingList
    }
}