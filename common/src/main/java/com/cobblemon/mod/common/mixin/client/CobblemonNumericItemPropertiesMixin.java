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
import com.cobblemon.mod.common.client.render.item.CobblemonModelPredicateRegistry;

@Mixin(net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.class)
public abstract class CobblemonNumericItemPropertiesMixin {
    @Shadow @Final private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void cobblemon$register(CallbackInfo ci) {
        ID_MAPPER.put(Identifier.parse("cobblemon:cast"), CobblemonModelPredicateRegistry.Property.CAST.getCodec());
        ID_MAPPER.put(Identifier.parse("cobblemon:ponigiri_overlay"), CobblemonModelPredicateRegistry.Property.PONIGIRI_OVERLAY.getCodec());
        ID_MAPPER.put(Identifier.parse("cobblemon:aprijuice_quality"), CobblemonModelPredicateRegistry.Property.APRIJUICE_QUALITY.getCodec());
    }
}
