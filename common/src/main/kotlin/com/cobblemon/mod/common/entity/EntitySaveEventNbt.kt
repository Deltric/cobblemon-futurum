/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Dynamic
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.NbtOps
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import java.util.UUID
import java.util.Optional
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.storage.ValueInput

/** Root NBT access is confined to the existing mutable Pokémon save/load event API. */
internal object EntitySaveEventNbt {
    val CODEC: MapCodec<CompoundTag> = MapCodec.assumeMapUnsafe(CompoundTag.CODEC)
}

/** Test presence independently of type, preserving optional legacy fields' wrong-type defaults. */
internal fun ValueInput.hasValue(key: String): Boolean = read(key, Codec.PASSTHROUGH).isPresent

/** A malformed mixed list used to reject the whole list, not retain its valid entries. */
internal val ENTITY_STRING_LIST_CODEC: Codec<List<String>> = Codec.PASSTHROUGH.comapFlatMap(
    { dynamic ->
        val tag = dynamic.convert(NbtOps.INSTANCE).value
        if (tag is ListTag && tag.all { it is StringTag }) {
            DataResult.success(tag.map { (it as StringTag).value() })
        } else {
            DataResult.error { "Expected a list containing only strings" }
        }
    },
    { values -> Dynamic(NbtOps.INSTANCE, ListTag().also { list -> values.forEach { list.add(StringTag.valueOf(it)) } }) }
)

/** Entity UUID fields historically require exactly four integers, without string/list coercion. */
internal fun ValueInput.readEntityUUID(key: String): Optional<UUID> =
    getIntArray(key).filter { it.size == 4 }.map(UUIDUtil::uuidFromIntArray)

internal fun ValueInput.readEntityBlockPos(key: String): Optional<BlockPos> =
    getIntArray(key).filter { it.size == 3 }.map { BlockPos(it[0], it[1], it[2]) }
