/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import net.minecraft.util.RandomSource
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.LevelReader
import com.cobblemon.mod.common.api.tags.CobblemonBlockTags
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class TumblestoneBlock(
    settings: Properties,
    stage: Int,
    height: Int,
    xzOffset: Int,
    nextStage: Block?
) : GrowableStoneBlock(settings, stage, height, xzOffset, nextStage), SimpleWaterloggedBlock {

    companion object {

        const val STAGE_0 = 0
        const val STAGE_1 = 1
        const val STAGE_2 = 2
        const val STAGE_3 = 3

        const val MAX_STAGE = STAGE_3
        const val MIN_STAGE = STAGE_0
    }

    override val growthChance = 5

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.DOWN)
            .setValue(WATERLOGGED, false))
    }

    override fun canGrow(state: BlockState, pos: BlockPos, world: BlockGetter): Boolean {
        if (stage == MAX_STAGE) return false
        val iterator: Iterator<BlockPos> =
            BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))
                .iterator()

        var blockPos: BlockPos
        do { if (!iterator.hasNext()) { return false }
            blockPos = iterator.next()
        } while (!world.getBlockState(blockPos).`is`(CobblemonBlockTags.TUMBLESTONE_HEAT_SOURCE))

        return true
    }

    override fun isRandomlyTicking(state: BlockState): Boolean = stage < MAX_STAGE

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) {
            Fluids.WATER.getSource(false)
        } else super.getFluidState(state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(WATERLOGGED)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        return super.getStateForPlacement(ctx)?.setValue(WATERLOGGED, ctx.level.getFluidState(ctx.clickedPos).type == Fluids.WATER)

    }

    override fun updateShape(state: BlockState, world: LevelReader, ticks: ScheduledTickAccess, pos: BlockPos, direction: Direction, neighborPos: BlockPos, neighborState: BlockState, random: RandomSource): BlockState {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
        return super.updateShape(state, world, ticks, pos, direction, neighborPos, neighborState, random)
    }
}