/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cobblemon.mod.common.client.render.color.CobblemonItemTintSource;

@Mixin(net.minecraft.client.color.item.ItemTintSources.class)
public abstract class CobblemonItemTintSourcesMixin {
    @Shadow @Final private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends net.minecraft.client.color.item.ItemTintSource>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void cobblemon$register(CallbackInfo ci) {
        ID_MAPPER.put(Identifier.parse("cobblemon:item"), CobblemonItemTintSource.CODEC);
    }
}
