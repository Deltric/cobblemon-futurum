/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.feature.structure

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.feature.Feature
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager

class CobblemonFeature(val configuration: CobblemonFeatureConfiguration) : Feature {
    companion object {
        val CODEC: MapCodec<CobblemonFeature> = CobblemonFeatureConfiguration.CODEC.xmap(::CobblemonFeature, { it.configuration })
    }
    override fun codec(): MapCodec<CobblemonFeature> = CODEC

    override fun place(worldGenLevel: WorldGenLevel, chunkGenerator: ChunkGenerator, randomSource: RandomSource, blockPos: BlockPos): Boolean {
        val rotation: Rotation = Rotation.getRandom(randomSource)
        val cobblemonFeatureConfiguration = configuration

        val i = randomSource.nextInt(cobblemonFeatureConfiguration.cobblemonStructures.size)
        val structureTemplateManager: StructureTemplateManager = worldGenLevel.level.server.structureTemplateManager
        val structureTemplate: StructureTemplate =
            structureTemplateManager.getOrCreate(cobblemonFeatureConfiguration.cobblemonStructures[i])

        val structurePlaceSettings = StructurePlaceSettings()
            .setRotation(rotation)
            .setRandom(randomSource)

        val vec3i: Vec3i = structureTemplate.getSize(rotation)

        if (vec3i.x == 0 && vec3i.y == 0 && vec3i.z == 0) {
            return false
        }

        val blockPos2: BlockPos = blockPos.offset(-vec3i.x / 2, 0, -vec3i.z / 2)
        val blockPos3 = structureTemplate.getZeroPositionWithTransform(blockPos2, Mirror.NONE, rotation)

        structurePlaceSettings.clearProcessors()
        val processors = (cobblemonFeatureConfiguration.cobblemonProcessors.value() as StructureProcessorList).list()
        processors.forEach { structurePlaceSettings.addProcessor(it) }

        return structureTemplate.placeInWorld(
            worldGenLevel,
            blockPos3,
            blockPos3,
            structurePlaceSettings,
            randomSource,
            2
        )
    }
}