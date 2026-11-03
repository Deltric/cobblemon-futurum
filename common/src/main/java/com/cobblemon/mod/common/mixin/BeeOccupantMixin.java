/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;


import com.cobblemon.mod.common.CobblemonMemories;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.LegacyHiveEntityTypeCodec;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityProcessor;
import com.cobblemon.mod.common.api.ai.config.task.PathToBeeHiveTaskConfig;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeehiveBlockEntity.Occupant.class)
public abstract class BeeOccupantMixin {

    @Inject(method = "createEntity", at = @At("HEAD"), cancellable = true)
    private void cobblemon$createEntity(Level level, BlockPos pos, CallbackInfoReturnable<Entity> cir) {
        // Handle our typed occupants before vanilla attempts to load incomplete worldgen placeholders.
        final BeehiveBlockEntity.Occupant occupant = (BeehiveBlockEntity.Occupant) (Object) this;
        if (occupant.entityData().type() == CobblemonEntities.POKEMON) {
            CompoundTag compoundTag = occupant.entityData().copyTagWithoutId();
            Entity entity;
            if (compoundTag.contains("pokemonProperties")) {
                // Special case for Pokemon stuffed into a beenest upon world gen.
                // For technical reasons we cannot create them as full entities during world gen.
                // Complete the saved properties when the occupant attempts to exit a hive.
                String properties = compoundTag.getStringOr("pokemonProperties", "");
                PokemonProperties pokemon = PokemonProperties.Companion.parse(properties);
                entity = pokemon.createEntity(level);
            } else {
                entity = EntityType.loadEntityRecursive(occupant.entityData().type(), compoundTag, level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
            }

            if (entity instanceof PokemonEntity pokemonEntity) {
                var state = level.getBlockState(pos);
                var newPos = Vec3.ZERO;
                var newYaw = 0f;
                var brain = pokemonEntity.getBrain();
                if (state.is(BlockTags.BEEHIVES)) {
                    // position in front of the entrance, face away from the block
                    var facing = state.getValue(HorizontalDirectionalBlock.FACING);
                    newPos = Vec3.atCenterOf(pos.relative(facing));
                    newYaw = facing.toYRot();
                    if (brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.REGISTERED)) {
                        brain.setMemory(CobblemonMemories.HIVE_LOCATION, pos); // This needs to be set in the case that the hive was picked up and moved.
                    }
                } else {
                    // Block has likely been destroyed
                    var center = Vec3.atCenterOf(pos);
                    newPos = new Vec3(
                            center.x + level.getRandom().nextFloat() * 0.6 - 0.3,
                            center.y + level.getRandom().nextFloat() * 0.6 - 0.3,
                            center.z + level.getRandom().nextFloat() * 0.6 - 0.3
                    );
                    newYaw = level.getRandom().nextFloat() * 360F;
                    if (brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.REGISTERED)) {
                        brain.eraseMemory(CobblemonMemories.HIVE_LOCATION);
                    }
                }
                entity.yRotO = newYaw;
                entity.setPos(newPos);
                // Do honey logic
                if (brain.checkMemory(CobblemonMemories.HAS_NECTAR, MemoryStatus.REGISTERED)) {
                    var hasNectar = brain.getMemory(CobblemonMemories.HAS_NECTAR).orElse(false);
                    if (hasNectar) {
                        // Remove nectar and reset got to hive cooldown
                        brain.eraseMemory(CobblemonMemories.HAS_NECTAR);
                        if (brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.REGISTERED)) {
                            brain.setMemoryWithExpiry(CobblemonMemories.HIVE_COOLDOWN, true, PathToBeeHiveTaskConfig.STAY_OUT_OF_HIVE_COOLDOWN);
                        }
                        // Increment honey level of the hive
                        if (state.is(BlockTags.BEEHIVES, (blockStateBase) -> blockStateBase.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                            int i = state.getValue(BeehiveBlock.HONEY_LEVEL);
                            if (i < 5) {
                                int j = level.getRandom().nextInt(100) == 0 ? 2 : 1;
                                if (i + j > 5) {
                                    --j;
                                }
                                level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, i + j));
                            }
                        }
                    }
                }
                cir.setReturnValue(entity);
            }
        }
    }

    @Inject(method = "of", at = @At("RETURN"), cancellable = true)
    private static void cobblemon$of(Entity entity, CallbackInfoReturnable<BeehiveBlockEntity.Occupant> cir) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            boolean hasNectar = pokemonEntity.getBrain().checkMemory(CobblemonMemories.HAS_NECTAR, MemoryStatus.REGISTERED)
                    && pokemonEntity.getBrain().getMemory(CobblemonMemories.HAS_NECTAR).orElse(false);
            var saved = cir.getReturnValue();
            var entityTag = saved.entityData().copyTagWithoutId();
            // Vanilla now strips Brain from bees. Pokémon must retain their serialized
            // memories, including nectar, for the release and honey-delivery logic.
            Brain.Packed.CODEC.encodeStart(entity.registryAccess().createSerializationContext(NbtOps.INSTANCE), pokemonEntity.getBrain().pack())
                    .resultOrPartial(Cobblemon.LOGGER::error).ifPresent(brainTag -> entityTag.put("Brain", brainTag));
            cir.setReturnValue(new BeehiveBlockEntity.Occupant(TypedEntityData.of(entity.getType(), entityTag), saved.ticksInHive(), hasNectar ? 2400 : 600));
        }
    }

    @ModifyArg(method = "lambda$static$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TypedEntityData;codec(Lcom/mojang/serialization/Codec;)Lcom/mojang/serialization/Codec;"), index = 0)
    private static Codec<EntityType<?>> cobblemon$legacyPlaceholder(Codec<EntityType<?>> codec) {
        return LegacyHiveEntityTypeCodec.wrap(codec, () -> CobblemonEntities.POKEMON);
    }
}
