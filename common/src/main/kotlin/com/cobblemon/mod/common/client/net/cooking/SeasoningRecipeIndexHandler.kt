/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.cooking

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.client.tooltips.SeasoningRecipeIndex
import com.cobblemon.mod.common.net.messages.client.cooking.SeasoningRecipeIndexPacket
import net.minecraft.client.Minecraft

object SeasoningRecipeIndexHandler : ClientNetworkPacketHandler<SeasoningRecipeIndexPacket> {
    override fun handle(packet: SeasoningRecipeIndexPacket, client: Minecraft) {
        SeasoningRecipeIndex.replace(packet.processorsByItem)
    }
}
