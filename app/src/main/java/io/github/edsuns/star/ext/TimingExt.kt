package io.github.edsuns.star.ext

import androidx.compose.runtime.MutableState
import io.github.edsuns.chaoxing.model.Course
import io.github.edsuns.chaoxing.model.Timing

/**
 * Created by Edsuns@qq.com on 2021/6/29.
 */

val Timing.needImage get() = this.type == Timing.Type.NORMAL_OR_PHOTO || this.type == Timing.Type.QRCODE

fun Timing.copy(state: Timing.State): Timing {
    val copy = Timing(course, activeId)
    copy.type = type
    copy.state = state
    return copy
}

var MutableState<MutableState<Timing>>.ref
    set(timing) {
        value.value = timing
    }
    get() = value.value

val BLANK_TIMING by lazy(LazyThreadSafetyMode.NONE) {
    val timing = Timing(Course("", "", ""), "")
    timing.type = Timing.Type.UNKNOWN
    timing.state = Timing.State.UNKNOWN
    timing
}