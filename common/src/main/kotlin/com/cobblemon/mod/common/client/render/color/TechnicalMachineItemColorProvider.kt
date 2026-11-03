/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.color

import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.item.components.TMMoveComponent.Companion.getTMMove
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemStack

object TechnicalMachineItemColorProvider : CobblemonItemColorProvider {
    override fun getColor(itemStack: ItemStack, layer: Int): Int {
        val moveType = getTMMove(itemStack)?.elementalType ?: ElementalTypes.NORMAL
        val color = when (layer) {
            0 -> moveType.primaryColor
            1 -> moveType.secondaryColor
            else -> 0xFFFFFF
        }
        return ARGB.opaque(color)
    }
}