/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import com.cobblemon.mod.common.api.snowstorm.ParticleMaterials
import net.minecraft.CrashReport
import net.minecraft.ReportedException
import net.minecraft.client.Camera
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.client.particle.ParticleGroup
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.renderer.culling.Frustum

class SnowstormParticleGroup(engine: ParticleEngine, private val type: ParticleRenderType) : ParticleGroup<SnowstormParticle>(engine) {
    private val renderState = SnowstormQuadRenderState(ParticleMaterials.layer(type), 1 + ParticleMaterials.RENDER_ORDER.indexOf(type))

    override fun extractRenderState(frustum: Frustum, camera: Camera, partialTickTime: Float): SnowstormQuadRenderState {
        for (particle in particles) {
            // Snowstorm expands its bounds for large and rectangular particles; point culling loses them.
            if (!frustum.isVisible(particle.boundingBox)) continue
            try {
                particle.extract(renderState, camera, partialTickTime)
            } catch (error: Throwable) {
                val report = CrashReport.forThrowable(error, "Extracting Snowstorm particle")
                report.addCategory("Particle being extracted").apply {
                    setDetail("Particle") { particle.toString() }
                    setDetail("Particle type") { type.toString() }
                }
                throw ReportedException(report)
            }
        }
        return renderState
    }
}
