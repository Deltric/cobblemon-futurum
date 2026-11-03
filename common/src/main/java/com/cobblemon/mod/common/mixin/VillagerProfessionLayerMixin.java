/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonVillagerProfessions;
import com.cobblemon.mod.common.client.render.VillagerNameState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerMixin {
    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/VillagerProfessionLayer;getIdentifier(Ljava/lang/String;Lnet/minecraft/core/Holder;)Lnet/minecraft/resources/Identifier;"))
    private Identifier cobblemon$professionTexture(VillagerProfessionLayer<?, ?> layer, String type, Holder<?> holder, Operation<Identifier> original,
                                                   @Local(argsOnly = true) LivingEntityRenderState state) {
        Identifier texture = original.call(layer, type, holder);
        if (!type.equals("profession") || !texture.getNamespace().equals(Cobblemon.MODID)
            || !(holder.value() instanceof VillagerProfession profession)) return texture;
        String name = ((VillagerNameState) state).cobblemon$getVillagerName();
        Pair<String, String[]> override = CobblemonVillagerProfessions.INSTANCE.getNameTagOverride(profession);
        if (name != null && override != null) {
            for (String suffix : override.getSecond()) {
                if (name.endsWith(suffix)) {
                    return texture.withPath(path -> path.substring(0, path.lastIndexOf('/') + 1) + override.getFirst() + ".png");
                }
            }
        }
        return texture;
    }
}
