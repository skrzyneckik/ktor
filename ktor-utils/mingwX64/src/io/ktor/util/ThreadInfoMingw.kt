/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import platform.posix.*

internal actual val SIGNAL_NUMBER: Int = -1

internal actual fun kill(thread: pthread_t, signal: Int): Int {
    return -1
}

internal actual fun setSignalHandler() = Unit

internal actual fun getDumpedStack(): List<String> = emptyList()
