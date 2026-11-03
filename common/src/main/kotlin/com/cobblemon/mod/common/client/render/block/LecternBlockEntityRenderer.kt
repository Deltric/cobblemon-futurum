/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.ModAPI

import com.cobblemon.mod.common.block.entity.LecternBlockEntity
import com.cobblemon.mod.common.item.PokedexItem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.ModelGeometry
import com.cobblemon.mod.common.client.render.NativeModelGeometry
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.item.*
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Unique

class LecternBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeometryBlockEntityRenderer<LecternBlockEntity>() {

    override fun extractGeometry(blockEntity: LecternBlockEntity, tickDelta: Float, poseStack: PoseStack, multiBufferSource: ModelGeometry.Builder, light: Int, overlay: Int ) {
        if (blockEntity !is LecternBlockEntity) return
        if (!blockEntity.isEmpty()) {
            val blockState = if (blockEntity.level != null) blockEntity.blockState
            else (CobblemonBlocks.CAMPFIRE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH) as BlockState)
            val yRot = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot()

            poseStack.pushPose()
            poseStack.translate(0.5, 1.17, 0.5)
            poseStack.rotate(Axis.YP.rotationDegrees(-yRot))
            poseStack.rotate(Axis.XP.rotationDegrees(22.5F))
            poseStack.translate(0.0, 0.0, 0.13)

            if (blockEntity.getItemStack().item is PokedexItem) {
                val resourceLocation = (blockEntity.getItemStack().item as PokedexItem).type.getItemModelPath(if (blockEntity.hasViewer()) "flat" else "flat_off")
                val displayStack = blockEntity.getItemStack().copy()
                displayStack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, resourceLocation)
                NativeModelGeometry.extractItem(displayStack, ItemDisplayContext.GROUND, light, overlay, poseStack, multiBufferSource, blockEntity.level, 0)
            } else {
                NativeModelGeometry.extractItem(blockEntity.getItemStack(), ItemDisplayContext.GROUND, light, overlay, poseStack, multiBufferSource, blockEntity.level, 0)
            }

            poseStack.popPose()
        }
    }
}