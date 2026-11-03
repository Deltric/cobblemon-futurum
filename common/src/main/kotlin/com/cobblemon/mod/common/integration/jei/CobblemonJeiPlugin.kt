/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.integration.jei

import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import com.cobblemon.mod.common.client.cooking.ClientCookingRecipes
import com.cobblemon.mod.common.integration.jei.berry.BerryMutationProvider
import com.cobblemon.mod.common.integration.jei.brewing.BrewingStandJeiProvider
import com.cobblemon.mod.common.integration.jei.cooking.CampfirePotJeiProvider
import com.cobblemon.mod.common.util.cobblemonResource
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.Identifier

@JeiPlugin
class CobblemonJeiPlugin : IModPlugin {
    private val jeiProviders: Set<CobblemonJeiProvider> = setOf(
        BerryMutationProvider(),
        CampfirePotJeiProvider(),
        BrewingStandJeiProvider()
    )

    override fun getPluginUid(): Identifier {
        return ID
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        jeiProviders.forEach {
            it.registerCategory(registration)
        }
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        jeiProviders.forEach {
            it.registerRecipes(registration)
        }
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        jeiProviders.forEach {
            it.registerRecipeCatalsysts(registration)
        }
    }

    private var recipeSubscription: ObservableSubscription<Unit>? = null

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        CobblemonJeiPlugin.jeiRuntime = jeiRuntime
        recipeSubscription?.unsubscribe()
        recipeSubscription = ClientCookingRecipes.changed.subscribe {
            jeiProviders.forEach { it.refreshRecipes(jeiRuntime.recipeManager) }
        }
        jeiProviders.forEach { it.refreshRecipes(jeiRuntime.recipeManager) }
    }

    override fun onRuntimeUnavailable() {
        recipeSubscription?.unsubscribe()
        recipeSubscription = null
        jeiRuntime = null
    }

    companion object {
        var jeiRuntime: IJeiRuntime? = null
        val ID = cobblemonResource("jei_plugin")
    }
}
