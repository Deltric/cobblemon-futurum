/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.cobblemon.mod.common.CobblemonEntities
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import java.util.UUID

fun CompoundTag.isPokemonEntity() : Boolean {
    return this.getStringOr("id", "") == CobblemonEntities.POKEMON_KEY.toString()
}

/** Retains the four-int UUID representation used by the removed vanilla helper. */
fun CompoundTag.putUUID(key: String, uuid: UUID) {
    putIntArray(key, UUIDUtil.uuidToIntArray(uuid))
}

/** A string UUID is deliberately not accepted by this legacy NBT accessor. */
fun CompoundTag.hasUUID(key: String): Boolean = getIntArray(key).filter { it.size == 4 }.isPresent

fun CompoundTag.getUUID(key: String): UUID {
    // The previous accessor threw for missing, wrong-type and wrong-length tags.
    // In particular, do not generate a replacement ID for a malformed save.
    val tag = get(key)!!
    require(tag is IntArrayTag) { "Expected UUID tag to be an int array, found ${tag.type.name}" }
    val parts = tag.asIntArray
    require(parts.size == 4) { "Expected UUID int array length 4, found ${parts.size}" }
    return UUIDUtil.uuidFromIntArray(parts)
}

/** Preserves exact type checks and the old numeric wildcard (99). */
fun CompoundTag.contains(key: String, type: Int): Boolean {
    val actualType = get(key)?.id?.toInt() ?: Tag.TAG_END.toInt()
    return actualType == type || (type == 99 && actualType in Tag.TAG_BYTE..Tag.TAG_DOUBLE)
}

/**
 * Old NBT lists were homogeneous. Reject a wrong or mixed element type as a whole,
 * rather than silently filtering individual entries from a saved collection.
 */
fun CompoundTag.getList(key: String, elementType: Int): ListTag {
    val list = getListOrEmpty(key)
    return if (list.all { it.id.toInt() == elementType }) list else ListTag()
}
