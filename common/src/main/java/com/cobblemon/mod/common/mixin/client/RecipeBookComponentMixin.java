/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.CobblemonRecipeCategories;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    @Definition(id = "recipeBookCategories", local = @Local(type = RecipeBookCategories.class))
    @Definition(id = "FURNACE_SEARCH", field = "Lnet/minecraft/client/RecipeBookCategories;FURNACE_SEARCH:Lnet/minecraft/client/RecipeBookCategories;")
    @Expression("recipeBookCategories != FURNACE_SEARCH")
    @ModifyExpressionValue(
            method = "updateTabs",
            at = @At(value = "MIXINEXTRAS:EXPRESSION")
    )
    private boolean notCookingPotSearch(boolean original, @Local RecipeBookCategories recipeBookCategories) {
        return original && recipeBookCategories != CobblemonRecipeCategories.COOKING_POT_SEARCH.toVanillaCategory();
    }
}