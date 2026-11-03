/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.brewing;

import com.cobblemon.mod.common.CobblemonRecipeTypes;
import com.cobblemon.mod.common.item.crafting.brewingstand.BrewingStandInput;
import com.cobblemon.mod.common.item.crafting.brewingstand.BrewingStandRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;doBrew(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V"))
    private static void cobblemon$doBrew(ServerLevel level, BlockPos pos, BrewingStandBlockEntity stand, Operation<Void> original) {
        var recipe = cobblemon$fetchBrewingRecipe(stand, level);
        if (recipe == null) {
            original.call(level, pos, stand);
            return;
        }
        for (int slot = 0; slot < 3; slot++) {
            if (!stand.getItem(slot).isEmpty()) {
                stand.setItem(slot, recipe.getResult());
            }
        }
        ItemStack ingredient = stand.getItem(3);
        ItemStackTemplate remainder = ingredient.getItem().getCraftingRemainder();
        ingredient.shrink(1);
        if (remainder != null) {
            if (ingredient.isEmpty()) ingredient = remainder.create();
            else Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
        }
        stand.setItem(3, ingredient);
        level.levelEvent(LevelEvent.SOUND_BREWING_STAND_BREW, pos, 0);
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;isBrewable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)Z"))
    private static boolean cobblemon$isBrewable(ServerLevel level, BrewingStandBlockEntity stand, Operation<Boolean> original) {
        return cobblemon$fetchBrewingRecipe(stand, level) != null || original.call(level, stand);
    }

    @Unique
    private static BrewingStandRecipe cobblemon$fetchBrewingRecipe(BrewingStandBlockEntity stand, ServerLevel level) {
        ItemStack ingredient = stand.getItem(3);
        List<ItemStack> bottles = List.of(stand.getItem(0), stand.getItem(1), stand.getItem(2));
        if (ingredient.isEmpty() || bottles.stream().allMatch(ItemStack::isEmpty)) return null;
        var input = new BrewingStandInput(ingredient, bottles);
        return level.recipeAccess().getRecipeFor(CobblemonRecipeTypes.INSTANCE.getBREWING_STAND(), input, level)
            .map(RecipeHolder::value).orElse(null);
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void cobblemon$canPlaceItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) return;
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) (Object) this;
        Level level = stand.getLevel();
        if (level == null) return;
        if (slot == 3 && BrewingStandRecipe.Companion.isInput(stack, level)) {
            cir.setReturnValue(true);
        } else if (slot >= 0 && slot < 3 && BrewingStandRecipe.Companion.isBottle(stack, level)) {
            cir.setReturnValue(stand.getItem(slot).isEmpty());
        }
    }
}
