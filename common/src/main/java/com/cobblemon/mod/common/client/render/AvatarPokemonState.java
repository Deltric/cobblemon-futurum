/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.cobblemon.mod.common.client.render.player.RecallArmAnimationTracker;

public interface AvatarPokemonState {
    MountedPlayerPose cobblemon$getMountedPose();
    void cobblemon$setMountedPose(MountedPlayerPose pose);
    ModelGeometry cobblemon$getIconGeometry();
    void cobblemon$setIconGeometry(ModelGeometry geometry);
    ModelGeometry cobblemon$getShoulderGeometry();
    void cobblemon$setShoulderGeometry(ModelGeometry geometry);
    RecallArmAnimationTracker.RecallBallRender cobblemon$getRecallBall();
    void cobblemon$setRecallBall(RecallArmAnimationTracker.RecallBallRender recall);
}
