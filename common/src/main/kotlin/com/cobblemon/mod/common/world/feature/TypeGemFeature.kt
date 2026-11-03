/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.feature

import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.util.RandomSource
import com.mojang.serialization.MapCodec
import com.cobblemon.mod.common.CobblemonBlocks.TYPE_GEM_CORE
import com.cobblemon.mod.common.CobblemonBlocks.typeGemBlocks
import com.cobblemon.mod.common.block.TypeGemCoreBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block.UPDATE_ALL
import net.minecraft.world.level.levelgen.feature.Feature

class TypeGemFeature(val state: BlockState) : Feature {
    companion object {
        val CODEC: MapCodec<TypeGemFeature> = BlockState.CODEC.fieldOf("state").xmap(::TypeGemFeature, { it.state })
    }

    override fun codec(): MapCodec<TypeGemFeature> = CODEC
    override fun place(worldGenLevel: WorldGenLevel, chunkGenerator: ChunkGenerator, random: RandomSource, origin: BlockPos): Boolean {

        val originBlock = worldGenLevel.getBlockState(origin)
        if (!originBlock.`is`(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return false
        }

        val neighborAirBlocks = getNeighborAirBlocks(worldGenLevel, origin)
        if (neighborAirBlocks.isEmpty()) {
            return false
        }

        val chosenPos = neighborAirBlocks[random.nextInt(neighborAirBlocks.size)]
        val typeGemBlocks = typeGemBlocks().toList()
        val (gemId, gemBlock) = typeGemBlocks[random.nextInt(typeGemBlocks.size)]
        val gemState = gemBlock.defaultBlockState()
        val coreState = TYPE_GEM_CORE.defaultBlockState()

        // Place the gem first then make sure it placed before placing the core.
        worldGenLevel.setBlock(origin, coreState, UPDATE_ALL)
        if (!worldGenLevel.getBlockState(chosenPos).`is`(gemBlock)) {
            return false
        }
        worldGenLevel.setBlock(origin, coreState, UPDATE_ALL)

        // Trigger forced growth with clusters
        val coreBlock = coreState.block as TypeGemCoreBlock
        coreBlock.forceGrow(worldGenLevel, origin, random, 0.5f)

        // println("[TypeGemFeature] Placed ${gemId.path} at $chosenPos")
        return true
    }

    fun getNeighborAirBlocks(worldGenLevel: WorldGenLevel, origin: BlockPos): List<BlockPos> {
        val airBlocks = mutableListOf<BlockPos>()

        for (direction: Direction in Direction.entries) {
            val pos = origin.offset(direction.unitVec3i)
            val blockState = worldGenLevel.getBlockState(pos)
            if (blockState.isAir) {
                airBlocks.add(pos)
            }
        }

        return airBlocks
    }
}