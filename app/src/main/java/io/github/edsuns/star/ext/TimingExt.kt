package io.github.edsuns.star.ext

import io.github.edsuns.chaoxing.model.Timing

/**
 * Created by Edsuns@qq.com on 2021/6/29.
 */

fun Timing.copy(state: Timing.State): Timing {
    val copy = Timing(course, activeId)
    copy.type = type
    copy.state = state
    return copy
}