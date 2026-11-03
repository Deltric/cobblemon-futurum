/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.CompoundTagExtensionsKt;
import com.cobblemon.mod.common.util.CompoundTagUtilities;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.PlayerSpawnerAccessor;
import com.cobblemon.mod.common.api.riding.RidingStyle;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import com.cobblemon.mod.common.api.stats.CobblemonStats;
import com.cobblemon.mod.common.duck.ServerLevelDuck;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PlayerSpawnerAccessor {

    @Shadow
    public abstract ServerLevel level();

    @Shadow
    private long timeEntitySatOnShoulder;

    @Unique
    public PlayerSpawner cobblemon$spawner;

    @Inject(method = "respawnEntityOnShoulder", at = @At("HEAD"), cancellable = true)
    private void cobblemon$removePokemon(CompoundTag nbt, CallbackInfo ci) {
        if (!CompoundTagExtensionsKt.isPokemonEntity(nbt)) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        UUID uuid = CompoundTagUtilities.getPokemonID(nbt);
        if (CompoundTagUtilities.isShoulderPokemon(player.getShoulderEntityRight())
                && uuid.equals(CompoundTagUtilities.getPokemonID(player.getShoulderEntityRight()))) {
            cobblemon$recallPokemon(uuid);
            player.setShoulderEntityRight(new CompoundTag());
        }
        if (CompoundTagUtilities.isShoulderPokemon(player.getShoulderEntityLeft())
                && uuid.equals(CompoundTagUtilities.getPokemonID(player.getShoulderEntityLeft()))) {
            cobblemon$recallPokemon(uuid);
            player.setShoulderEntityLeft(new CompoundTag());
        }
        ci.cancel();
    }

    @Inject(method = "removeEntitiesOnShoulder", at = @At("HEAD"), cancellable = true)
    private void cobblemon$preventPokemonDropping(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        // Death and spectator transitions still recall Pokémon through the hook above.
        if (player.isSpectator() || player.isDeadOrDying()) return;
        if (timeEntitySatOnShoulder + 20L < level().getGameTime()) {
            if (!CompoundTagUtilities.isShoulderPokemon(player.getShoulderEntityLeft())) {
                player.respawnEntityOnShoulder(player.getShoulderEntityLeft());
                player.setShoulderEntityLeft(new CompoundTag());
            }
            if (!CompoundTagUtilities.isShoulderPokemon(player.getShoulderEntityRight())) {
                player.respawnEntityOnShoulder(player.getShoulderEntityRight());
                player.setShoulderEntityRight(new CompoundTag());
            }
        }
        ci.cancel();
    }

    @Unique
    private void cobblemon$recallPokemon(UUID uuid) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player.getUUID(), player.registryAccess());
        for (Pokemon pokemon : party) {
            if (pokemon.getUuid().equals(uuid)) pokemon.recall();
        }
    }

    @Override
    public PlayerSpawner getPlayerSpawner() {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (cobblemon$spawner == null) {
            cobblemon$spawner = PlayerSpawnerFactory.INSTANCE.create(player);
        }
        return cobblemon$spawner;
    }

    @Override
    public void setPlayerSpawner(PlayerSpawner spawner) {
        this.cobblemon$spawner = spawner;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cobblemon$tickSpawner(CallbackInfo ci) {
        if (cobblemon$canSpawn()) {
            getPlayerSpawner().tick();
        }
    }

    @Inject(method = "rideTick", at = @At("HEAD"))
    private void cobblemon$updateOrientationControllerRideTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof OrientationControllable controllableVehicle)) return;
        var shouldUseCustomOrientation = cobblemon$shouldUseCustomOrientation(vehicle);
        controllableVehicle.getOrientationController().setActive(shouldUseCustomOrientation);
    }

    @Unique
    private boolean cobblemon$shouldUseCustomOrientation(Entity entity) {
        if (entity == null) return false;
        if (!(entity instanceof PokemonEntity pokemonEntity)) return false;
        return pokemonEntity.ifRidingAvailableSupply(false, (behaviour, settings, state) -> {
            return behaviour.shouldRoll(settings, state, pokemonEntity);
        });
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    public void cobblemon$resetOrientationOnDismount(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (!(player.getVehicle() instanceof OrientationControllable controllableVehicle)) return;
        controllableVehicle.getOrientationController().setActive(false);
    }

    //TODO: Switch to sending out on load instead of preventing saving
    @WrapOperation(method = "saveParentVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasExactlyOnePlayerPassenger()Z"))
    private boolean cobblemon$cancelSavingPokemonMounts(Entity rootVehicle, Operation<Boolean> original) {
        return !(rootVehicle instanceof PokemonEntity) && original.call(rootVehicle);
    }

    @WrapOperation(method = "checkRidingStatistics", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;"))
    public Entity cobblemon$checkRidingStatistics(ServerPlayer player, Operation<Entity> original, @Local int distance) {
        var entity = original.call(player);
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return entity;
        }

        var ridingController = pokemonEntity.getRidingController();
        if (ridingController == null) return entity;

        var activeContext = ridingController.getContext();
        if (activeContext == null) return entity;

        var ridingStyle = activeContext.getStyle();
        CobblemonStats.CobblemonStat stat = switch (ridingStyle) {
            case RidingStyle.LAND -> CobblemonStats.RIDING_LAND;
            case RidingStyle.AIR -> CobblemonStats.RIDING_AIR;
            case RidingStyle.LIQUID -> CobblemonStats.RIDING_LIQUID;
            default -> null;
        };

        if (stat != null) {
            player.awardStat(CobblemonStats.getStat(stat), distance);
        }

        return entity;
    }

    /**
     * Gets whether we should spawn Cobblemon entities here.
     *
     * @return true if we can spawn cobblemon in the current level,
     *         false otherwise
     */
    @SuppressWarnings("resource")
    @Unique
    private boolean cobblemon$canSpawn() {
        return Cobblemon.config.getEnableSpawning() &&
                this.level().getGameRules().get(CobblemonGameRules.DO_POKEMON_SPAWNING) &&
                // intentionally last as this is a more expensive check, even with all the caching
                !Cobblemon.config.getWorldSpawningBlocklist().contains(
                        ((ServerLevelDuck) this.level()).cobblemon$getLevelResourceLocation()
                );
    }
}
