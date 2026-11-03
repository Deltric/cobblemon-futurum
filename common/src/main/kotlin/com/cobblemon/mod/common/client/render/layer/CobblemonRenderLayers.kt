/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.layer

import com.cobblemon.mod.common.mixin.accessor.ModelRenderPipelineAccessor
import com.cobblemon.mod.common.mixin.accessor.RenderTypeAccessor
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import java.util.function.BiFunction
import java.util.function.Function

object CobblemonRenderLayers {
    val BERRY_LAYER: RenderType = RenderTypeAccessor.`cobblemon$create`(
        "cobblemon_berries",
        RenderSetup.builder(
            RenderPipeline.builder(ModelRenderPipelineAccessor.`cobblemon$blockSnippet`())
                .withLocation(cobblemonResource("pipeline/berries"))
                .withShaderDefine("ALPHA_CUTOUT", 0.5F)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withCull(false)
                .build()
        ).withTexture("Sampler0", cobblemonResource("textures/atlas/berries.png"))
            .useLightmap()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup()
    )

    val ENTITY_TRANSLUCENT = BiFunction<Identifier, Boolean, RenderType> { texture, affectsOutline ->
        RenderTypes.entityTranslucent(texture, affectsOutline)
    }

    val ENTITY_CUTOUT = Function<Identifier, RenderType>(RenderTypes::entityCutoutCull)
}
