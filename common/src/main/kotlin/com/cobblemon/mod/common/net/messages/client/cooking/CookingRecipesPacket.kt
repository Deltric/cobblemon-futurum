/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.cooking

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.item.crafting.CookingPotRecipeBase
import com.cobblemon.mod.common.item.crafting.brewingstand.BrewingStandRecipe
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager

/** Full custom recipes for recipe-viewer integrations, independent of recipe-book unlocks. */
class CookingRecipesPacket(val first: Boolean, val last: Boolean, val recipes: List<RecipeHolder<*>>) : NetworkPacket<CookingRecipesPacket> {
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        require(recipes.size <= CHUNK_SIZE) { "Invalid cooking recipe chunk size: ${recipes.size}" }
        buffer.writeBoolean(first)
        buffer.writeBoolean(last)
        buffer.writeVarInt(recipes.size)
        recipes.forEach { CookingRecipeHolderCodec.INSTANCE.encode(buffer, it) }
    }

    companion object {
        val ID = cobblemonResource("cooking_recipes")
        private const val CHUNK_SIZE = 64

        fun decode(buffer: RegistryFriendlyByteBuf): CookingRecipesPacket {
            val first = buffer.readBoolean()
            val last = buffer.readBoolean()
            val size = buffer.readVarInt()
            require(size in 0..CHUNK_SIZE) { "Invalid cooking recipe chunk size: $size" }
            return CookingRecipesPacket(first, last, List(size) { CookingRecipeHolderCodec.INSTANCE.decode(buffer) })
        }

        fun fromRecipes(manager: RecipeManager): List<CookingRecipesPacket> {
            val chunks = manager.recipes.filter { it.value is CookingPotRecipeBase || it.value is BrewingStandRecipe }
                .chunked(CHUNK_SIZE).ifEmpty { listOf(emptyList()) }
            return chunks.mapIndexed { index, recipes -> CookingRecipesPacket(index == 0, index == chunks.lastIndex, recipes) }
        }
    }
}
