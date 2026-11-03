/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.CobblemonRecipeCategories;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {
    @Shadow private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab;

    @Inject(method = "rebuildCollections", at = @At("RETURN"))
    private void cobblemon$aggregateCookingRecipes(CallbackInfo ci) {
        Map<ExtendedRecipeBookCategory, List<RecipeCollection>> categories = new HashMap<>(collectionsByTab);
        CobblemonRecipeCategories.Companion.getCustomAggregateCategories().forEach((search, included) ->
            categories.put(search, included.stream().flatMap(category ->
                collectionsByTab.getOrDefault(category, List.of()).stream()).toList()));
        collectionsByTab = Map.copyOf(categories);
    }
}
