/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import net.minecraft.server.level.ServerLevel
import com.cobblemon.mod.common.block.entity.DiscShelfBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.Containers
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.VoxelShape

class DiscShelfBlock(properties: Properties) : BaseEntityBlock(properties) {
    companion object {
        val FACING: EnumProperty<Direction> = HorizontalDirectionalBlock.FACING
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return DiscShelfBlockEntity(pos, state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, ctx.horizontalDirection.opposite)
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hit: BlockHitResult
    ): InteractionResult {
        val entity = level.getBlockEntity(pos) as? DiscShelfBlockEntity ?: return InteractionResult.PASS
        return entity.handleUseWithoutItem(state, level, pos, player, hit)
    }

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): InteractionResult {
        val entity = level.getBlockEntity(pos) as? DiscShelfBlockEntity ?: return InteractionResult.PASS
        return entity.handleUseItem(stack, state, level, pos, player, hit)
    }

    override fun hasAnalogOutputSignal(state: BlockState) = true

    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos, direction: Direction): Int {
        val entity = level.getBlockEntity(pos) as? DiscShelfBlockEntity
        return entity?.items?.count { !it.isEmpty }?.coerceIn(0, 14) ?: 0
    }

    override fun affectNeighborsAfterRemoval(state: BlockState, level: ServerLevel, pos: BlockPos, movedByPiston: Boolean) {
        if (!movedByPiston) level.updateNeighbourForOutputSignal(pos, this)
    }

    override fun getShape(
        state: BlockState, level: BlockGetter,
        pos: BlockPos, context: net.minecraft.world.phys.shapes.CollisionContext
    ): VoxelShape = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
}
