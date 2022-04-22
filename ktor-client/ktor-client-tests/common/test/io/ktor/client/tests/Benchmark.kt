/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import kotlin.test.*

class Benchmark {
    @Test
    fun measureEncodeToByteArray() {
        testStrings.map {
            it.encodeToByteArray()
        }
    }
}
