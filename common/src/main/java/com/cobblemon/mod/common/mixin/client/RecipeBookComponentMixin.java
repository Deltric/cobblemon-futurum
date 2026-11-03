/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.CobblemonRecipeCategories;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    // This local category is only used to identify search tabs; the tab retains its own category.
    @ModifyExpressionValue(method = "updateTabs", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookTabButton;getCategory()Lnet/minecraft/world/item/crafting/ExtendedRecipeBookCategory;"))
    private ExtendedRecipeBookCategory cobblemon$classifyCookingSearch(ExtendedRecipeBookCategory category) {
        return category == CobblemonRecipeCategories.COOKING_POT_SEARCH.toVanillaCategory()
            ? SearchRecipeBookCategory.CRAFTING : category;
    }
}
