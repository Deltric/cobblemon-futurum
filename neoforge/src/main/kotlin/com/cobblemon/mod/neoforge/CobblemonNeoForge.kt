/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.neoforge

import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.predicate.CobblemonEntitySubPredicates
import com.cobblemon.mod.common.CobblemonMobEffects
import com.cobblemon.mod.common.api.net.serializers.*
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.loot.LootInjector
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.didSleep
import com.cobblemon.mod.common.util.adapters.RegistryOpsProvider
import com.cobblemon.mod.common.world.CobblemonStructures
import com.cobblemon.mod.common.world.feature.CobblemonFeatures
import com.cobblemon.mod.common.world.placementmodifier.CobblemonPlacementModifierTypes
import com.cobblemon.mod.common.world.predicate.CobblemonBlockPredicates
import com.cobblemon.mod.common.world.structureprocessors.CobblemonProcessorTypes
import com.cobblemon.mod.common.world.structureprocessors.CobblemonStructureProcessorListOverrides
import com.cobblemon.mod.neoforge.client.CobblemonNeoForgeClient
import com.cobblemon.mod.neoforge.event.NeoForgePlatformEventHandler
import com.cobblemon.mod.neoforge.net.CobblemonNeoForgeNetworkManager
import com.cobblemon.mod.neoforge.permission.ForgePermissionValidator
import com.cobblemon.mod.neoforge.worldgen.CobblemonBiomeModifiers
import com.mojang.brigadier.arguments.ArgumentType
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.core.component.DataComponents
import com.cobblemon.mod.common.api.item.CobblemonComposting
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.KnownPack
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.Pack.Position
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.stats.Stats
import net.minecraft.tags.TagKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.CreativeModeTab.TabVisibility
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory
import net.minecraft.world.level.gamerules.GameRuleType
import net.minecraft.world.flag.FeatureFlagSet
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.serialization.Codec
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FlowerPotBlock
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.InterModComms
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.NeoForgeMod
import net.neoforged.neoforge.event.*
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.resource.JarContentsPackResources
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import java.util.*
import kotlin.reflect.KClass

