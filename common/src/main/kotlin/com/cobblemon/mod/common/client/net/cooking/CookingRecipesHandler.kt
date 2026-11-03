/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.cooking

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.client.cooking.ClientCookingRecipes
import com.cobblemon.mod.common.net.messages.client.cooking.CookingRecipesPacket
import net.minecraft.client.Minecraft

object CookingRecipesHandler : ClientNetworkPacketHandler<CookingRecipesPacket> {
    override fun handle(packet: CookingRecipesPacket, client: Minecraft) {
        ClientCookingRecipes.accept(packet)
    }
}
