/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.AvatarPokemonState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void cobblemon$hideRecallHandItem(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm,
                                              PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (arm == HumanoidArm.RIGHT && state instanceof AvatarPokemonState pokemonState && pokemonState.cobblemon$getRecallBall() != null) {
            ci.cancel();
        }
    }
}
