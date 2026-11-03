/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.cooking

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.writeIdentifier
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/** Server-computed seasoning preview; recipe implementations stay on the server. */
class CookingPotPreviewPacket(val containerId: Int, val preview: ItemStack, val seasoningTag: Identifier?) : NetworkPacket<CookingPotPreviewPacket> {
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeVarInt(containerId)
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, preview)
        buffer.writeNullable(seasoningTag) { buf, value -> buf.writeIdentifier(value) }
    }

    companion object {
        val ID = cobblemonResource("cooking_pot_preview")
        fun decode(buffer: RegistryFriendlyByteBuf) = CookingPotPreviewPacket(
            buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readNullable { it.readIdentifier() }
        )
    }
}
