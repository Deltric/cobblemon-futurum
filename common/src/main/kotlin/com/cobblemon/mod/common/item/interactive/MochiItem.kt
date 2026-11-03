/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.interactive

import net.minecraft.world.item.Item.Properties

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.item.CobblemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class MochiItem(properties: Properties, override val stat: Stat): CobblemonItem(properties), EVIncreaseItem {
    override val evIncreaseAmount: Int = 4

    override fun applyToPokemon(player: ServerPlayer, stack: ItemStack, pokemon: Pokemon): InteractionResult {
        if (!canUseOnPokemon(stack, pokemon)) {
            return InteractionResult.FAIL
        }

        pokemon.feedPokemon(1)

        return super.applyToPokemon(player, stack, pokemon)
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        if (user is ServerPlayer) {
            return use(user, user.getItemInHand(hand))
        }
        return InteractionResult.SUCCESS
    }

    override fun onSuccessfulApply(player: ServerPlayer, stack: ItemStack, pokemon: Pokemon) {
        pokemon.entity?.playSound(CobblemonSounds.MOCHI_USE)
        stack.consume(1, player)
    }
}