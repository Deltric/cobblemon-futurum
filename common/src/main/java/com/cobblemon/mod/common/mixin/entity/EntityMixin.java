/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.entity;

import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.duck.RidePassenger;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow @Nullable public abstract Entity getVehicle();

    // Mounted riders keep fluid tracking/current forces, but do not acquire the
    // swimming flag or entry splash/fall-reset side effects from intersecting water.
    @WrapOperation(method = "updateFluidInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;resetFallDistance()V"))
    private void cobblemon$preventMountedWaterFallReset(Entity entity, Operation<Void> original) {
        if (!(getVehicle() instanceof PokemonEntity)) original.call(entity);
    }

    @WrapOperation(method = "updateFluidInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;doWaterSplashEffect()V"))
    private void cobblemon$preventMountedWaterSplash(Entity entity, Operation<Void> original) {
        if (!(getVehicle() instanceof PokemonEntity)) original.call(entity);
    }

    @WrapOperation(method = "updateFluidInteraction", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;wasTouchingWater:Z", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void cobblemon$verifyActuallyTouchingWater(Entity entity, boolean touchingWater, Operation<Void> original) {
        if (!touchingWater || !(getVehicle() instanceof PokemonEntity)) original.call(entity, touchingWater);
    }

    @WrapOperation(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;onGround()Z"
            )
    )
    public boolean cobblemon$forceOnGroundForStepUp(Entity entity, Operation<Boolean> original) {
        if (entity instanceof PokemonEntity vehicle && entity.hasControllingPassenger()) {
            return true;
        }
        return original.call(entity);
    }

    @Inject(
            method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblemon$modifyEyePosition_partial(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        cobblemon$getCustomEyePos(cir);
    }

    @Inject(
            method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblemon$modifyEyePosition_noPartial(CallbackInfoReturnable<Vec3> cir) {
        cobblemon$getCustomEyePos(cir);
    }

    @Unique
    private void cobblemon$getCustomEyePos(CallbackInfoReturnable<Vec3> cir) {
        var entity = (Entity)(Object)this;
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (!(player instanceof RidePassenger ridePassenger)) return;
        if (!(player.getVehicle() instanceof OrientationControllable vehicle)) return;
        if (!(vehicle instanceof PokemonEntity)) return;
        var vehicleController = vehicle.getOrientationController();
        if (vehicleController == null) return;

        Vec3 customEyePos = ridePassenger.cobblemon$getRideEyePos();
        cir.setReturnValue(customEyePos);
    }


    @WrapOperation(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V", ordinal = 0)
    )
    private void cobblemon$customPusherLogic(Entity pusher, double x, double y, double z,
                                            Operation<Void> original, @Local(argsOnly = true) Entity pushee) {
        float force = cobblemon$collisionForce(pusher, pushee);
        original.call(pusher, x * force, y, z * force);
    }

    @WrapOperation(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V", ordinal = 1)
    )
    private void cobblemon$customPusheeLogic(Entity pushee, double x, double y, double z, Operation<Void> original) {
        float force = cobblemon$collisionForce(pushee, (Entity) (Object) this);
        original.call(pushee, x * force, y, z * force);
    }

    @Unique
    private static float cobblemon$collisionForce(Entity receiver, Entity other) {
        if (!(receiver instanceof PokemonEntity || receiver instanceof Player)
                || !(other instanceof PokemonEntity || other instanceof Player)) return 1F;
        // Weights remain hectograms, with the original 900 hg player estimate.
        float receiverWeight = receiver instanceof PokemonEntity pokemon ? pokemon.getPokemon().getSpecies().getWeight() : 900F;
        float otherWeight = other instanceof PokemonEntity pokemon ? pokemon.getPokemon().getSpecies().getWeight() : 900F;
        return otherWeight / (receiverWeight + otherWeight) * 2F;
    }
}
