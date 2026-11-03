/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.AvatarPokemonState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    // Apply the captured ride pose before native held-item, attack and arm-bob
    // animations. This also works for armor models consuming the same state.
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;isUsingItem:Z", ordinal = 0))
    private void cobblemon$animateRiders(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarPokemonState pokemonState && pokemonState.cobblemon$getMountedPose() != null) {
            pokemonState.cobblemon$getMountedPose().apply((HumanoidModel<?>)(Object)this);
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void cobblemon$poseRecallArm(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarPokemonState pokemonState) || pokemonState.cobblemon$getRecallBall() == null) return;
        float raise = pokemonState.cobblemon$getRecallBall().getRaise();
        if (raise <= 0f) return;
        var rightArm = ((HumanoidModel<?>)(Object)this).rightArm;
        rightArm.xRot = Mth.lerp(raise, rightArm.xRot, (float) (-Math.PI / 2.0));
        rightArm.yRot = Mth.lerp(raise, rightArm.yRot, 0f);
        rightArm.zRot = Mth.lerp(raise, rightArm.zRot, 0f);
    }
}
