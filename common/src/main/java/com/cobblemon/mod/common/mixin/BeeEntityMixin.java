/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.block.SaccharineLeafBlock;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeGrowCropGoal")
public abstract class BeeEntityMixin {
    // Seed vanilla's prospective state; its bee-growable tag gate, event, update
    // and pollination counter remain responsible for committing the growth.
    @ModifyVariable(method = "tick", at = @At("STORE"), index = 5)
    private BlockState cobblemon$growSaccharineLeaves(BlockState before, @Local(name = "belowState") BlockState belowState) {
        if (before == null && belowState.getBlock() instanceof SaccharineLeafBlock) {
            int age = belowState.getValue(SaccharineLeafBlock.Companion.getAGE());
            if (age < 2 && !belowState.getValue(BlockStateProperties.WATERLOGGED)) {
                return belowState.setValue(SaccharineLeafBlock.Companion.getAGE(), age + 1);
            }
        }
        return before;
    }
}
