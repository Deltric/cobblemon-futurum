/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.AvatarPokemonState;
import com.cobblemon.mod.common.client.render.ClientPlayerIcon;
import com.cobblemon.mod.common.client.render.layer.PokemonOnShoulderRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.client.render.player.MountedPlayerRenderer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
    @Unique private PokemonOnShoulderRenderer cobblemon$shoulders;

    protected PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cobblemon$addShoulderLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        cobblemon$shoulders = new PokemonOnShoulderRenderer(this);
        addLayer(cobblemon$shoulders);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void cobblemon$extractPokemonState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        AvatarPokemonState pokemonState = (AvatarPokemonState) state;
        pokemonState.cobblemon$setShoulderGeometry(null);
        pokemonState.cobblemon$setIconGeometry(null);
        pokemonState.cobblemon$setMountedPose(null);
        if (entity instanceof Player player) {
            if (player.getVehicle() instanceof PokemonEntity) state.isCrouching = false;
            pokemonState.cobblemon$setMountedPose(MountedPlayerRenderer.INSTANCE.extract(player, state));
            cobblemon$shoulders.extract(player, state, partialTicks);
            pokemonState.cobblemon$setIconGeometry(ClientPlayerIcon.Companion.extractPlayerIcon(player, partialTicks));
        }
    }
}
