/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.EnumMap;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public final class ModelPreviewRenderer extends PictureInPictureRenderer<ModelPreviewRenderState> {
    private final EnumMap<ModelPreviewRenderState.LightingMode, GpuBuffer> lights;
    private final @Nullable PreviewRenderSlots<ModelPreviewRenderer> slots;
    private final boolean ownsLights;
    private final Projection previewProjection = new Projection();
    private @Nullable ProjectionMatrixBuffer previewProjectionBuffer;
    private int screenGuiScale;
    private int textureScale;

    public ModelPreviewRenderer() { this(true); }

    /** NeoForge already pools independent native targets through its registration event. */
    public static ModelPreviewRenderer singleTarget() { return new ModelPreviewRenderer(false); }

    private ModelPreviewRenderer(boolean pooled) {
        this.lights = new EnumMap<>(ModelPreviewRenderState.LightingMode.class);
        this.ownsLights = true;
        this.slots = pooled ? new PreviewRenderSlots<>(() -> new ModelPreviewRenderer(lights), ModelPreviewRenderer::close) : null;
        addLights(ModelPreviewRenderState.LightingMode.PORTRAIT, new Vector3f(0.2F, 1, -1), new Vector3f(0.1F, 0, 8));
        addLights(ModelPreviewRenderState.LightingMode.PROFILE, new Vector3f(-1, 1, 1), new Vector3f(1.3F, -1, 1));
        addLights(ModelPreviewRenderState.LightingMode.POKE_BALL, new Vector3f(2.2F, 4, -4), new Vector3f(1.1F, -4, 7));
    }

    /** Each slot gets the native superclass's independent color/depth textures. */
    private ModelPreviewRenderer(EnumMap<ModelPreviewRenderState.LightingMode, GpuBuffer> lights) {
        this.lights = lights;
        this.slots = null;
        this.ownsLights = false;
    }

    @Override
    public void prepare(ModelPreviewRenderState state, GuiRenderState guiState,
                        FeatureRenderDispatcher dispatcher, int guiScale) {
        if (slots != null) {
            slots.acquire().prepare(state, guiState, dispatcher, guiScale);
        } else {
            screenGuiScale = guiScale;
            textureScale = PreviewRenderResolution.textureScale(state.pose(), guiScale);
            super.prepare(state, guiState, dispatcher, textureScale);
        }
    }

    /** The queued native texture views remain alive through all GUI draw passes. */
    public void endFrame() {
        if (slots != null) slots.endFrame();
    }

    private void addLights(ModelPreviewRenderState.LightingMode mode, Vector3f first, Vector3f second) {
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Cobblemon preview lights " + mode,
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(),
                Std140Builder.onStack(stack, Lighting.UBO_SIZE).putVec3(first).putVec3(second).get());
        }
        lights.put(mode, buffer);
    }

    @Override public void close() {
        super.close();
        if (previewProjectionBuffer != null) previewProjectionBuffer.close();
        if (slots != null) slots.close();
        if (ownsLights) {
            lights.values().forEach(GpuBuffer::close);
            lights.clear();
        }
    }

    @Override public Class<ModelPreviewRenderState> getRenderStateClass() { return ModelPreviewRenderState.class; }
    @Override protected String getTextureLabel() { return "cobblemon_model_preview"; }
    @Override protected float getTranslateY(int height, int guiScale) { return height / 2.0F; }

    @Override
    protected void renderToTexture(ModelPreviewRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        if (textureScale != screenGuiScale) {
            if (previewProjectionBuffer == null) {
                previewProjectionBuffer = new ProjectionMatrixBuffer("Cobblemon preview projection");
            }
            PreviewRenderResolution.setupProjection(previewProjection,
                (state.x1() - state.x0()) * textureScale, (state.y1() - state.y0()) * textureScale,
                screenGuiScale, textureScale);
            RenderSystem.setProjectionMatrix(previewProjectionBuffer.getBuffer(previewProjection), ProjectionType.ORTHOGRAPHIC);
        }
        RenderSystem.setShaderLights(lights.get(state.lighting()).slice());
        // The extracted vertices already contain the preview's scale and 3D rotations.
        // Undo the PiP origin and Z flip, then place the recorded rectangle in its texture.
        pose.translate(-(state.x0() + state.x1()) / 2.0F, -(state.y0() + state.y1()) / 2.0F, 0);
        pose.scale(1, 1, -1);
        state.geometry().submit(pose, collector);
    }
}
