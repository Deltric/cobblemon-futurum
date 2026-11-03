/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.client

import com.cobblemon.mod.common.CobblemonClientImplementation
import com.cobblemon.mod.common.BakingOverride
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation
import net.minecraft.client.resources.model.geometry.QuadCollection
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonClient.pokedexUsageContext
import com.cobblemon.mod.common.client.CobblemonClient.reloadCodedAssets
import com.cobblemon.mod.common.client.keybind.CobblemonKeyBinds
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases
import com.cobblemon.mod.common.item.PokedexItem
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.particle.SnowstormParticleType
import com.cobblemon.mod.common.platform.events.ClientEntityEvent
import com.cobblemon.mod.common.platform.events.ClientPlayerEvent
import com.cobblemon.mod.common.platform.events.ClientTickEvent
import com.cobblemon.mod.common.platform.events.ItemTooltipEvent
import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.cobblemon.mod.common.platform.events.RenderEvent
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.isUsingPokedex
import com.cobblemon.mod.fabric.CobblemonFabric
import com.mojang.blaze3d.vertex.PoseStack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry
import com.cobblemon.mod.common.api.snowstorm.ParticleMaterials
import com.cobblemon.mod.common.client.render.SnowstormParticleGroup
import net.minecraft.client.particle.ParticleRenderType
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.client.rendering.v1.AtlasRegistry
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

class CobblemonFabricClient: ClientModInitializer, CobblemonClientImplementation {
    private val bakingOverrideKeys = CobblemonBakingOverrides.models.associateWith { model ->
        ExtraModelKey.create<QuadCollection> { model.modelIdentifier.toString() }
    }

    override fun getBakingOverride(model: BakingOverride): QuadCollection =
        checkNotNull((Minecraft.getInstance().modelManager as FabricModelManager).getModel(bakingOverrideKeys.getValue(model))) {
            "Cobblemon extra model ${model.modelLocation} was not baked"
        }

    override fun onInitializeClient() {
        registerParticleFactory(CobblemonParticles.SNOWSTORM_PARTICLE_TYPE, SnowstormParticleType::Factory)
        var previousGroup = ParticleRenderType.ELDER_GUARDIANS
        ParticleMaterials.RENDER_ORDER.forEach { group ->
            ParticleGroupRegistry.register(group) { engine -> SnowstormParticleGroup(engine, group) }
            ParticleGroupRegistry.registerOrdering(previousGroup, group)
            previousGroup = group
        }
        CobblemonClient.initialize(this)
        ModelLoadingPlugin.register { context ->
            bakingOverrideKeys.forEach { (model, key) ->
                context.addModel(key, SimpleUnbakedExtraModel(model.modelLocation) { resolved, baker ->
                    resolved.bakeTopGeometry(resolved.topTextureSlots, baker, BlockModelRotation.IDENTITY)
                })
            }
        }
        CobblemonFabric.networkManager.registerClientHandlers()

        CobblemonAtlases.atlases.forEach { AtlasRegistry.register(it.config) }
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            cobblemonResource("coded_assets"), ResourceManagerReloadListener(::reloadCodedAssets)
        )

