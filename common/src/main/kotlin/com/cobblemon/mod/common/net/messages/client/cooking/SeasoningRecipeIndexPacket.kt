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
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.readList
import com.cobblemon.mod.common.util.readMap
import com.cobblemon.mod.common.util.writeCollection
import com.cobblemon.mod.common.util.writeIdentifier
import com.cobblemon.mod.common.util.writeMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.RecipeManager

/** Tooltip projection of server recipes; independent of the player's recipe-book unlocks. */
class SeasoningRecipeIndexPacket(processorsByItem: Map<Identifier, Set<String>>) : NetworkPacket<SeasoningRecipeIndexPacket> {
    val processorsByItem: Map<Identifier, Set<String>> = processorsByItem.mapValues { (_, types) -> types.toSet() }
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeMap(processorsByItem, { buf, item -> buf.writeIdentifier(item) }) { buf, types ->
            buf.writeCollection(types) { inner, type -> inner.writeUtf(type) }
        }
    }

    companion object {
        val ID = cobblemonResource("seasoning_recipe_index")

        fun decode(buffer: RegistryFriendlyByteBuf) = SeasoningRecipeIndexPacket(
            buffer.readMap({ it.readIdentifier() }, { it.readList { inner -> inner.readUtf() }.toSet() })
        )

        fun fromRecipes(recipes: RecipeManager): SeasoningRecipeIndexPacket {
            val index = linkedMapOf<Identifier, MutableSet<String>>()
            recipes.recipes.forEach { holder ->
                val recipe = holder.value as? CookingPotRecipeBase ?: return@forEach
                val result = BuiltInRegistries.ITEM.getKey(recipe.result.item)
                index.getOrPut(result) { linkedSetOf() }.addAll(recipe.seasoningProcessors.map { it.type })
            }
            return SeasoningRecipeIndexPacket(index)
        }
    }
}
