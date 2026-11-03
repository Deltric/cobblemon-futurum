/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.client.pot.CookingQuality
import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity
import com.cobblemon.mod.common.item.interactive.PokerodItem
import com.mojang.serialization.MapCodec
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** Native numeric model properties; registered by the common client bootstrap mixin. */
object CobblemonModelPredicateRegistry {
    enum class Property : RangeSelectItemModelProperty {
        CAST, PONIGIRI_OVERLAY, APRIJUICE_QUALITY;

        val codec: MapCodec<Property> = MapCodec.unit(this)
        override fun type(): MapCodec<out RangeSelectItemModelProperty> = codec

        override fun get(stack: ItemStack, level: ClientLevel?, owner: ItemOwner?, seed: Int): Float = when (this) {
            CAST -> cast(stack, owner)
            PONIGIRI_OVERLAY -> ponigiri(stack)
            APRIJUICE_QUALITY -> aprijuice(stack)
        }
    }

    private fun cast(stack: ItemStack, owner: ItemOwner?): Float {
        val player = owner as? Player ?: return 0F
        val bobber = player.fishing as? PokeRodFishingBobberEntity ?: return 0F
        val rodId = bobber.entityData.get(PokeRodFishingBobberEntity.POKEROD_ID)
        val mainHandRod = player.mainHandItem.item as? PokerodItem
        val usingMainHand = mainHandRod != null && rodId == mainHandRod.pokeRodId.toString()
        val isMainHand = stack == player.mainHandItem
        val isOffHand = stack == player.offhandItem && !usingMainHand
        return if (isMainHand && usingMainHand || isOffHand) 1F else 0F
    }

    private fun ponigiri(stack: ItemStack): Float {
        val id = stack.get(CobblemonItemComponents.INGREDIENT)?.ingredientIds?.firstOrNull()?.toString() ?: return 0F
        return when (id) {
            "minecraft:potato" -> 0.01f
            "minecraft:beetroot" -> 0.02f
            "minecraft:carrot" -> 0.03f
            "minecraft:dried_kelp" -> 0.04f
            "cobblemon:medicinal_leek" -> 0.05f
            "minecraft:red_mushroom" -> 0.06f
            "minecraft:brown_mushroom" -> 0.07f
            "minecraft:pumpkin" -> 0.08f
            "cobblemon:rice" -> 0.09f
            "minecraft:chicken" -> 0.10f
            "minecraft:cod" -> 0.11f
            "minecraft:mutton" -> 0.12f
            "minecraft:porkchop" -> 0.13f
            "minecraft:rabbit" -> 0.14f
            "minecraft:salmon" -> 0.15f
            "minecraft:beef" -> 0.16f
            "minecraft:egg" -> 0.17f
            "minecraft:poisonous_potato" -> 0.18f
            "minecraft:rotten_flesh" -> 0.19f
            "minecraft:golden_carrot" -> 0.20f
            "cobblemon:vivichoke" -> 0.21f
            "cobblemon:tasty_tail" -> 0.22f
            "minecraft:sweet_berries" -> 0.23f
            else -> 0.0f
        }
    }

    private fun aprijuice(stack: ItemStack): Float {
        val quality = stack.get(CobblemonItemComponents.RIDE_BOOST)?.getQuality() ?: return 0F
        return when (quality) {
            CookingQuality.LOW -> 0F
            CookingQuality.MEDIUM -> 0.1F
            CookingQuality.HIGH -> 0.2F
        }
    }
}
