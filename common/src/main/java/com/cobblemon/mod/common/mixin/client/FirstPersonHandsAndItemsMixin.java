/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.item.PokedexItem;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cobblemon$extractHands(LocalPlayer player, float partialTicks, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof PokedexItem) {
            state.handRenderSelection = null;
        }
        if (player.getVehicle() instanceof OrientationControllable controllable && controllable.getOrientationController().isActive()) {
            // Hand submission uses these differences for the two view-bob rotations.
            state.xBob = state.viewXRot;
            state.yBob = state.viewYRot;
        }
    }
}
