/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.multiblock

import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.level.storage.ValueInput
import com.cobblemon.mod.common.api.multiblock.builder.MultiblockStructureBuilder
import com.cobblemon.mod.common.util.DataKeys
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Multiblock entities are kind of complicated. Basically every multiblock entity should have a MultiBlockStructureBuilder
 * that is checked when this block entity is created. The multiblock entity also contains a reference to a MultiblockStructure
 * which is shared between all MultiblockEntities in the structure. Finally, every Multiblock contains the location of the
 * block that controls this blockentities associated structure
 */
abstract class MultiblockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    var multiblockBuilder: MultiblockStructureBuilder?
) : BlockEntity(type, pos, state) {

    abstract var multiblockStructure: MultiblockStructure?
    abstract var masterBlockPos: BlockPos?

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registryLookup)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        // Multiblock build conditions exclude already formed structures.
        output.putBoolean(DataKeys.FORMED, masterBlockPos != null)
        val structure = multiblockStructure
        if (structure != null && structure.controllerBlockPos == blockPos) {
            structure.save(output.child(DataKeys.MULTIBLOCK_STORAGE))
        } else {
            output.storeNullable(DataKeys.CONTROLLER_BLOCK, BlockPos.CODEC, masterBlockPos)
        }
    }

    override fun preRemoveSideEffects(pos: BlockPos, state: BlockState) {
        val world = level ?: return
        multiblockStructure?.playerWillDestroy(world, pos, state, null)
        super.preRemoveSideEffects(pos, state)
    }

    override fun isValidBlockState(blockState: BlockState) = blockState.block is MultiblockBlock

    abstract override fun loadAdditional(input: ValueInput)

}
