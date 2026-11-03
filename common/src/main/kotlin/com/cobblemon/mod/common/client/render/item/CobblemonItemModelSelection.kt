/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.item.PokeBallItem
import com.cobblemon.mod.common.item.PokedexItem
import com.cobblemon.mod.common.item.WearableItem
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

object CobblemonItemModelSelection {
    @JvmStatic
    fun select(original: Identifier?, stack: ItemStack, context: ItemDisplayContext, owner: ItemOwner?): Identifier? {
        // Commands, packs and callers such as the lectern may explicitly choose an item definition.
        if (original != stack.prototype.get(DataComponents.ITEM_MODEL)) return original
        val item = stack.item
        val isFlat = context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED
        return when (item) {
            is PokeBallItem -> if (isFlat) item.pokeBall.model2d else item.pokeBall.model3d
            is PokedexItem -> if (isFlat) item.type.getItemSpritePath() else {
                val entity = owner?.asLivingEntity()
                val scanning = entity is Player && entity.useItem === stack && entity.isUsingItem
                val canOpen = entity != null && ((entity.offhandItem === stack && entity.mainHandItem.item !is PokedexItem) || entity.mainHandItem === stack)
                item.type.getItemModelPath(if (scanning) "scanning" else if (canOpen) null else "off")
            }
            is WearableItem -> if (context == ItemDisplayContext.HEAD) item.getModel3d() else item.getModel2d()
            CobblemonItems.TECHNICAL_MACHINE -> stack.get(CobblemonItemComponents.TM_MOVE)?.let {
                cobblemonResource("tm/${it.move.elementalType.showdownId()}_tm")
            } ?: original
            else -> original
        }
    }
}
