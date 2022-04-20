/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import io.ktor.util.collections.*
import platform.posix.*
import kotlin.native.concurrent.*

internal expect val SIGNAL_NUMBER: Int

@OptIn(ExperimentalStdlibApi::class, InternalAPI::class)
@EagerInitialization
private val init = ThreadInfo

@InternalAPI
public object ThreadInfo {
    private val threads = ConcurrentMap<Worker, pthread_t>()

    init {
        setSignalHandler()
    }

    public fun registerCurrentThread() {
        val thread = pthread_self()!!
        threads[Worker.current] = thread
    }

    public fun dropWorker(worker: Worker) {
        threads.remove(worker)
    }

    public fun getAllStackTraces(): List<WorkerStacktrace> {
        if (Platform.osFamily == OsFamily.WINDOWS) return emptyList()

        val result = mutableListOf<WorkerStacktrace>()
        for ((worker, thread) in threads.entries) {
            kill(thread, SIGNAL_NUMBER)
            result += WorkerStacktrace(worker, getDumpedStack())
        }

        return result
    }

    public fun printAllStackTraces() {
        getAllStackTraces().forEach {
            println(it.worker)
            it.stacktrace.forEach {
                println("\tat $it")
            }
        }
    }

    public fun stopAllWorkers() {
        for (worker in threads.keys) {
            if (worker == Worker.current) continue
            worker.requestTermination(processScheduledJobs = false)
        }

        threads.clear()
        registerCurrentThread()
    }
}

@InternalAPI
public class WorkerStacktrace(
    public val worker: Worker,
    public val stacktrace: List<String>
)

internal expect fun kill(thread: pthread_t, signal: Int): Int

internal expect fun setSignalHandler()

internal expect fun getDumpedStack(): List<String>
