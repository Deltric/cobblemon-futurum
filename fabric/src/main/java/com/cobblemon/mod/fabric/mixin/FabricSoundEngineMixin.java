/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.api.riding.sound.RideAttenuationModel;
import com.cobblemon.mod.common.api.riding.sound.RideLoopSound;
import com.cobblemon.mod.common.client.sound.instances.AlphaCrySoundInstance;
import com.cobblemon.mod.common.duck.ChannelDuck;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(SoundEngine.class)
public class FabricSoundEngineMixin {
    @WrapOperation(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V", ordinal = 0)
    )
    private void cobblemon$configureSoundChannel(
            ChannelAccess.ChannelHandle handle,
            Consumer<Channel> configure,
            Operation<Void> original,
            @Local(argsOnly = true) SoundInstance instance,
            @Local(name = "attenuationDistance") float attenuationDistance
    ) {
        if (!(instance instanceof RideLoopSound) && !(instance instanceof AlphaCrySoundInstance)) {
            original.call(handle, configure);
            return;
        }
        original.call(handle, (Consumer<Channel>) channel -> {
            // Keep native playback configuration, then apply the custom effects
            // so native attenuation cannot overwrite them in the next queued task.
            configure.accept(channel);
            if (instance instanceof RideLoopSound rideSound) {
                if (rideSound.getRideAttenuation() == RideAttenuationModel.LINEAR) {
                    channel.linearAttenuation(attenuationDistance);
                } else if (rideSound.getRideAttenuation() == RideAttenuationModel.EXPONENTIAL) {
                    ((ChannelDuck) channel).cobblemon$inverseAttenuation(attenuationDistance);
                } else {
                    channel.disableAttenuation();
                }
                cobblemon$applyMuffling(channel, rideSound);
            }
            if (instance instanceof AlphaCrySoundInstance) {
                ((ChannelDuck) channel).cobblemon$applyReverb(1.8f, 0.25f, 0.1f, 0.9f);
            }
        });
    }

    @WrapOperation(
            method = "tickInGameSound()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V", ordinal = 0)
    )
    private void cobblemon$updateRideSoundChannel(
            ChannelAccess.ChannelHandle handle,
            Consumer<Channel> update,
            Operation<Void> original,
            @Local(name = "instance") TickableSoundInstance instance
    ) {
        if (!(instance instanceof RideLoopSound rideSound)) {
            original.call(handle, update);
            return;
        }
        original.call(handle, (Consumer<Channel>) channel -> {
            update.accept(channel);
            cobblemon$applyMuffling(channel, rideSound);
        });
    }

    @Unique
    private static void cobblemon$applyMuffling(Channel channel, RideLoopSound rideSound) {
        if (rideSound.getShouldMuffle()) {
            ((ChannelDuck) channel).cobblemon$applyLowPassFilter(1.0f, rideSound.getMuffleAmount());
        } else {
            ((ChannelDuck) channel).cobblemon$clearFilters();
        }
    }
}
