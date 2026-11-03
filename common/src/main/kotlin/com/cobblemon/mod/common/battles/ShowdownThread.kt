/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles

import com.cobblemon.mod.common.battles.runner.ShowdownService
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.CountDownLatch

class ShowdownThread internal constructor(serviceProvider: () -> ShowdownService) : Thread("Cobblemon Showdown") {

    constructor() : this({ ShowdownService.service })

    private val logger = LogManager.getLogger("com.cobblemon.mod.common.Cobblemon")
    private val service by lazy(serviceProvider)
    @Volatile private var startupFailure: Throwable? = null

    private val latch = CountDownLatch(1)

    private val whenReady : Queue<(ShowdownService) -> Unit> = LinkedList()

    fun launch() {
        this.start()
        this.latch.await()
        checkStartup()
        for (action in whenReady) {
            action(service)
        }
    }

    fun queue(action: (ShowdownService) -> Unit) {
        if (this.latch.count == 0L) {
            checkStartup()
            action(service)
        } else {
            this.whenReady.add(action)
        }
    }

    override fun run() {
        try {
            logger.info("Starting showdown service...")
            service.openConnection()
            logger.info("Showdown has been started!")
        } catch (failure: Throwable) {
            startupFailure = failure
        } finally {
            // Initialization errors must reach the launch thread instead of
            // leaving Minecraft waiting forever for a dead worker.
            this.latch.countDown()
        }
    }

    private fun checkStartup() {
        startupFailure?.let { throw IllegalStateException("Unable to start the Cobblemon Showdown service", it) }
    }
}
