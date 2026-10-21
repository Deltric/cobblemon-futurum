/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.platform.events.PlatformEvents;
import com.cobblemon.mod.common.platform.events.ServerPlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        PlatformEvents.SERVER_PLAYER_TICK_PRE.emit(new ServerPlayerTickEvent.Pre(player));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        PlatformEvents.SERVER_PLAYER_TICK_POST.emit(new ServerPlayerTickEvent.Post(player));
    }
}
