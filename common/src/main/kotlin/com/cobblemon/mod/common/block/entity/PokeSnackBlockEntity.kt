/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.entity

import net.minecraft.core.UUIDUtil
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.level.storage.ValueInput
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.api.cooking.getColourMixFromColors
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.cooking.PokeSnackSpawnPokemonEvent
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.fishing.SpawnBait.Effect
import com.cobblemon.mod.common.api.fishing.SpawnBaitEffects
import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.api.spawning.detail.*
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.spawning.spawner.FixedAreaSpawner
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import com.cobblemon.mod.common.block.PokeSnackBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.item.components.BaitEffectsComponent
import com.cobblemon.mod.common.item.components.FoodColourComponent
import com.cobblemon.mod.common.item.components.IngredientComponent
import com.cobblemon.mod.common.net.messages.client.effect.PokeSnackBlockParticlesPacket
import com.cobblemon.mod.common.util.DataKeys
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.*

open class PokeSnackBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CobblemonBlockEntities.POKE_SNACK, pos, state), TintBlockEntity,
    SpawningInfluence {

    companion object {
        const val SPAWNS_PER_BITE = 1
        const val RADIUS = 8
        const val RANDOM_TICKS_BETWEEN_SPAWNS = 2
        const val POKE_SNACK_CRUMBED_ASPECT = "poke_snack_crumbed"
    }

    class Spawner(
        name: String,
        spawnPool: SpawnPool,
        buckets: Map<String, Float>,
        world: ServerLevel,
        position: BlockPos,
        horizontalRadius: Int,
        verticalRadius: Int,
        maxPokemonPerChunk: Float
    ) : FixedAreaSpawner(
        name = name,
        spawnPool = spawnPool,
        buckets = buckets,
        world = world,
        position = position,
        horizontalRadius = horizontalRadius,
        verticalRadius = verticalRadius,
        maxPokemonPerChunk = maxPokemonPerChunk
    )

    override var tint: Int? = null

    val spawner: Spawner by lazy {
        val server = level as ServerLevel

        val baitEffects = getBaitEffects()

        val ctx = PokeSnackSpawnerFactory.Context(
            world = server,
            pos = blockPos,
            horizontalRadius = RADIUS,
            verticalRadius = RADIUS,
            maxPokemonPerChunk = Cobblemon.config.pokeSnackPokemonPerChunk,
            name = "poke_snack_spawner_${server.dimension().identifier()}_$blockPos",
            baitEffects = baitEffects
        )

        PokeSnackSpawnerFactory.create(ctx).also { spawner ->
            spawner.influences.add(this)
        }
    }

    // Only non-herd Pokémon can be spawned from a Poké Snack
    override fun affectSpawnable(detail: SpawnDetail, spawnablePosition: SpawnablePosition) = detail.type in SpawnDetail.pokemonTypes && detail !is PokemonHerdSpawnDetail

    // When a Pokémon spawns, apply the crumbed aspect and play the eating effects. Eat part of the cake, too.
    override fun affectSpawn(action: SpawnAction<*>, entity: Entity) {
        if (entity is PokemonEntity) {
            entity.pokemon.forcedAspects += POKE_SNACK_CRUMBED_ASPECT
            val block = blockState.block as? PokeSnackBlock ?: return
            val level = level ?: return
            val entityPos = entity.blockPosition()

            PokeSnackBlockParticlesPacket(blockPos, entityPos).sendToPlayersAround(
                blockPos.x.toDouble(),
                blockPos.y.toDouble(),
                blockPos.z.toDouble(),
                64.0,
                level.dimension(),
            )

            block.eat(level, blockPos, blockState, null)
        }
    }

    var placedBy: UUID? = null
    var amountSpawned: Int = 0
    var foodColourComponent: FoodColourComponent? = null
    var baitEffectsComponent: BaitEffectsComponent? = null
    var ingredientComponent: IngredientComponent? = null
    var randomTicksUntilNextSpawn: Float = getRandomTicksBetweenSpawns()

    fun initializeFromItemStack(itemStack: ItemStack) {
        foodColourComponent = itemStack.get(CobblemonItemComponents.FOOD_COLOUR)
        ingredientComponent = itemStack.get(CobblemonItemComponents.INGREDIENT)
        if (isLure()) baitEffectsComponent = itemStack.get(CobblemonItemComponents.BAIT_EFFECTS)

        foodColourComponent?.let {
            getColourMixFromColors(it.getColoursAsARGB())?.let(::setTint)
        }
    }

    fun isLure(): Boolean {
        val block = blockState.block
        if (block is PokeSnackBlock) {
            return block.isLure
        }
        return false
    }

    fun randomTick() {
        randomTicksUntilNextSpawn--
        if (randomTicksUntilNextSpawn > 0) {
            return
        }

        val nearestPlayer = level?.getNearestPlayer(
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble(),
            Cobblemon.config.maximumSpawningZoneDistanceFromPlayer.toDouble(),
            false, // true here makes it so creative players aren't considered
        )

        // High simulation distances may cause the player to be null here, we don't want to spawn without a player nearby.
        if (nearestPlayer != null) {
            attemptSpawn(nearestPlayer)
        }

        randomTicksUntilNextSpawn = getRandomTicksBetweenSpawns()
    }

    fun attemptSpawn(player: Player) {
        val cause = SpawnCause(spawner = spawner, entity = player)
        val zoneInput = spawner.getZoneInput(cause)
        val spawnAction = spawner.calculateSpawnActionsForArea(zoneInput, 1).firstOrNull() ?: return

        CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_PRE.postThen(
            PokeSnackSpawnPokemonEvent.Pre(this, spawnAction),
            { },
            { event ->
                spawnAction.complete()
                val result = spawnAction.future
                val resultingSpawn = result.get()

                if (resultingSpawn is EntitySpawnResult) {
                    val pokemonEntity = resultingSpawn.entities.firstOrNull() as PokemonEntity
                    CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.post(
                        PokeSnackSpawnPokemonEvent.Post(this, spawnAction, pokemonEntity)
                    )
                }
            }
        )
    }

    fun getRandomTicksBetweenSpawns(): Float {
        val biteTimeMultiplier = getBiteTimeMultiplier()
        return (RANDOM_TICKS_BETWEEN_SPAWNS * biteTimeMultiplier).coerceAtLeast(1F)
    }

    fun getBiteTimeMultiplier(): Float {
        val baitEffects = getBaitEffects()
        val biteTimeEffects = baitEffects.filter { it.type == SpawnBait.Effects.BITE_TIME }
        if (biteTimeEffects.isEmpty()) return 1F

        val biteTimeEffect = biteTimeEffects.random()
        if (Math.random() > biteTimeEffect.chance) {
            return 1F
        }

        return 1F - biteTimeEffect.value.toFloat()
    }

    fun toItemStack(): ItemStack {
        val stack = ItemStack(this.blockState.block)

        if (isLure() && baitEffectsComponent != null) {
            stack.set(CobblemonItemComponents.BAIT_EFFECTS, baitEffectsComponent)
        }

        if (foodColourComponent != null) {
            stack.set(CobblemonItemComponents.FOOD_COLOUR, foodColourComponent)
        }

        if (ingredientComponent != null) {
            stack.set(CobblemonItemComponents.INGREDIENT, ingredientComponent)
        }

        return stack
    }

    /**
     * Combine all the [SpawnBait.Effect] values from the [baitEffectsComponent] data.
     */
    fun getBaitEffects(): List<Effect> {
        return baitEffectsComponent
            ?.effects
            ?.mapNotNull(SpawnBaitEffects::getFromIdentifier)
            ?.flatMap { it.effects }
            .orEmpty()
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        saveTint(output)
        output.putInt(DataKeys.AMOUNT_SPAWNED, amountSpawned)
        output.storeNullable(DataKeys.FOOD_COLOUR, CobblemonItemComponents.FOOD_COLOUR.codec()!!, foodColourComponent)
        output.storeNullable(DataKeys.BAIT_EFFECTS, CobblemonItemComponents.BAIT_EFFECTS.codec()!!, baitEffectsComponent)
        output.storeNullable(DataKeys.INGREDIENTS, CobblemonItemComponents.INGREDIENT.codec()!!, ingredientComponent)
        output.putFloat(DataKeys.TICKS_UNTIL_NEXT_SPAWN, randomTicksUntilNextSpawn)
        output.storeNullable(DataKeys.PLACED_BY, UUIDUtil.CODEC, placedBy)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        loadTint(input)
        amountSpawned = input.getIntOr(DataKeys.AMOUNT_SPAWNED, 0)
        input.read(DataKeys.FOOD_COLOUR, CobblemonItemComponents.FOOD_COLOUR.codec()!!).ifPresent { foodColourComponent = it }
        input.read(DataKeys.BAIT_EFFECTS, CobblemonItemComponents.BAIT_EFFECTS.codec()!!).ifPresent { baitEffectsComponent = it }
        input.read(DataKeys.INGREDIENTS, CobblemonItemComponents.INGREDIENT.codec()!!).ifPresent { ingredientComponent = it }
        randomTicksUntilNextSpawn = input.getFloatOr(DataKeys.TICKS_UNTIL_NEXT_SPAWN, randomTicksUntilNextSpawn)
        input.read(DataKeys.PLACED_BY, UUIDUtil.CODEC).ifPresent { placedBy = it }
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registryLookup)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
