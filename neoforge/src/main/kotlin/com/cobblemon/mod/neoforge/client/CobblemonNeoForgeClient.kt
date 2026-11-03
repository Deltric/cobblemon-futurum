/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.neoforge.client

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonClientImplementation
import com.cobblemon.mod.common.BakingOverride
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel
import net.minecraft.client.resources.model.geometry.QuadCollection
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonClient.pokedexUsageContext
import com.cobblemon.mod.common.client.CobblemonClient.reloadCodedAssets
import com.cobblemon.mod.common.client.gui.config.CobblemonConfigScreen
import com.cobblemon.mod.common.client.keybind.CobblemonKeyBinds
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases
import com.cobblemon.mod.common.item.PokedexItem
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.particle.SnowstormParticleType
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.isUsingPokedex
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ReloadableResourceManager
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.ClientHooks
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent
import com.cobblemon.mod.common.api.snowstorm.ParticleMaterials
import com.cobblemon.mod.common.client.render.ModelPreviewRenderer
import com.cobblemon.mod.common.client.render.ModelPreviewRenderState
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent
import com.cobblemon.mod.common.client.render.SnowstormParticleGroup
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.common.NeoForge

object CobblemonNeoForgeClient : CobblemonClientImplementation {

    private data class ClientReloader(val id: Identifier, val listener: PreparableReloadListener, val dependencies: List<Identifier>)
    private val clientReloaders = mutableListOf<ClientReloader>()

    fun init(modEventBus: IEventBus) {
        with(modEventBus) {
            addListener(::onClientSetup)
            addListener(::onKeyMappingRegister)
            addListener(::onRegisterParticleProviders)
            addListener(::onRegisterParticleGroups)
            addListener(::onRegisterPictureInPictureRenderers)
            addListener(::registerTextureAtlases)
            addListener(::registerBakingOverrides)
            addListener(::onRegisterReloadListener)
        }
        with(NeoForge.EVENT_BUS) {
            addListener(::onRenderGuiOverlayEvent)
            addListener(::afterRenderGuiOverlayEvent)
            addListener(::afterEndClientTickEvent)
        }
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            CobblemonClient.initialize(this)
        }
        NeoForgeClientPlatformEventHandler.register()

