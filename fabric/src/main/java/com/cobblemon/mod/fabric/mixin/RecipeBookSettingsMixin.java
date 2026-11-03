/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.CobblemonRecipeBookTypes;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.UnaryOperator;

@Mixin(RecipeBookSettings.class)
public class RecipeBookSettingsMixin {

    @Unique
    private RecipeBookSettings.TypeSettings cobblemon$cookingPotSettings = new RecipeBookSettings.TypeSettings(true, true);

    @Inject(method = "getSettings", at = @At("HEAD"), cancellable = true)
    private void cobblemon$getCookingPotSettings(RecipeBookType type, CallbackInfoReturnable<RecipeBookSettings.TypeSettings> cir) {
        if (type.name().equals(CobblemonRecipeBookTypes.COOKING_POT_NAME)) {
            cir.setReturnValue(cobblemon$cookingPotSettings);
        }
    }

    @Inject(method = "updateSettings", at = @At("HEAD"), cancellable = true)
    private void cobblemon$updateCookingPotSettings(RecipeBookType type, UnaryOperator<RecipeBookSettings.TypeSettings> operator, CallbackInfo ci) {
        if (type.name().equals(CobblemonRecipeBookTypes.COOKING_POT_NAME)) {
            cobblemon$cookingPotSettings = operator.apply(cobblemon$cookingPotSettings);
            ci.cancel();
        }
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void cobblemon$copyCookingPotSettings(CallbackInfoReturnable<RecipeBookSettings> cir) {
        RecipeBookType type = RecipeBookType.valueOf(CobblemonRecipeBookTypes.COOKING_POT_NAME);
        RecipeBookSettings copy = cir.getReturnValue();
        copy.setOpen(type, cobblemon$cookingPotSettings.open());
        copy.setFiltering(type, cobblemon$cookingPotSettings.filtering());
    }

    @Inject(method = "replaceFrom", at = @At("TAIL"))
    private void cobblemon$replaceCookingPotSettings(RecipeBookSettings other, CallbackInfo ci) {
        cobblemon$cookingPotSettings = other.getSettings(RecipeBookType.valueOf(CobblemonRecipeBookTypes.COOKING_POT_NAME));
    }

    // Native codecs explicitly encode the four vanilla books. Leave them intact:
    // Fabric previously excluded this custom book from the packet and NBT formats.
}
