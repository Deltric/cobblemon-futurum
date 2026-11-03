/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.integration.jei.brewing

import com.cobblemon.mod.common.integration.jei.CobblemonJeiProvider
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import com.cobblemon.mod.common.client.cooking.ClientCookingRecipes
import com.cobblemon.mod.common.item.crafting.brewingstand.BrewingStandRecipe
import mezz.jei.api.recipe.IRecipeManager

class BrewingStandJeiProvider : CobblemonJeiProvider {
    override fun registerCategory(registration: IRecipeCategoryRegistration) {
    }

    private var sourceRecipes = emptyList<net.minecraft.world.item.crafting.RecipeHolder<*>>()
    private var displayedRecipes = emptyList<JeiBrewingStandRecipe>()

    private fun captureRecipes() {
        sourceRecipes = ClientCookingRecipes.recipes.filter { it.value is BrewingStandRecipe }
        displayedRecipes = sourceRecipes.map { JeiBrewingStandRecipe(it.value as BrewingStandRecipe, it.id.identifier()) }
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        captureRecipes()
        registration.addRecipes(RecipeTypes.BREWING, displayedRecipes)
    }

    override fun refreshRecipes(manager: IRecipeManager) {
        // RecipeHolder equality compares only the key; compare recipe values to detect changed definitions.
        val updated = ClientCookingRecipes.recipes.filter { it.value is BrewingStandRecipe }
        if (updated.map { it.value } == sourceRecipes.map { it.value }) return
        manager.hideRecipes(RecipeTypes.BREWING, displayedRecipes)
        captureRecipes()
        manager.addRecipes(RecipeTypes.BREWING, displayedRecipes)
    }

    override fun registerRecipeCatalsysts(registration: IRecipeCatalystRegistration) { }
}