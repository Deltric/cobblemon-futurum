/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.PosableEntity;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
    @Inject(method = "showHitboxes", at = @At("TAIL"))
    private void cobblemon$showLocators(Entity entity, float partialTicks, boolean serverEntity, CallbackInfo ci) {
        if (!serverEntity && entity instanceof PosableEntity posable && posable.getDelegate() instanceof PosableState state) {
            Vec3 offset = entity.getPosition(partialTicks).subtract(entity.position());
            state.getLocatorStates().forEach((locator, matrix) ->
                Gizmos.cuboid(AABB.ofSize(matrix.getOrigin().add(offset), 0.25, 0.25, 0.25), GizmoStyle.stroke(0xff00ff00)));
        }
    }
}
