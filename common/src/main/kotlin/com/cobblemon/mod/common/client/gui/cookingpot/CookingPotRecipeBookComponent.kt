/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.cookingpot

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonRecipeCategories
import com.cobblemon.mod.common.block.campfirepot.CookingPotMenu
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.recipebook.GhostSlots
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent
import net.minecraft.client.gui.screens.recipebook.RecipeCollection
import net.minecraft.network.chat.Component
import net.minecraft.recipebook.PlaceRecipeHelper
import net.minecraft.resources.Identifier
import net.minecraft.util.context.ContextMap
import net.minecraft.world.entity.player.StackedItemContents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay

class CookingPotRecipeBookComponent(menu: CookingPotMenu) : RecipeBookComponent<CookingPotMenu>(menu, listOf(
    TabInfo(Items.COMPASS, CobblemonRecipeCategories.COOKING_POT_SEARCH.toVanillaCategory()),
    TabInfo(CobblemonItems.LEEK_AND_POTATO_STEW, CobblemonRecipeCategories.COOKING_POT_FOODS.toVanillaCategory()),
    TabInfo(CobblemonItems.PROTEIN, CobblemonRecipeCategories.COOKING_POT_MISC.toVanillaCategory()),
    TabInfo(CobblemonItems.POTION, CobblemonRecipeCategories.COOKING_POT_MEDICINES.toVanillaCategory()),
    TabInfo(CobblemonItems.APRIJUICE_RED, CobblemonRecipeCategories.COOKING_POT_COMPLEX_DISHES.toVanillaCategory())
)) {
    override fun getFilterButtonTextures() = FILTER_BUTTON_SPRITES
    override fun getRecipeFilterName(): Component = Component.translatable("cobblemon.container.campfire_pot.recipe_book.toggle_recipes")
    override fun isCraftingSlot(slot: Slot) = slot === menu.resultSlot || slot in menu.inputGridSlots

    override fun selectMatchingRecipes(collection: RecipeCollection, stackedContents: StackedItemContents) {
        collection.selectRecipes(stackedContents) { display ->
            when (display) {
                is ShapedCraftingRecipeDisplay -> display.width() <= menu.gridWidth && display.height() <= menu.gridHeight
                is ShapelessCraftingRecipeDisplay -> display.ingredients().size <= menu.gridWidth * menu.gridHeight
                else -> false
            }
        }
    }

    override fun fillGhostRecipe(ghostSlots: GhostSlots, recipe: RecipeDisplay, context: ContextMap) {
        ghostSlots.setResult(menu.resultSlot, context, recipe.result())
        val slots = menu.inputGridSlots
        when (recipe) {
            is ShapedCraftingRecipeDisplay -> PlaceRecipeHelper.placeRecipe(
                menu.gridWidth, menu.gridHeight, recipe.width(), recipe.height(), recipe.ingredients()
            ) { ingredient, gridIndex, _, _ -> ghostSlots.setInput(slots[gridIndex], context, ingredient) }
            is ShapelessCraftingRecipeDisplay -> recipe.ingredients().take(slots.size).forEachIndexed { index, ingredient ->
                ghostSlots.setInput(slots[index], context, ingredient)
            }
        }
    }

    companion object {
        private val FILTER_BUTTON_SPRITES = WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/campfire_pot_filter_disabled_highlighted")
        )
    }
}
