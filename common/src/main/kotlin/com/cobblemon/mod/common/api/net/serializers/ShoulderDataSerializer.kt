/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.net.serializers

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.resources.Identifier

/** Client shoulder state retains the saved vanilla compound schema and deep-copy semantics. */
object ShoulderDataSerializer : EntityDataSerializer<CompoundTag> {
    val ID: Identifier = Identifier.fromNamespaceAndPath("cobblemon", "shoulder_data")

    override fun codec(): StreamCodec<in RegistryFriendlyByteBuf, CompoundTag> = ByteBufCodecs.COMPOUND_TAG
    override fun copy(value: CompoundTag): CompoundTag = value.copy()
}
