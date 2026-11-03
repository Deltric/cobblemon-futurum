/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import java.util.function.Supplier
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

interface CobblemonClientImplementation {
    fun getBakingOverride(model: BakingOverride): net.minecraft.client.resources.model.geometry.QuadCollection

    fun registerLayer(modelLayer: ModelLayerLocation, supplier: Supplier<LayerDefinition>)
    fun <T : ParticleOptions> registerParticleFactory(
        type: ParticleType<T>,
        factory: (SpriteSet) -> ParticleProvider<T>
    )

    fun registerBlockColors(provider: BlockTintSource, vararg blocks: Block)

    fun <T : BlockEntity, S : net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> registerBlockEntityRenderer(type: BlockEntityType<out T>, factory: BlockEntityRendererProvider<T, S>)

    fun <T : Entity> registerEntityRenderer(type: EntityType<out T>, factory: EntityRendererProvider<T>)
}