        // Preserve the scanner's suppression of the normal HUD while keeping sleep overlays.
        listOf(
            VanillaHudElements.MISC_OVERLAYS,
            VanillaHudElements.CROSSHAIR,
            VanillaHudElements.SPECTATOR_MENU,
            VanillaHudElements.HOTBAR,
            VanillaHudElements.ARMOR_BAR,
            VanillaHudElements.HEALTH_BAR,
            VanillaHudElements.FOOD_BAR,
            VanillaHudElements.AIR_BAR,
            VanillaHudElements.MOUNT_HEALTH,
            VanillaHudElements.INFO_BAR,
            VanillaHudElements.EXPERIENCE_LEVEL,
            VanillaHudElements.HELD_ITEM_TOOLTIP,
            VanillaHudElements.SPECTATOR_TOOLTIP,
            VanillaHudElements.MOB_EFFECTS,
            VanillaHudElements.BOSS_BAR,
            VanillaHudElements.DEMO_TIMER,
            VanillaHudElements.SCOREBOARD,
            VanillaHudElements.OVERLAY_MESSAGE,
            VanillaHudElements.TITLE_AND_SUBTITLE,
            VanillaHudElements.CHAT,
            VanillaHudElements.PLAYER_LIST,
            VanillaHudElements.SUBTITLES
        ).forEach { id ->
            HudElementRegistry.replaceElement(id) { original ->
                HudElement { graphics, tickDelta ->
                    if (!pokedexUsageContext.suppressesHud()) original.extractRenderState(graphics, tickDelta)
                }
            }
        }
        var lastChatUpdateTime: Long? = null
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, cobblemonResource("overlays")) { graphics, _ ->
            if (!pokedexUsageContext.suppressesHud()) {
                val now = System.currentTimeMillis()
                lastChatUpdateTime?.let { CobblemonClient.beforeChatRender(graphics, (now - it) / 1000F * 20F) }
                lastChatUpdateTime = now
            }
        }

        // The scanner stays above the HUD and keeps animating when the normal HUD is hidden.
        HudElementRegistry.addLast(cobblemonResource("pokedex_scanner")) { graphics, tickDelta ->
            val client = Minecraft.getInstance()
            val player = client.player
            if (player != null) {
                if (player.isUsingPokedex() || pokedexUsageContext.transitionIntervals > 0) {
                    if (!player.isUsingItem) pokedexUsageContext.resetState(false)
                    pokedexUsageContext.renderUpdate(graphics, tickDelta)
                } else {
                    pokedexUsageContext.resetState()
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
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
        })

        CobblemonKeyBinds.register(KeyMappingHelper::registerKeyMapping)

        ClientEntityEvents.ENTITY_LOAD.register { entity, level -> PlatformEvents.CLIENT_ENTITY_LOAD.post(ClientEntityEvent.Load(entity, level))}
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, level -> PlatformEvents.CLIENT_ENTITY_UNLOAD.post(ClientEntityEvent.Unload(entity, level))}
        ClientTickEvents.START_CLIENT_TICK.register { client -> PlatformEvents.CLIENT_TICK_PRE.post(ClientTickEvent.Pre(client)) }
        ClientTickEvents.END_CLIENT_TICK.register { client -> PlatformEvents.CLIENT_TICK_POST.post(ClientTickEvent.Post(client)) }
        ClientPlayConnectionEvents.JOIN.register { _, _, client -> client.player?.let { PlatformEvents.CLIENT_PLAYER_LOGIN.post(ClientPlayerEvent.Login(it)) } }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.player?.let { PlatformEvents.CLIENT_PLAYER_LOGOUT.post(ClientPlayerEvent.Logout(it)) } }
        ItemTooltipCallback.EVENT.register { stack, context, type, lines -> PlatformEvents.CLIENT_ITEM_TOOLTIP.post(ItemTooltipEvent(stack, context, type, lines)) }

        LevelRenderEvents.COLLECT_SUBMITS.register { context ->
            PlatformEvents.RENDER.post(RenderEvent(
                stage = RenderEvent.Stage.SUBMIT,
                poseStack = context.poseStack(),
                collector = context.submitNodeCollector(),
                levelState = context.levelState()
            ))
        }

    }

    override fun registerLayer(modelLayer: ModelLayerLocation, supplier: Supplier<LayerDefinition>) {
        ModelLayerRegistry.registerModelLayer(modelLayer) { supplier.get() }
    }

    override fun <T : ParticleOptions> registerParticleFactory(type: ParticleType<T>, factory: (SpriteSet) -> ParticleProvider<T>) {
        ParticleProviderRegistry.getInstance().register(type, ParticleProviderRegistry.PendingParticleProvider { factory(it) })
    }

    override fun registerBlockColors(provider: BlockTintSource, vararg blocks: Block) {
        BlockColorRegistry.register(listOf(provider), *blocks)
    }

    override fun <T : BlockEntity, S : net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> registerBlockEntityRenderer(type: BlockEntityType<out T>, factory: BlockEntityRendererProvider<T, S>) {
        BlockEntityRenderers.register(type, factory)
    }

    override fun <T : Entity> registerEntityRenderer(type: EntityType<out T>, factory: EntityRendererProvider<T>) {
        EntityRendererRegistry.register(type, factory)
    }
}