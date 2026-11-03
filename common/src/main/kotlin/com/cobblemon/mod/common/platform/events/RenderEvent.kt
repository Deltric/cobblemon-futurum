/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.platform.events

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.LevelRenderState

/**
 * Native geometry submission. Consumers must use independently extracted state;
 * material pipelines determine the eventual solid/translucent/OIT draw phase.
 */
data class RenderEvent(
    val stage: Stage,
    val poseStack: PoseStack,
    val collector: SubmitNodeCollector,
    val levelState: LevelRenderState
) {
    enum class Stage { SUBMIT }
}
