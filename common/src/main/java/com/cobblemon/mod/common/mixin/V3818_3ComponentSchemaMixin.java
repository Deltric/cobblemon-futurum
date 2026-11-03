/*
 * Copyright (C) 2023 Cobblemon Contributors
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.datafixer.VanillaBlockEntitySchemaRegistration;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.schemas.V3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(V3818_3.class)
public abstract class V3818_3ComponentSchemaMixin {
    @Inject(method = "components(Lcom/mojang/datafixers/schemas/Schema;)Ljava/util/SequencedMap;", at = @At("RETURN"))
    private static void cobblemon$registerComponents(Schema schema, CallbackInfoReturnable<SequencedMap<String, Supplier<TypeTemplate>>> cir) {
        VanillaBlockEntitySchemaRegistration.registerComponents(schema, cir.getReturnValue());
    }
}
