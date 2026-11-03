/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.decor


import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.FlowerBedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class PomohanaFlowerBlock(properties: Properties) : FlowerBedBlock(properties, SHAPE_HEIGHT) {

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = AABB

    companion object {
        // Only used for the vanilla segment shapes, which getShape replaces.
        private const val SHAPE_HEIGHT = 3
        private val AABB = Shapes.box(0.125, 0.0, 0.125, 0.875, 1.0, 0.875)
    }

}