/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles

import com.cobblemon.mod.common.battles.runner.ShowdownService
import java.lang.reflect.Proxy
import java.time.Duration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShowdownThreadTest {
    @Test
    fun `successful startup runs queued and later callbacks with the opened service`() {
        var opened = 0
        val service = service { opened++ }
        val worker = ShowdownThread { service }
        val observed = mutableListOf<ShowdownService>()
        worker.queue { observed.add(it) }

        assertTimeoutPreemptively(Duration.ofSeconds(5)) { worker.launch() }
        worker.queue { observed.add(it) }

        assertEquals(1, opened)
        assertEquals(2, observed.size)
        assertTrue(observed.all { it === service })
    }

    @Test
    fun `engine linkage failure releases launcher and preserves its cause`() {
        val cause = NoSuchMethodError("removed Java API")
        val service = service { throw cause }
        val worker = ShowdownThread { service }
        var callbackRan = false
        worker.queue { callbackRan = true }

        assertTimeoutPreemptively(Duration.ofSeconds(5)) {
            val failure = assertThrows(IllegalStateException::class.java) { worker.launch() }
            assertSame(cause, failure.cause)
        }
        val laterFailure = assertThrows(IllegalStateException::class.java) { worker.queue { callbackRan = true } }
        assertSame(cause, laterFailure.cause)
        assertFalse(callbackRan)
    }

    @Test
    fun `service initialization failure also releases the launcher`() {
        val cause = ExceptionInInitializerError("service initialization")
        val worker = ShowdownThread { throw cause }
        assertTimeoutPreemptively(Duration.ofSeconds(5)) {
            val failure = assertThrows(IllegalStateException::class.java) { worker.launch() }
            assertSame(cause, failure.cause)
        }
    }

    private fun service(open: () -> Unit): ShowdownService = Proxy.newProxyInstance(
        ShowdownService::class.java.classLoader,
        arrayOf(ShowdownService::class.java)
    ) { _, method, _ ->
        check(method.name == "openConnection") { "Unexpected service call: ${method.name}" }
        open()
        null
    } as ShowdownService
}
