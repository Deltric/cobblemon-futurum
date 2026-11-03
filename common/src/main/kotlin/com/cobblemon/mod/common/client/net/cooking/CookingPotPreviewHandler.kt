/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.cooking

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.block.campfirepot.CookingPotMenu
import com.cobblemon.mod.common.net.messages.client.cooking.CookingPotPreviewPacket
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey

object CookingPotPreviewHandler : ClientNetworkPacketHandler<CookingPotPreviewPacket> {
    override fun handle(packet: CookingPotPreviewPacket, client: Minecraft) {
        val menu = client.player?.containerMenu as? CookingPotMenu ?: return
        if (menu.containerId != packet.containerId) return
        menu.previewItem = packet.preview
        menu.activeSeasoningTag = packet.seasoningTag?.let { TagKey.create(Registries.ITEM, it) }
    }
}
