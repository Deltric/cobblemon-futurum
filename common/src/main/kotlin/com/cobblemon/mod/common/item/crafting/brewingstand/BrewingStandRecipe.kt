/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting.brewingstand

import net.minecraft.world.item.crafting.RecipeBookCategories
import net.minecraft.world.item.crafting.RecipeBookCategory
import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.ItemStackTemplate
import com.cobblemon.mod.common.CobblemonRecipeSerializers
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import net.minecraft.server.level.ServerLevel
import com.cobblemon.mod.common.client.cooking.ClientCookingRecipes

class BrewingStandRecipe(
    val groupName: String,
    val input: Ingredient,
    val bottle: Ingredient,
    val resultTemplate: ItemStackTemplate
) : Recipe<BrewingStandInput> {

    override fun getType() = CobblemonRecipeTypes.BREWING_STAND
    override fun getSerializer() = CobblemonRecipeSerializers.BREWING_STAND
    val result: ItemStack get() = resultTemplate.create()
    override fun group() = groupName
    override fun showNotification() = true
    override fun placementInfo(): PlacementInfo = PlacementInfo.NOT_PLACEABLE
    override fun recipeBookCategory(): RecipeBookCategory = RecipeBookCategories.CRAFTING_MISC
    override fun assemble(input: BrewingStandInput): ItemStack = resultTemplate.create()
    fun getResultItem(registries: HolderLookup.Provider): ItemStack = resultTemplate.create()

    override fun matches(input: BrewingStandInput, level: Level): Boolean {
        val ingredientMatches = this.input.test(input.getIngredient())
        val validBottles = input.getBottles()
            .filter { !it.isEmpty }
            .all { this.bottle.test(it) }

        return ingredientMatches && validBottles
    }

    companion object {
        private fun recipes(level: Level) = if (level is ServerLevel) level.recipeAccess().recipes else ClientCookingRecipes.recipes

        fun isBottle(itemStack: ItemStack, level: Level): Boolean =
            recipes(level).any { (it.value as? BrewingStandRecipe)?.bottle?.test(itemStack) == true }

        fun isInput(itemStack: ItemStack, level: Level): Boolean =
            recipes(level).any { (it.value as? BrewingStandRecipe)?.input?.test(itemStack) == true }

        fun isBottle(itemStack: ItemStack, recipeManager: RecipeManager): Boolean {
            val recipes = recipeManager.recipes.mapNotNull { it.value() as? BrewingStandRecipe }
            return recipes.any { recipe ->
                recipe.bottle.test(itemStack)
            }
        }

        fun isInput(itemStack: ItemStack, recipeManager: RecipeManager): Boolean {
            val recipes = recipeManager.recipes.mapNotNull { it.value() as? BrewingStandRecipe }
            return recipes.any { recipe ->
                recipe.input.test(itemStack)
            }
        }
    }

    object Serializer {
            val CODEC: MapCodec<BrewingStandRecipe> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter { recipe -> recipe.groupName },
                    Ingredient.CODEC.fieldOf("input").forGetter { recipe -> recipe.input },
                    Ingredient.CODEC.fieldOf("bottle").forGetter { recipe -> recipe.bottle },
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter { recipe -> recipe.resultTemplate }
                ).apply(instance, ::BrewingStandRecipe)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BrewingStandRecipe> =
                StreamCodec.of(::toNetwork, ::fromNetwork)

            private fun fromNetwork(buffer: RegistryFriendlyByteBuf): BrewingStandRecipe {
                val group = buffer.readUtf(32767)
                val input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                val bottle = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                val result = ItemStackTemplate.STREAM_CODEC.decode(buffer)
                return BrewingStandRecipe(group, input, bottle, result)
            }

            private fun toNetwork(buffer: RegistryFriendlyByteBuf, recipe: BrewingStandRecipe) {
                buffer.writeUtf(recipe.groupName)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.bottle)
                ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.resultTemplate)
            }

        val INSTANCE = RecipeSerializer(CODEC, STREAM_CODEC)
    }
}