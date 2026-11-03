/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.net.serializers

import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.resources.Identifier
import java.util.Optional
import java.util.UUID

/** For optional identifiers such as battle IDs, which do not refer to entities. */
object OptionalUUIDDataSerializer : EntityDataSerializer<Optional<UUID>> {
    val ID: Identifier = Identifier.fromNamespaceAndPath("cobblemon", "optional_uuid")

    private val CODEC: StreamCodec<RegistryFriendlyByteBuf, Optional<UUID>> =
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC)

    override fun codec(): StreamCodec<RegistryFriendlyByteBuf, Optional<UUID>> = CODEC

    override fun copy(value: Optional<UUID>): Optional<UUID> = value
}
