package io.github.edsuns.star.ext

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */

val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
    logE("$context execute error, thread: ${Thread.currentThread().name}", throwable = throwable)
}

val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)
val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

fun logD(vararg message: Any?) {
    Log.d("ChaoXing", message.contentDeepToString())
}

fun logE(vararg message: Any?, throwable: Throwable = Throwable()) {
    Log.e("ChaoXing", message.contentDeepToString(), throwable)
}