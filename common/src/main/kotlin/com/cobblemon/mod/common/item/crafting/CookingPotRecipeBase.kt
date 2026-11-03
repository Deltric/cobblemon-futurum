/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting

import java.util.Optional
import net.minecraft.world.item.crafting.RecipeBookCategory
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.ItemStackTemplate
import com.cobblemon.mod.common.CobblemonRecipeCategories
import com.cobblemon.mod.common.CobblemonItems
import net.minecraft.core.HolderLookup
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.Level

interface CookingPotRecipeBase : Recipe<CraftingInput> {
    val resultTemplate: ItemStackTemplate
    val result: ItemStack get() = resultTemplate.create()
    val groupName: String
    val category: CookingPotBookCategory
    val seasoningTag: TagKey<Item>
    val seasoningProcessors: List<SeasoningProcessor>

    override fun group() = groupName
    override fun showNotification() = true
    val ingredients: List<Optional<Ingredient>>
    override fun recipeBookCategory(): RecipeBookCategory = when (category) {
        CookingPotBookCategory.FOODS -> CobblemonRecipeCategories.COOKING_POT_FOODS
        CookingPotBookCategory.MEDICINES -> CobblemonRecipeCategories.COOKING_POT_MEDICINES
        CookingPotBookCategory.COMPLEX_DISHES -> CobblemonRecipeCategories.COOKING_POT_COMPLEX_DISHES
        CookingPotBookCategory.MISC -> CobblemonRecipeCategories.COOKING_POT_MISC
    }.toVanillaCategory()
    override fun matches(input: CraftingInput, level: Level): Boolean
    fun category() = category
    fun getResultItem(registries: HolderLookup.Provider) = result

    override fun assemble(input: CraftingInput): ItemStack {
        return resultTemplate.create()
    }

    fun applySeasoning(stack: ItemStack, seasoning: List<ItemStack>) {
        for (processor in seasoningProcessors) {
            processor.apply(stack, seasoning)
        }
    }

    fun getToastSymbol(): ItemStack? {
        return ItemStack(CobblemonItems.CAMPFIRE_POT_RED)
    }

}
