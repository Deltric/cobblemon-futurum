/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.entity

import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.decor.MistleafFlowerBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class MistleafFlowerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(CobblemonBlockEntities.MISTLEAF_FLOWER, pos, state) {
    companion object {
        fun serverTick(level: Level, pos: BlockPos, state: BlockState, mistleafFlowerBlockEntity: MistleafFlowerBlockEntity) {
            if (level.isClientSide) return
            if (state.block !is MistleafFlowerBlock) return

            val glowing = state.getValue(MistleafFlowerBlock.GLOWING)

            // Uses the global clock and eight-day moon cycle, as the 1.21.1 day time and moon phase did.
            val time = level.overworldClockTime
            val night = Math.floorMod(time, 24000L) in 12542L..23459L
            val moonPhase = Math.floorMod(time / 24000L, 8L).toInt()
            val wantedState = when {
                !state.`is`(CobblemonBlocks.MISTLEAF) || !night -> 0 //Daytime, and likely dimensions with no concept of day/night cycle or moon phase
                //The light level and comparator output increases on odds from New to Full, and decreases on the evens from Full to New
                moonPhase == 4 -> 1 //New Moon
                moonPhase == 5 -> 3 //Waxing Crescent
                moonPhase == 6 -> 5 //First Quarter
                moonPhase == 7 -> 7 //Waxing Gibbous
                moonPhase == 0 -> 8 //Full Moon
                moonPhase == 1 -> 6 //Waning Gibbous
                moonPhase == 2 -> 4 // Third Quarter
                else -> 2 //Waning Crescent
            }

            if (glowing != wantedState) {
                level.setBlock(pos, state.setValue(MistleafFlowerBlock.GLOWING, wantedState), Block.UPDATE_ALL)
            }
        }
    }
}