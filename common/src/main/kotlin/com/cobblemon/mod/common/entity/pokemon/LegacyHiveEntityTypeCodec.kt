/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import java.util.function.Supplier

/** Accept the pre-typed-data worldgen placeholder while all new saves use a registered entity type. */
object LegacyHiveEntityTypeCodec {
    const val LEGACY_ID = "cobblemon_pokemon_entity_placeholder"

    @JvmStatic
    fun <T> wrap(codec: Codec<T>, pokemonType: Supplier<T>): Codec<T> = object : Codec<T> {
        override fun <A> decode(ops: DynamicOps<A>, input: A): DataResult<Pair<T, A>> {
            if (ops.getStringValue(input).result().filter { it == LEGACY_ID || it == "minecraft:$LEGACY_ID" }.isPresent) {
                return DataResult.success(Pair.of(pokemonType.get(), ops.empty()))
            }
            return codec.decode(ops, input)
        }

        override fun <A> encode(input: T, ops: DynamicOps<A>, prefix: A): DataResult<A> =
            codec.encode(input, ops, prefix)
    }
}