        val modContainer = ModList.get().getModContainerById(Cobblemon.MODID).get()
        modContainer.registerExtensionPoint(IConfigScreenFactory::class.java, object : IConfigScreenFactory {
            override fun createScreen(modContainer: ModContainer, arg: Screen): Screen = CobblemonConfigScreen(arg)
        })
    }

    private fun onRegisterPictureInPictureRenderers(event: RegisterPictureInPictureRenderersEvent) {
        event.register(ModelPreviewRenderState::class.java, ModelPreviewRenderer::singleTarget)
    }

    private fun registerTextureAtlases(event: RegisterTextureAtlasesEvent) {
        CobblemonAtlases.atlases.forEach { event.register(it.config) }
    }

    private fun onRegisterReloadListener(event: AddClientReloadListenersEvent) {
        event.addListener(cobblemonResource("coded_assets"), ResourceManagerReloadListener(::reloadCodedAssets))
        clientReloaders.forEach { event.addListener(it.id, it.listener) }
        clientReloaders.forEach { registration ->
            registration.dependencies.forEach { event.addDependency(it, registration.id) }
        }
    }



    private val bakingOverrideKeys = CobblemonBakingOverrides.models.associateWith { model ->
        StandaloneModelKey<QuadCollection> { model.modelIdentifier.toString() }
    }

    override fun getBakingOverride(model: BakingOverride): QuadCollection =
        checkNotNull(Minecraft.getInstance().modelManager.getStandaloneModel(bakingOverrideKeys.getValue(model))) {
            "Cobblemon standalone model ${model.modelLocation} was not baked"
        }

    private fun registerBakingOverrides(event: ModelEvent.RegisterStandalone) {
        bakingOverrideKeys.forEach { (model, key) ->
            event.register(key, SimpleUnbakedStandaloneModel.quadCollection(model.modelLocation))
        }
    }

    override fun registerLayer(modelLayer: ModelLayerLocation, supplier: Supplier<LayerDefinition>) {
        ClientHooks.registerLayerDefinition(modelLayer, supplier)
    }

    override fun <T : ParticleOptions> registerParticleFactory(type: ParticleType<T>, factory: (SpriteSet) -> ParticleProvider<T>) {
        throw UnsupportedOperationException("Forge can't store these early, use CobblemonForgeClient#onRegisterParticleProviders")
    }

    override fun registerBlockColors(provider: BlockTintSource, vararg blocks: Block) {
        Minecraft.getInstance().blockColors.register(listOf(provider), *blocks)
    }

    override fun <T : BlockEntity, S : net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> registerBlockEntityRenderer(type: BlockEntityType<out T>, factory: BlockEntityRendererProvider<T, S>) {
        BlockEntityRenderers.register(type, factory)
    }

    override fun <T : Entity> registerEntityRenderer(type: EntityType<out T>, factory: EntityRendererProvider<T>) {
        EntityRenderers.register(type, factory)
    }

    private fun onKeyMappingRegister(event: RegisterKeyMappingsEvent) {
        CobblemonKeyBinds.register(event::register)
    }

    private fun onRegisterParticleGroups(event: RegisterParticleGroupsEvent) {
        ParticleMaterials.RENDER_ORDER.forEach { group ->
            event.register(group) { engine -> SnowstormParticleGroup(engine, group) }
        }
    }

    private fun onRegisterParticleProviders(event: RegisterParticleProvidersEvent) {
        event.registerSpriteSet(CobblemonParticles.SNOWSTORM_PARTICLE_TYPE, SnowstormParticleType::Factory)
    }

    var lastUpdateTime: Long? = null

    private val scannerHiddenLayers = setOf(
        VanillaGuiLayers.CAMERA_OVERLAYS,
        VanillaGuiLayers.CROSSHAIR,
        VanillaGuiLayers.AFTER_CAMERA_DECORATIONS,
        VanillaGuiLayers.HOTBAR,
        VanillaGuiLayers.PLAYER_HEALTH,
        VanillaGuiLayers.ARMOR_LEVEL,
        VanillaGuiLayers.FOOD_LEVEL,
        VanillaGuiLayers.VEHICLE_HEALTH,
        VanillaGuiLayers.AIR_LEVEL,
        VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
        VanillaGuiLayers.EXPERIENCE_LEVEL,
        VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
        VanillaGuiLayers.SELECTED_ITEM_NAME,
        VanillaGuiLayers.SPECTATOR_TOOLTIP,
        VanillaGuiLayers.EFFECTS,
        VanillaGuiLayers.BOSS_OVERLAY,
        VanillaGuiLayers.DEMO_OVERLAY,
        VanillaGuiLayers.SCOREBOARD_SIDEBAR,
        VanillaGuiLayers.OVERLAY_MESSAGE,
        VanillaGuiLayers.TITLE,
        VanillaGuiLayers.CHAT,
        VanillaGuiLayers.TAB_LIST,
        VanillaGuiLayers.SUBTITLE_OVERLAY
    )

    private fun onRenderGuiOverlayEvent(event: RenderGuiLayerEvent.Pre) {
        if (pokedexUsageContext.suppressesHud() && event.name in scannerHiddenLayers) {
            event.isCanceled = true
            return
        }
        if (event.name == VanillaGuiLayers.CHAT) {
            val lastUpdateTime = lastUpdateTime
            if (lastUpdateTime != null) {
                // "Why don't you just use the event.partialDeltaTicks"
                // Well JAMES it's because for some reason the value is like 2.8x too big. Forge bug? Weird event structure? Don't know don't care
                CobblemonClient.beforeChatRender(event.guiGraphics, (System.currentTimeMillis() - lastUpdateTime) / 1000F * 20F)
            }
            this.lastUpdateTime = System.currentTimeMillis()
        }
    }

    private fun afterRenderGuiOverlayEvent(event: RenderGuiEvent.Post) {
        val client = Minecraft.getInstance()
        val player = client.player
        if (player != null) {
            if (player.isUsingPokedex() || pokedexUsageContext.transitionIntervals > 0) {
                if (!player.isUsingItem) pokedexUsageContext.resetState(false)
                pokedexUsageContext.renderUpdate(event.guiGraphics, event.partialTick)
            } else {
                pokedexUsageContext.resetState()
            }
        }
    }

    private fun afterEndClientTickEvent(event: ClientTickEvent.Post) {
        val client = Minecraft.getInstance()
        val player = client.player
        if (player != null) {
            val itemStack = player.mainHandItem
            val offhandStack = player.offhandItem
            if (((itemStack.item is PokedexItem && player.usedItemHand == InteractionHand.MAIN_HAND) ||
                        (offhandStack.item is PokedexItem && player.usedItemHand == InteractionHand.OFF_HAND)) &&
                player.isUsingItem &&
                pokedexUsageContext.scanningGuiOpen
            ) {
                val keyAttack = client.options.keyAttack
                pokedexUsageContext.attackKeyHeld(keyAttack.isDown)
            }
        }
    }

    internal fun registerResourceReloader(id: Identifier, reloader: PreparableReloadListener, dependencies: Collection<Identifier>) {
        clientReloaders.add(ClientReloader(id, reloader, dependencies.toList()))
    }
}
