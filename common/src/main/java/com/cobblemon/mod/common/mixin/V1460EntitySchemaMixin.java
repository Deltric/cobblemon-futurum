/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.datafixer.VanillaEntitySchemaRegistration;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.schemas.V1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(V1460.class)
public abstract class V1460EntitySchemaMixin {
    // V1460 rebuilds the entity map; all supported later schemas inherit it.
    // An unknown entity choice makes DFU retain the entire chunk's entity list,
    // preventing upgrades even for vanilla neighbors in the same chunk.
    @Inject(method = "registerEntities(Lcom/mojang/datafixers/schemas/Schema;)Ljava/util/Map;", at = @At("RETURN"))
    private void cobblemon$registerEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        VanillaEntitySchemaRegistration.register(schema, cir.getReturnValue());
    }
}
