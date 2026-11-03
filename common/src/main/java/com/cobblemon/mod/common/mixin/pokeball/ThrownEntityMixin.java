/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.pokeball;

import com.cobblemon.mod.common.entity.pokeball.WaterDragModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ThrowableProjectile.class)
public abstract class ThrownEntityMixin extends Entity {

    public ThrownEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyArg(method = "applyInertia", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double cobblemon$waterDragModifier(double value) {
        if (this.isInWater() && this instanceof WaterDragModifier) {
            return ((WaterDragModifier) this).waterDrag();
        }
        return value;
    }

}