@Mod(Cobblemon.MODID)
class CobblemonNeoForge(
    private val modEventBus: IEventBus,
    private val dist: Dist
) : CobblemonImplementation {
    override val modAPI = ModAPI.NEOFORGE
    private val hasBeenSynced = hashSetOf<UUID>()

    private val commandArgumentTypes = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Cobblemon.MODID)
    private data class ServerReloader(
        val identifier: Identifier,
        val listener: (HolderLookup.Provider) -> PreparableReloadListener,
        val dependencies: List<Identifier>
    )
    private val reloadableResources = arrayListOf<ServerReloader>()
    private val queuedWork = arrayListOf<() -> Unit>()

    override val networkManager = CobblemonNeoForgeNetworkManager

    init {
        with(modEventBus) {
            NeoForgeShoulderAttachments.register(this)
            this@CobblemonNeoForge.commandArgumentTypes.register(this)
            addListener(this@CobblemonNeoForge::initialize)
            Cobblemon.preInitialize(this@CobblemonNeoForge)
            addListener(CobblemonBiomeModifiers::register)
            addListener(this@CobblemonNeoForge::on)
            addListener(networkManager::registerMessages)
            addListener(EventPriority.HIGH, ::onBuildContents)
            addListener(::onAddPackFindersEvent)
            addListener(::modifyDefaultComponents)
        }
        with(NeoForge.EVENT_BUS) {
            addListener(::onDataPackSync)
            addListener(::onLogin)
            addListener(::onRespawn)
            addListener(::onDimensionChange)
            addListener(::onLogout)
            addListener(::wakeUp)
            addListener(::registerCommands)
            addListener(::onReload)
            addListener(::addCobblemonStructures)
            addListener(::onLootTableLoad)
        }
        NeoForgePlatformEventHandler.register()
        if (dist == Dist.CLIENT) {
            CobblemonNeoForgeClient.init(modEventBus)
        }
    }

    fun addCobblemonStructures(event: ServerAboutToStartEvent) {
        CobblemonStructures.registerJigsaws(event.server)
        CobblemonStructureProcessorListOverrides.register(event.server)
    }

    fun wakeUp(event: PlayerWakeUpEvent) {
        val playerEntity = event.entity as? ServerPlayer ?: return
        playerEntity.didSleep()
    }

    fun initialize(event: FMLCommonSetupEvent) {
        Cobblemon.LOGGER.info("Initializing...")
        event.enqueueWork {
            this.queuedWork.forEach { it.invoke() }
            this.attemptModCompat()
        }
        Cobblemon.initialize()
    }

    // This event gets fired before init, so we need to put resource packs in EARLY
    fun onAddPackFindersEvent(event: AddPackFindersEvent) {
        val modFile = ModList.get().getModContainerById(Cobblemon.MODID).get().modInfo
        Cobblemon.builtinPacks
            .filter { it.neededMods.all(Cobblemon.implementation::isModInstalled) }
            .filter { it.packType == event.packType }
            .forEach {
                val subPath = if (it.packType == PackType.CLIENT_RESOURCES) "resourcepacks" else "datapacks"
                var packLocation = cobblemonResource("$subPath/${it.id}")
                var version = modFile.version

                var pack = Pack.readMetaAndCreate(PackLocationInfo("mod/$packLocation", it.displayName, PackSource.BUILT_IN, Optional.of(KnownPack("neoforge", "mod/$packLocation", version.toString()))),
                    JarContentsPackResources.JarContentsResourcesSupplier(modFile.owningFile.file.contents, packLocation.path),
                    it.packType,
                    PackSelectionConfig(it.activationBehaviour == ResourcePackActivationBehaviour.ALWAYS_ENABLED, Position.TOP, false)
                )

                if (pack == null) {
                    Cobblemon.LOGGER.error("Failed to register built-in pack ${it.id}. If you are in dev you can ignore this")
                    return@forEach
                }

                event.addRepositorySource { it.accept(pack) }
            }
    }

    fun on(event: RegisterEvent) {

        event.register(Registries.BLOCK_PREDICATE_TYPE) {
            CobblemonBlockPredicates.touch()
        }
        event.register(Registries.PLACEMENT_MODIFIER_TYPE) {
            CobblemonPlacementModifierTypes.touch()
        }

        event.register(Registries.STRUCTURE_PROCESSOR) {
            CobblemonProcessorTypes.touch()
        }

        event.register(Registries.ACTIVITY) { registry ->
            CobblemonActivities.activities.forEach {
                registry.register(cobblemonResource(it.name), it)
            }
        }

        event.register(Registries.SENSOR_TYPE) { registry ->
            CobblemonSensors.sensors.forEach { (key, sensorType) ->
                registry.register(cobblemonResource(key), sensorType)
            }
        }

        event.register(Registries.MEMORY_MODULE_TYPE) { registry ->
            CobblemonMemories.memories.forEach { (key, memoryModuleType) ->
                registry.register(cobblemonResource(key), memoryModuleType)
            }
        }

        event.register(Registries.CUSTOM_STAT) { registry ->
            Cobblemon.statistics.registerStats()
            Cobblemon.statistics.stats.forEach { entry ->
                val cobblemonStat = entry.value
                registry.register(cobblemonStat.resourceLocation, cobblemonStat.resourceLocation)
                Stats.CUSTOM.get(cobblemonStat.resourceLocation, cobblemonStat.formatter)
            }
        }
    }

    fun onDataPackSync(event: OnDatapackSyncEvent) {
        val recipients = event.player?.let { listOf(it) } ?: event.playerList.players
        val server = event.playerList.server
        com.cobblemon.mod.common.net.messages.client.cooking.SeasoningRecipeIndexPacket.fromRecipes(server.recipeManager).sendToPlayers(recipients)
        event.player?.let { Cobblemon.dataProvider.sync(it) }
        com.cobblemon.mod.common.net.messages.client.cooking.CookingRecipesPacket.fromRecipes(server.recipeManager).forEach { it.sendToPlayers(recipients) }
    }

    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        (event.entity as? ServerPlayer)?.let(NeoForgeShoulderAttachments::syncFromNative)
        this.hasBeenSynced.add(event.entity.uuid)
    }

    fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        (event.entity as? ServerPlayer)?.let(NeoForgeShoulderAttachments::syncFromNative)
    }

    fun onDimensionChange(event: PlayerEvent.PlayerChangedDimensionEvent) {
        (event.entity as? ServerPlayer)?.let(NeoForgeShoulderAttachments::syncFromNative)
    }

    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        this.hasBeenSynced.remove(event.entity.uuid)
    }

    override fun isModInstalled(id: String) = ModList.get().isLoaded(id)

    override fun environment(): Environment {
        return if (dist.isClient) Environment.CLIENT else Environment.SERVER
    }

    override fun registerPermissionValidator() {
        Cobblemon.permissionValidator = ForgePermissionValidator
    }

    override fun registerSoundEvents() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonSounds.resourceKey) { helper ->
                CobblemonSounds.register { identifier, sounds -> helper.register(identifier, sounds) }
            }
        }
    }

    override fun registerDataComponents() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonItemComponents.resourceKey) { helper ->
                CobblemonItemComponents.register { identifier, ComponentType ->  helper.register(identifier, ComponentType)}
            }
        }
    }

    override fun registerEntityDataSerializers() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS) { helper ->
                helper.register(Vec3DataSerializer.ID, Vec3DataSerializer)
                helper.register(StringSetDataSerializer.ID, StringSetDataSerializer)
                helper.register(PoseTypeDataSerializer.ID, PoseTypeDataSerializer)
                helper.register(RideBoostsDataSerializer.ID, RideBoostsDataSerializer)
                helper.register(PlatformTypeDataSerializer.ID, PlatformTypeDataSerializer)
                helper.register(IdentifierDataSerializer.ID, IdentifierDataSerializer)
                helper.register(UUIDSetDataSerializer.ID, UUIDSetDataSerializer)
                helper.register(NPCPlayerTextureSerializer.ID, NPCPlayerTextureSerializer)
                helper.register(OptionalUUIDDataSerializer.ID, OptionalUUIDDataSerializer)
                helper.register(ShoulderDataSerializer.ID, ShoulderDataSerializer)
            }
        }
    }

    override fun registerBlocks() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonBlocks.resourceKey) { helper ->
                CobblemonBlocks.register { identifier, block ->
                    if (block !is FlowerPotBlock) helper.register(identifier, block)
                }
            }
        }

        MOD_BUS.addListener<RegisterEvent>(EventPriority.LOW) { event ->
            // Register the pots after registering main blocks to solve race condition when pottable blocks are still not registered
            event.register(CobblemonBlocks.resourceKey) { helper ->
                CobblemonBlocks.register { identifier, block ->
                    if (block is FlowerPotBlock) {
                        helper.register(identifier, block)

                        val pottedIdentifier = RegistryAccessResolver.getAccess()
                            .registryOrThrow(Registries.BLOCK)
                            .getKeyOrNull(block.potted)!!

                        (Blocks.FLOWER_POT as FlowerPotBlock).addPlant(pottedIdentifier) { block }
                    }
                }
            }
        }
    }

    override fun registerParticles() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonParticles.resourceKey) { helper ->
                CobblemonParticles.register { identifier, particleType -> helper.register(identifier, particleType) }
            }
        }
    }

    override fun registerMenu() {
        with(modEventBus) {
            addListener<RegisterEvent> { event ->
                event.register(CobblemonMenuType.resourceKey) { helper ->
                    CobblemonMenuType.register { identifier, item -> helper.register(identifier, item) }
                }
            }
        }
    }

    override fun registerRecipeTypes() {
        with(modEventBus) {
            addListener<RegisterEvent> { event ->
                event.register(CobblemonRecipeTypes.resourceKey) { helper ->
                    CobblemonRecipeTypes.register { identifier, item -> helper.register(identifier, item) }
                }
                event.register(Registries.RECIPE_BOOK_CATEGORY) { helper ->
                    CobblemonRecipeCategories.register { identifier, category -> helper.register(identifier, category) }
                }
            }
        }
    }

    override fun registerItems() {
        with(modEventBus) {
            addListener<RegisterEvent> { event ->
                event.register(CobblemonItems.resourceKey) { helper ->
                    CobblemonItems.register { identifier, item -> helper.register(identifier, item) }
                }
            }
            addListener<RegisterEvent> { event ->
                event.register(Registries.CREATIVE_MODE_TAB) { helper ->
                    CobblemonItemGroups.register { holder ->
                        val itemGroup = CreativeModeTab.builder()
                            .title(holder.displayName)
                            .icon(holder.displayIconProvider)
                            .displayItems(holder.entryCollector)
                            .build()
                        helper.register(holder.key, itemGroup)
                        itemGroup
                    }
                }
            }
        }
    }

    override fun registerEntityTypes() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonEntities.resourceKey) { helper ->
                CobblemonEntities.register { identifier, type -> helper.register(identifier, type) }
            }
        }
    }

    override fun registerEntityAttributes() {
        modEventBus.addListener<EntityAttributeCreationEvent> { event ->
            CobblemonEntities.registerAttributes { entityType, builder ->
                builder.add(Attributes.NAME_TAG_DISTANCE)
                    .add(NeoForgeMod.SWIM_SPEED)
                    //.add(ForgeMod.ENTITY_GRAVITY)
                event.put(entityType, builder.build())
            }
        }
    }

    override fun registerBlockEntityTypes() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonBlockEntities.resourceKey) { helper ->
                CobblemonBlockEntities.register { identifier, type -> helper.register(identifier, type) }
            }
        }
    }

    override fun registerPoiTypes() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonPoiTypes.resourceKey) { helper ->
                CobblemonPoiTypes.register { identifier, type -> helper.register(identifier, type) }
            }
        }
    }

    override fun registerVillagers() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonVillagerProfessions.resourceKey) { helper ->
                CobblemonVillagerProfessions.register { identifier, profession -> helper.register(identifier, profession) }
            }
        }
    }

    override fun registerRecipeSerializers() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonRecipeSerializers.resourceKey) { helper ->
                CobblemonRecipeSerializers.register { identifier, feature -> helper.register(identifier, feature) }
            }
        }
    }

    override fun registerWorldGenFeatures() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonFeatures.resourceKey) { helper ->
                CobblemonFeatures.register { identifier, feature -> helper.register(identifier, feature) }
            }
        }
    }

    override fun addFeatureToWorldGen(feature: ResourceKey<PlacedFeature>, step: GenerationStep.Decoration, validTag: TagKey<Biome>?) {
        CobblemonBiomeModifiers.add(feature, step, validTag)
    }

    override fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>> registerCommandArgument(identifier: Identifier, argumentClass: KClass<A>, serializer: ArgumentTypeInfo<A, T>) {

        //This is technically a supplier not a function (it is unused), but we need to explicitly say whether its a supplier or a function
        //Idk how to explicitly say its a supplier, so lets just make it a function by specifying a param
        this.commandArgumentTypes.register(identifier.path) { it ->
            ArgumentTypeInfos.registerByClass(argumentClass.java, serializer)
        }
    }

    private fun registerCommands(e: RegisterCommandsEvent) {
        CobblemonCommands.register(e.dispatcher, e.buildContext, e.commandSelection)
    }

    override fun registerGameRule(identifier: Identifier, category: GameRuleCategory, defaultValue: Boolean): GameRule<Boolean> {
        val rule = GameRule(
            category, GameRuleType.BOOL, BoolArgumentType.bool(),
            { visitor, value -> visitor.visitBoolean(value) }, Codec.BOOL,
            { value -> if (value) 1 else 0 }, defaultValue, FeatureFlagSet.of()
        )
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(Registries.GAME_RULE) { helper -> helper.register(identifier, rule) }
        }
        return rule
    }


    override fun registerCriteria() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonCriteria.resourceKey) { helper ->
                CobblemonCriteria.register { identifier, criteria -> helper.register(identifier, criteria) }
            }
        }
    }

    override fun registerEntitySubPredicates() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonEntitySubPredicates.resourceKey) { helper ->
                CobblemonEntitySubPredicates.register { identifier, criteria -> helper.register(identifier, criteria) }
            }
        }
    }

    override fun registerResourceReloader(
        identifier: Identifier,
        type: PackType,
        dependencies: Collection<Identifier>,
        reloaderFactory: (HolderLookup.Provider) -> PreparableReloadListener
    ) {
        if (type == PackType.SERVER_DATA) {
            this.reloadableResources += ServerReloader(identifier, reloaderFactory, dependencies.toList())
        }
        else {
            CobblemonNeoForgeClient.registerResourceReloader(identifier, reloaderFactory(RegistryAccess.EMPTY), dependencies)
        }
    }

    private fun onReload(e: AddServerReloadListenersEvent) {
        reloadableResources.forEach {
            val lookup = e.serverResources.registryLookup
            e.addListener(it.identifier, RegistryOpsProvider.wrapReloadListener(it.listener(lookup)) { _ -> lookup })
        }
        reloadableResources.forEach { registration ->
            registration.dependencies.forEach { dependency -> e.addDependency(dependency, registration.identifier) }
        }
    }

    override fun server(): MinecraftServer? = ServerLifecycleHooks.getCurrentServer()

    override fun registerCompostable(item: ItemLike, chance: Float) {
        CobblemonComposting.register(item, chance)
    }

    private fun modifyDefaultComponents(event: ModifyDefaultComponentsEvent) {
        CobblemonComposting.forEach { item, compostable ->
            event.modify(item, ModifyDefaultComponentsEvent.Initializer { components, _, _ ->
                components.set(DataComponents.COMPOSTABLE, compostable)
            })
        }
    }

    override fun registerMobEffects() {
        modEventBus.addListener<RegisterEvent> { event ->
            event.register(CobblemonMobEffects.resourceKey) { helper ->
                CobblemonMobEffects.register { identifier, effect ->
                    helper.register(identifier, effect)
                }
            }
        }
    }

    private fun onLootTableLoad(e: LootTableLoadEvent) {
        LootInjector.attemptInjection(e.name, e.registries) { builder -> e.table.addPool(builder.build()) }
    }

    private fun onBuildContents(e: BuildCreativeModeTabContentsEvent) {
        val forgeInject = ForgeItemGroupInject(e)
        CobblemonItemGroups.inject(e.tabKey, forgeInject)
    }

    private fun attemptModCompat() {
        // CarryOn has a tag key for this but for some reason Forge version just doesn't work instead we do this :)
        // See https://github.com/Tschipp/CarryOn/wiki/IMC-support-for-Modders
        if (this.isModInstalled("carryon")) {
            InterModComms.sendTo("carryon", "blacklistEntity") { CobblemonEntities.POKEMON_KEY.toString() }
            InterModComms.sendTo("carryon", "blacklistEntity") { CobblemonEntities.EMPTY_POKEBALL_KEY.toString() }
        }
    }

    private class ForgeItemGroupInject(private val entries: BuildCreativeModeTabContentsEvent) : CobblemonItemGroups.Injector {

        override fun putFirst(item: ItemLike) {
            this.entries.insertFirst(ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS)
        }

        override fun putBefore(item: ItemLike, target: ItemLike) {
            this.entries.insertBefore(
                ItemStack(target),
                ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS
            )
        }

        override fun putAfter(item: ItemLike, target: ItemLike) {
            this.entries.insertAfter(
                ItemStack(target),
                ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS)
        }

        override fun putLast(item: ItemLike) {
            this.entries.accept(ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS)
        }
    }
}
