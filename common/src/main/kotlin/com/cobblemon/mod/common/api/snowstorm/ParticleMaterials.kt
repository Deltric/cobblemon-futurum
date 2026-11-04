/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.snowstorm

import com.cobblemon.mod.common.mixin.accessor.ParticleRenderPipelineAccessor
import com.mojang.renderpearl.api.pipeline.BlendFactor
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.resources.Identifier

object ParticleMaterials {
    val ALPHA = ParticleRenderType("cobblemon:alpha", "CA")
    val ADD = ParticleRenderType("cobblemon:add", "CD")
    val BLEND = ParticleRenderType("cobblemon:blend", "CB")
    val OPAQUE = ParticleRenderType("cobblemon:opaque", "CO")

    val RENDER_ORDER = listOf(ADD, ALPHA, BLEND, OPAQUE)

    // These materials intentionally write depth even while blending. They must use the
    // direct particle pass: native OIT replaces both their blend equations and depth writes.
    private fun layer(name: String, cull: Boolean, source: BlendFactor, destination: BlendFactor) =
        SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES,
            RenderPipeline.builder(ParticleRenderPipelineAccessor.`cobblemon$particleSnippet`())
                .withLocation(Identifier.fromNamespaceAndPath("cobblemon", "pipeline/particle_$name"))
                .withCull(cull)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(ColorTargetState(BlendFunction(source, destination)))
                .build())

    private val alphaLayer by lazy { layer("alpha", true, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA) }
    private val additiveLayer by lazy { layer("add", false, BlendFactor.SRC_ALPHA, BlendFactor.ONE) }
    private val blendLayer by lazy { layer("blend", false, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA) }
    private val opaqueLayer by lazy { layer("opaque", true, BlendFactor.SRC_COLOR, BlendFactor.ZERO) }

    fun layer(type: ParticleRenderType) = when (type) {
        ALPHA -> alphaLayer
        ADD -> additiveLayer
        BLEND -> blendLayer
        OPAQUE -> opaqueLayer
        else -> error("Not a Snowstorm material: $type")
    }

    fun isSnowstorm(type: ParticleRenderType) = type === ALPHA || type === ADD || type === BLEND || type === OPAQUE
}
