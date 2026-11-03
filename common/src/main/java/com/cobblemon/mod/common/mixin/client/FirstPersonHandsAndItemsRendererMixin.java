/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.player.FirstPersonRecallBallRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsAndItemsRendererMixin {
    // The recall ball replaces the main hand and its item while a recall is shown.
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void cobblemon$submitRecallBall(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float frameInterp, float xRot, InteractionHand hand,
                                           float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                           SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND && FirstPersonRecallBallRenderer.INSTANCE.submit(poseStack, collector, lightCoords)) {
            ci.cancel();
        }
    }
}
