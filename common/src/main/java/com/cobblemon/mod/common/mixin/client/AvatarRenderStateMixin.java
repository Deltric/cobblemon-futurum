/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.AvatarPokemonState;
import com.cobblemon.mod.common.client.render.ModelGeometry;
import com.cobblemon.mod.common.client.render.MountedPlayerPose;
import com.cobblemon.mod.common.client.render.player.RecallArmAnimationTracker;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarPokemonState {
    @Unique private MountedPlayerPose cobblemon$mountedPose;
    @Override public MountedPlayerPose cobblemon$getMountedPose() { return cobblemon$mountedPose; }
    @Override public void cobblemon$setMountedPose(MountedPlayerPose pose) { cobblemon$mountedPose = pose; }
    @Unique private ModelGeometry cobblemon$shoulderGeometry;
    @Unique private ModelGeometry cobblemon$iconGeometry;
    @Override public ModelGeometry cobblemon$getIconGeometry() { return cobblemon$iconGeometry; }
    @Override public void cobblemon$setIconGeometry(ModelGeometry geometry) { cobblemon$iconGeometry = geometry; }
    @Override public ModelGeometry cobblemon$getShoulderGeometry() { return cobblemon$shoulderGeometry; }
    @Override public void cobblemon$setShoulderGeometry(ModelGeometry geometry) { cobblemon$shoulderGeometry = geometry; }
    @Unique private RecallArmAnimationTracker.RecallBallRender cobblemon$recallBall;
    @Override public RecallArmAnimationTracker.RecallBallRender cobblemon$getRecallBall() { return cobblemon$recallBall; }
    @Override public void cobblemon$setRecallBall(RecallArmAnimationTracker.RecallBallRender recall) { cobblemon$recallBall = recall; }
}
