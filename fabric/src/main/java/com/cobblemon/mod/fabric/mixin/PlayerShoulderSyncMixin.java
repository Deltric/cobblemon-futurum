/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.duck.PlayerDuck;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import com.cobblemon.mod.common.api.net.serializers.ShoulderDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores custom shoulder data synchronization removed from vanilla Player. */
@Mixin(Player.class)
public abstract class PlayerShoulderSyncMixin implements PlayerDuck {
    @Unique private static final EntityDataAccessor<CompoundTag> COBBLEMON_LEFT_SHOULDER =
        SynchedEntityData.defineId(Player.class, ShoulderDataSerializer.INSTANCE);
    @Unique private static final EntityDataAccessor<CompoundTag> COBBLEMON_RIGHT_SHOULDER =
        SynchedEntityData.defineId(Player.class, ShoulderDataSerializer.INSTANCE);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void cobblemon$defineShoulders(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(COBBLEMON_LEFT_SHOULDER, new CompoundTag());
        builder.define(COBBLEMON_RIGHT_SHOULDER, new CompoundTag());
    }

    @Override public CompoundTag cobblemon$getShoulderEntityLeft() {
        return ((Player)(Object)this).getEntityData().get(COBBLEMON_LEFT_SHOULDER);
    }
    @Override public CompoundTag cobblemon$getShoulderEntityRight() {
        return ((Player)(Object)this).getEntityData().get(COBBLEMON_RIGHT_SHOULDER);
    }
    @Override public void cobblemon$setShoulderEntityLeft(CompoundTag tag) {
        ((Player)(Object)this).getEntityData().set(COBBLEMON_LEFT_SHOULDER, tag.copy());
    }
    @Override public void cobblemon$setShoulderEntityRight(CompoundTag tag) {
        ((Player)(Object)this).getEntityData().set(COBBLEMON_RIGHT_SHOULDER, tag.copy());
    }
}
