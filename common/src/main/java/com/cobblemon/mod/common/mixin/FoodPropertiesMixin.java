/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.item.LeftoversCreatedEvent;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
    @Inject(method = "onConsume", at = @At("TAIL"))
    private void cobblemon$createLeftovers(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (!(user instanceof ServerPlayer player)) return;
        if (!stack.is(CobblemonItemTags.LEAVES_LEFTOVERS)
                || !(level.getRandom().nextDouble() < Cobblemon.config.getAppleLeftoversChance())) return;
        CobblemonEvents.LEFTOVERS_CREATED.postThen(
                new LeftoversCreatedEvent(player, new ItemStack(CobblemonItems.LEFTOVERS)),
                event -> null,
                event -> {
                    if (!player.addItem(event.getLeftovers())) {
                        var itemPos = player.getLookAngle().scale(0.5F).add(player.position());
                        level.addFreshEntity(new ItemEntity(level, itemPos.x(), itemPos.y(), itemPos.z(), event.getLeftovers()));
                    }
                    return null;
                }
        );
    }
}
