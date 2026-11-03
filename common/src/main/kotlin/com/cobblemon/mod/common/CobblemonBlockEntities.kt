/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.block.entity.*
import com.cobblemon.mod.common.block.habitat.HabitatBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockBuilder
import com.cobblemon.mod.common.platform.PlatformRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

object CobblemonBlockEntities : PlatformRegistry<Registry<BlockEntityType<*>>, ResourceKey<Registry<BlockEntityType<*>>>, BlockEntityType<*>>() {

    override val registry: Registry<BlockEntityType<*>> = BuiltInRegistries.BLOCK_ENTITY_TYPE
    override val resourceKey: ResourceKey<Registry<BlockEntityType<*>>> = Registries.BLOCK_ENTITY_TYPE

    private fun <T : BlockEntity> type(factory: BlockEntityType.BlockEntitySupplier<T>, vararg blocks: Block): BlockEntityType<T> =
        BlockEntityType(factory, blocks.toSet())

    @JvmField
    val HEALING_MACHINE: BlockEntityType<HealingMachineBlockEntity> = this.create("healing_machine", type(::HealingMachineBlockEntity, CobblemonBlocks.HEALING_MACHINE))

    @JvmField
    val PC: BlockEntityType<PCBlockEntity> = this.create("pc", type(::PCBlockEntity, CobblemonBlocks.PC))

    @JvmField
    val LECTERN: BlockEntityType<LecternBlockEntity> = this.create("lectern", type(::LecternBlockEntity, CobblemonBlocks.LECTERN))

    @JvmField
    val DISC_SHELF: BlockEntityType<DiscShelfBlockEntity> = this.create("disc_shelf", type(::DiscShelfBlockEntity, CobblemonBlocks.DISC_SHELF))

    @JvmField
    val BERRY: BlockEntityType<BerryBlockEntity> = this.create("berry", type(::BerryBlockEntity, *CobblemonBlocks.berries().values.toTypedArray()))

    @JvmField
    val PASTURE: BlockEntityType<PokemonPastureBlockEntity> = this.create("pasture", type(::PokemonPastureBlockEntity, CobblemonBlocks.PASTURE))
    @JvmField
    val SIGN: BlockEntityType<CobblemonSignBlockEntity> = this.create("sign", type(::CobblemonSignBlockEntity, CobblemonBlocks.APRICORN_SIGN, CobblemonBlocks.APRICORN_WALL_SIGN, CobblemonBlocks.SACCHARINE_SIGN, CobblemonBlocks.SACCHARINE_WALL_SIGN))
    @JvmField
    val HANGING_SIGN: BlockEntityType<CobblemonHangingSignBlockEntity> = this.create("hanging_sign", type(::CobblemonHangingSignBlockEntity, CobblemonBlocks.APRICORN_HANGING_SIGN, CobblemonBlocks.APRICORN_WALL_HANGING_SIGN, CobblemonBlocks.SACCHARINE_HANGING_SIGN, CobblemonBlocks.SACCHARINE_WALL_HANGING_SIGN))
    @JvmField
    val TM_MACHINE: BlockEntityType<TMMachineBlockEntity> = this.create("tm_machine", type(::TMMachineBlockEntity, CobblemonBlocks.TM_MACHINE))

    @JvmField
    val HABITAT_BLOCK: BlockEntityType<HabitatBlockEntity> = this.create("habitat_block",
            type(::HabitatBlockEntity, CobblemonBlocks.HABITAT_BLOCK)
    )

    @JvmField
    val GILDED_CHEST: BlockEntityType<GildedChestBlockEntity> = this.create("chest", type(::GildedChestBlockEntity,
        CobblemonBlocks.GILDED_CHEST,
        CobblemonBlocks.BLUE_GILDED_CHEST,
        CobblemonBlocks.YELLOW_GILDED_CHEST,
        CobblemonBlocks.PINK_GILDED_CHEST,
        CobblemonBlocks.BLACK_GILDED_CHEST,
        CobblemonBlocks.WHITE_GILDED_CHEST,
        CobblemonBlocks.GREEN_GILDED_CHEST,
        CobblemonBlocks.GIMMIGHOUL_CHEST
    ))

    @JvmField
    val FOSSIL_MULTIBLOCK: BlockEntityType<FossilMultiblockEntity> = this.create("fossil_multiblock",
        type({ pos, state -> FossilMultiblockEntity(pos, state, FossilMultiblockBuilder(pos)) },
            CobblemonBlocks.MONITOR
        )
    )

    @JvmField
    val RESTORATION_TANK: BlockEntityType<RestorationTankBlockEntity> = this.create("restoration_tank",
        type({ pos, state -> RestorationTankBlockEntity(pos, state, FossilMultiblockBuilder(pos)) },
            CobblemonBlocks.RESTORATION_TANK
        )
    )

    @JvmField
    val FOSSIL_ANALYZER: BlockEntityType<FossilAnalyzerBlockEntity> = this.create("fossil_analyzer",
        type({ pos, state -> FossilAnalyzerBlockEntity(pos, state, FossilMultiblockBuilder(pos)) },
            CobblemonBlocks.FOSSIL_ANALYZER
        )
    )

    @JvmField
    val DISPLAY_CASE: BlockEntityType<DisplayCaseBlockEntity> = this.create("display_case",
        type(::DisplayCaseBlockEntity, CobblemonBlocks.DISPLAY_CASE)
    )

    // TODO after 1.7
//    @JvmField
//    val INCENSE_SWEET: BlockEntityType<SweetIncenseBlockEntity> = this.create("incense_sweet",
//            type(::SweetIncenseBlockEntity, CobblemonBlocks.INCENSE_SWEET)
//    )

    @JvmField
    val CAMPFIRE: BlockEntityType<CampfireBlockEntity> = this.create("campfire_pot",
            type(::CampfireBlockEntity,
                CobblemonBlocks.CAMPFIRE,
                CobblemonBlocks.SOUL_CAMPFIRE
            )
    )

    @JvmField
    val POKE_SNACK: BlockEntityType<PokeSnackBlockEntity> = this.create(
        "poke_snack",
        type(::PokeSnackBlockEntity, CobblemonBlocks.POKE_SNACK, CobblemonBlocks.POKE_CAKE)
    )
}
