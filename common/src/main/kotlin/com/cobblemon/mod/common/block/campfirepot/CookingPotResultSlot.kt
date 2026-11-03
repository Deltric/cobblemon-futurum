/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.campfirepot

import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.CobblemonSounds
import net.minecraft.sounds.SoundSource
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class CookingPotResultSlot(
    container: CraftingContainer,
    index: Int,
    x: Int,
    y: Int
) : Slot(container, index, x, y) {

    override fun onTake(player: Player, stack: ItemStack) {
        if (stack.has(CobblemonItemComponents.FLAVOUR)) {
            stack.set(CobblemonItemComponents.CRAFTED, true)
        }

        val menu = player.containerMenu
        if (menu is CookingPotMenu) {
            menu.broadcastChanges()
            if (player is ServerPlayer) {
                player.connection.send(ClientboundSoundPacket(Holder.direct(CobblemonSounds.CAMPFIRE_POT_TAKE_ITEM), SoundSource.MASTER,
                    player.x, player.y, player.z, 1F, 1F, player.random.nextLong()))
            }
        }

        super.onTake(player, stack)
    }

    override fun mayPlace(stack: ItemStack): Boolean = false
}