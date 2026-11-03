/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.world.gamerules.LegacyGameRuleMigration;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.GameRuleRegistryFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRuleRegistryFix.class)
public abstract class GameRuleRegistryFixMixin {
    // Verified in 26.3: this lambda converts the old GameRules map before it is
    // stored as game_rules and decoded through the new game-rule registry.
    @ModifyReturnValue(method = "lambda$makeRule$2", at = @At("RETURN"))
    private static Dynamic<?> cobblemon$migrateSavedRules(Dynamic<?> rules) {
        return LegacyGameRuleMigration.migrate(rules);
    }
}
