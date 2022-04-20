/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import kotlinx.cinterop.*
import platform.darwin.*
import platform.posix.*
import threadUtils.*

internal actual fun kill(thread: pthread_t, signal: Int): Int {
    start_operation()
    return pthread_kill(thread, signal)
}

internal actual fun setSignalHandler() {
    set_signal_handler()
}

internal actual fun getDumpedStack(): List<String> {
    while (is_done() != 0) {}

    val symbols = backtrace_symbols(callstack, stack_size)!!
    return List(stack_size) { symbols[it]!!.toKString() }
}
