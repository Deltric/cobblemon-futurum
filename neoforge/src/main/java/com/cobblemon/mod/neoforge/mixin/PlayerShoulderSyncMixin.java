/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.neoforge.mixin;

import com.cobblemon.mod.common.duck.PlayerDuck;
import com.cobblemon.mod.neoforge.NeoForgeShoulderAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** NeoForge requires attachments for custom state added to vanilla entities. */
@Mixin(Player.class)
public abstract class PlayerShoulderSyncMixin implements PlayerDuck {
    @Override public CompoundTag cobblemon$getShoulderEntityLeft() {
        return NeoForgeShoulderAttachments.getLeft((Player)(Object)this);
    }

    @Override public CompoundTag cobblemon$getShoulderEntityRight() {
        return NeoForgeShoulderAttachments.getRight((Player)(Object)this);
    }

    @Override public void cobblemon$setShoulderEntityLeft(CompoundTag tag) {
        NeoForgeShoulderAttachments.setLeft((Player)(Object)this, tag);
    }

    @Override public void cobblemon$setShoulderEntityRight(CompoundTag tag) {
        NeoForgeShoulderAttachments.setRight((Player)(Object)this, tag);
    }
}
