/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.integration.jei.cooking

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.integration.jei.CobblemonJeiProvider
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import com.cobblemon.mod.common.client.cooking.ClientCookingRecipes
import com.cobblemon.mod.common.item.crafting.CookingPotRecipeBase
import mezz.jei.api.recipe.IRecipeManager

class CampfirePotJeiProvider : CobblemonJeiProvider {
    override fun registerCategory(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(CampfirePotRecipeCategory(registration))
    }

    private var displayedRecipes: List<CookingPotRecipeBase> = emptyList()

    private fun currentRecipes() = ClientCookingRecipes.recipes.mapNotNull { it.value as? CookingPotRecipeBase }

    override fun registerRecipes(registration: IRecipeRegistration) {
        displayedRecipes = currentRecipes()
        registration.addRecipes(CampfirePotRecipeCategory.RECIPE_TYPE, displayedRecipes)
    }

    override fun refreshRecipes(manager: IRecipeManager) {
        val updated = currentRecipes()
        if (updated == displayedRecipes) return
        manager.hideRecipes(CampfirePotRecipeCategory.RECIPE_TYPE, displayedRecipes)
        displayedRecipes = updated
        manager.addRecipes(CampfirePotRecipeCategory.RECIPE_TYPE, displayedRecipes)
    }

    override fun registerRecipeCatalsysts(registration: IRecipeCatalystRegistration) {
        CobblemonItems.campfire_pots.forEach {
            registration.addRecipeCatalyst(it, CampfirePotRecipeCategory.RECIPE_TYPE)
        }
    }
}