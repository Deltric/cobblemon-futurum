/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters

import com.google.gson.*
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.minecraft.core.HolderLookup
import java.lang.reflect.Type

/**
 * @param registryAccess The registries to use. When null, they are resolved on each call so a reload's
 * staged lookup is used rather than whatever was current when this adapter was created.
 */
class CodecBackedAdapter<T>(val codec: Codec<T>, val registryAccess: HolderLookup.Provider? = null) : JsonDeserializer<T>, JsonSerializer<T> {

    private fun ops() = registryAccess?.createSerializationContext(JsonOps.INSTANCE) ?: RegistryOpsProvider.getOpsWithDefaultFallback()


    override fun deserialize(
        jElement: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): T {
        val ops = ops()
        val result = ops.withDecoder(codec).apply(jElement)
        return result.result().orElseThrow {
            IllegalStateException("Failed to deserialize $jElement: ${result.error().orElse(null)}")
        }.first
    }

    override fun serialize(
        src: T,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val ops = ops()
        val result = ops.withEncoder(codec).apply(src)
        return result.result().orElseThrow {
            IllegalStateException("Failed to serialize $src: ${result.error().orElse(null)}")
        }
    }
}