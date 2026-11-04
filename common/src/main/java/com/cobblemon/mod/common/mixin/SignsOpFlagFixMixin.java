/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.mojang.datafixers.TypeRewriteRule;
import net.minecraft.util.datafix.fixes.SignsOpFlagFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignsOpFlagFix.class)
public abstract class SignsOpFlagFixMixin {
    @Shadow
    private TypeRewriteRule makeRuleForType(String entityName) { throw new AssertionError(); }

    // Keep the original native version boundary: newly created signs must not
    // gain command permission merely because they have a Cobblemon block ID.
    @Inject(method = "makeRule()Lcom/mojang/datafixers/TypeRewriteRule;", at = @At("RETURN"), cancellable = true)
    private void cobblemon$upgradeSignPermissions(CallbackInfoReturnable<TypeRewriteRule> cir) {
        cir.setReturnValue(TypeRewriteRule.seq(cir.getReturnValue(),
                makeRuleForType("cobblemon:sign"), makeRuleForType("cobblemon:hanging_sign")));
    }
}
