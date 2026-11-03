/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.color

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/** Resolves one declared model tint layer from the item's component-driven color provider. */
data class CobblemonItemTintSource(val layer: Int) : ItemTintSource {
    override fun calculate(stack: ItemStack, level: ClientLevel?, owner: LivingEntity?): Int =
        providers[stack.item]?.getColor(stack, layer) ?: -1

    override fun type(): MapCodec<out ItemTintSource> = CODEC

    companion object {
        @JvmField val CODEC: MapCodec<CobblemonItemTintSource> = Codec.intRange(0, 255).fieldOf("layer").xmap(::CobblemonItemTintSource, CobblemonItemTintSource::layer)
        private val providers = mutableMapOf<Item, CobblemonItemColorProvider>()
        fun register(provider: CobblemonItemColorProvider, vararg items: Item) {
            items.forEach { providers[it] = provider }
        }
    }
}
