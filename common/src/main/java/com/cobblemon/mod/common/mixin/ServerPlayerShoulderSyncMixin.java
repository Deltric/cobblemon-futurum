/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.duck.PlayerDuck;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native setters are also used by save loading and respawn copying. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerShoulderSyncMixin {
    @Inject(method = "setShoulderEntityLeft", at = @At("TAIL"))
    private void cobblemon$syncLeft(CompoundTag tag, CallbackInfo ci) {
        ((PlayerDuck)this).cobblemon$setShoulderEntityLeft(tag);
    }
    @Inject(method = "setShoulderEntityRight", at = @At("TAIL"))
    private void cobblemon$syncRight(CompoundTag tag, CallbackInfo ci) {
        ((PlayerDuck)this).cobblemon$setShoulderEntityRight(tag);
    }
}
