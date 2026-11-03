/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.Environment
import com.cobblemon.mod.common.util.server
import com.google.gson.JsonElement
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.server.packs.resources.PreparableReloadListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Utility for obtaining RegistryOps with the correct client/server registry access.
 * Used when manually encoding/decoding registry-backed values (e.g., items, predicates).
 */
object RegistryOpsProvider {
    private val reloadLookup = ThreadLocal<HolderLookup.Provider>()

    /** The active reload's tags are not applied to the global registries yet. */
    fun <T> withRegistryLookup(lookup: HolderLookup.Provider, action: () -> T): T {
        val previous = reloadLookup.get()
        reloadLookup.set(lookup)
        try {
            return action()
        } finally {
            if (previous == null) reloadLookup.remove() else reloadLookup.set(previous)
        }
    }

    fun wrapReloadListener(
        listener: PreparableReloadListener,
        lookupForReload: (PreparableReloadListener.SharedState) -> HolderLookup.Provider
    ): PreparableReloadListener = object : PreparableReloadListener {
        override fun prepareSharedState(currentReload: PreparableReloadListener.SharedState) {
            withRegistryLookup(lookupForReload(currentReload)) {
                listener.prepareSharedState(currentReload)
            }
        }

        override fun reload(
            currentReload: PreparableReloadListener.SharedState,
            taskExecutor: Executor,
            preparationBarrier: PreparableReloadListener.PreparationBarrier,
            reloadExecutor: Executor
        ): CompletableFuture<Void> {
            val lookup = lookupForReload(currentReload)
            fun scoped(executor: Executor) = Executor { task ->
                executor.execute { withRegistryLookup(lookup) { task.run() } }
            }
            return withRegistryLookup(lookup) {
                listener.reload(currentReload, scoped(taskExecutor), preparationBarrier, scoped(reloadExecutor))
            }
        }

        override fun getName(): String = listener.name
    }

    fun getRegistryLookupWithDefaultFallback(): HolderLookup.Provider? {
        reloadLookup.get()?.let { return it }
        if (Cobblemon.implementation.environment() == Environment.CLIENT) {
            val minecraft = Minecraft.getInstance()
            if (minecraft != null && minecraft.level != null)
                return minecraft.level!!.registryAccess()
        }


        if (Cobblemon.implementation.environment() == Environment.SERVER) {
            val server = server()
            if (server != null)
                return server.registryAccess()
        }

        return null
    }

    fun getOpsWithDefaultFallback(): DynamicOps<JsonElement> =
        getRegistryLookupWithDefaultFallback()?.createSerializationContext(JsonOps.INSTANCE) ?: JsonOps.INSTANCE
}
