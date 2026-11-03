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
import com.cobblemon.mod.common.block.MintBlock
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.CropBlock.UPDATE_CLIENTS
import net.minecraft.world.level.levelgen.feature.Feature

class MintBlockFeature(val state: BlockState) : Feature {
    companion object {
        val CODEC: MapCodec<MintBlockFeature> = BlockState.CODEC.fieldOf("state").xmap(::MintBlockFeature, { it.state })
    }

    override fun codec(): MapCodec<MintBlockFeature> = CODEC

    override fun place(worldGenLevel: WorldGenLevel, chunkGenerator: ChunkGenerator, random: RandomSource, origin: BlockPos): Boolean {
        val world = worldGenLevel
        val blockPos = origin
        val blockState = state
        val floor = blockPos.below()

        if (!world.getBlockState(floor).`is`(BlockTags.SUBSTRATE_OVERWORLD)) return false

        // Attempt to get at least one other valid position for the crop
        val validPlacements = getValidPositions(world, blockPos)
        if (validPlacements.isEmpty()) return false

        val minAge = MintBlock.MATURE_AGE - 2
        val maxAge = MintBlock.MATURE_AGE

        // Generate the blocks
        world.setBlock(blockPos, blockState.setValue(MintBlock.AGE, random.nextIntBetweenInclusive(minAge, maxAge)), UPDATE_CLIENTS)
        validPlacements.shuffled().take(2).forEach { position ->
            world.setBlock(position, blockState.setValue(MintBlock.AGE, random.nextIntBetweenInclusive(minAge, maxAge)), UPDATE_CLIENTS)
        }
        return true
    }

    private fun getValidPositions(world: WorldGenLevel, origin: BlockPos): List<BlockPos> {
        val validPositions = mutableListOf<BlockPos>()

        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    if (x == 0 && z == 0) continue
                    val offsetPos = origin.offset(x, y, z)
                    val floorBlockState = world.getBlockState(offsetPos.below())
                    if (world.isEmptyBlock(offsetPos) && floorBlockState.`is`(BlockTags.SUBSTRATE_OVERWORLD)) {
                        validPositions.add(offsetPos)
                    }
                }
            }
        }

        return validPositions
    }

}