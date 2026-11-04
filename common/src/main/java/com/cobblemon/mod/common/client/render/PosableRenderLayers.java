/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.cobblemon.mod.common.mixin.accessor.ModelRenderPipelineAccessor;
import com.cobblemon.mod.common.mixin.accessor.RenderTypeAccessor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.resources.Identifier;

/** Posable layer materials, including opaque emissive layers and culled translucency. */
public final class PosableRenderLayers {
    private PosableRenderLayers() {}

    private static final RenderPipeline ITEM_GLINT = RenderPipeline.builder(
        ModelRenderPipelineAccessor.cobblemon$entitySnippet(), ModelRenderPipelineAccessor.cobblemon$glintSnippet())
        .withLocation(Identifier.fromNamespaceAndPath("cobblemon", "pipeline/posable_cutout_glint"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F).withBindGroupLayout(BindGroupLayouts.SAMPLER1)
        .withColorTargetState(ColorTargetState.DEFAULT).build();

    public static RenderType itemGlint(Identifier texture) {
        return RenderTypeAccessor.cobblemon$create("cobblemon_posable_cutout_glint",
            RenderSetup.builder(ITEM_GLINT).withTexture("Sampler0", texture)
                .withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
                .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING).useLightmap().useOverlay()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup());
    }

    // Additive glows must not write depth: their faded edges would hide water and other translucent terrain drawn
    // behind them. 1.21.1's dragon rays (the bloom) were colour-only, but the 26.3 pipeline writes depth; lightning
    // (the trail) wrote depth in both versions.
    private static final RenderType ALPHA_EYE_TRAIL = glow("alpha_eye_trail",
        ModelRenderPipelineAccessor.cobblemon$lightningSnippet(), RenderPipelines.OIT_LIGHTNING, true);
    private static final RenderType ALPHA_EYE_BLOOM = glow("alpha_eye_bloom",
        ModelRenderPipelineAccessor.cobblemon$dragonRaysSnippet(), RenderPipelines.OIT_DRAGON_RAYS, false);

    private static RenderType glow(String name, RenderPipeline.Snippet snippet, OitPipelineSet oit, boolean sortOnUpload) {
        RenderPipeline pipeline = RenderPipeline.builder(snippet)
            .withLocation(Identifier.fromNamespaceAndPath("cobblemon", "pipeline/" + name))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
            .build();
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).setOitPipelines(oit);
        if (sortOnUpload) setup.sortOnUpload();
        return RenderTypeAccessor.cobblemon$create("cobblemon_" + name, setup.createRenderSetup());
    }

    /** Additive quads for the alpha eye trail, like vanilla lightning but without depth writes. */
    public static RenderType alphaEyeTrail() { return ALPHA_EYE_TRAIL; }

    /** Additive triangles for the alpha eye bloom, like vanilla dragon rays but without depth writes. */
    public static RenderType alphaEyeBloom() { return ALPHA_EYE_BLOOM; }

    private static final RenderPipeline EMISSIVE_OPAQUE = emissivePipeline(false);
    private static final RenderPipeline EMISSIVE_TRANSLUCENT = emissivePipeline(true);
    private static final OitPipelineSet EMISSIVE_OIT = OitPipelineSet.builder(
        "cobblemon_posable_emissive", RenderPipeline.builder(ModelRenderPipelineAccessor.cobblemon$oitEntitySnippet()).withCull(true)
    ).withAccumulateModifier(builder -> builder.withShaderDefine("PER_FACE_LIGHTING")
        .withShaderDefine("EMISSIVE").withBindGroupLayout(BindGroupLayouts.SAMPLER1)).build();

    private static RenderPipeline emissivePipeline(boolean translucent) {
        return RenderPipeline.builder(ModelRenderPipelineAccessor.cobblemon$emissiveSnippet())
            .withLocation(Identifier.fromNamespaceAndPath("cobblemon", "pipeline/posable_emissive_" + (translucent ? "translucent" : "opaque")))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(translucent ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT)
            .withCull(true)
            .build();
    }

    public static RenderType layer(Identifier texture, boolean emissive, boolean translucent, boolean translucentCull) {
        RenderPipeline pipeline = emissive ? (translucent ? EMISSIVE_TRANSLUCENT : EMISSIVE_OPAQUE)
            : translucent ? (translucentCull ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : RenderPipelines.ENTITY_TRANSLUCENT)
            : RenderPipelines.ENTITY_CUTOUT_CULL;
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).withTexture("Sampler0", texture).useOverlay().affectsCrumbling();
        if (!emissive) {
            setup.useLightmap();
            // These legacy branches used vanilla materials with outline support.
            if (!translucent || !translucentCull) setup.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        }
        if (translucent) {
            setup.sortOnUpload().setOitPipelines(emissive ? EMISSIVE_OIT
                : translucentCull ? RenderPipelines.OIT_ENTITY_CULL : RenderPipelines.OIT_ENTITY);
        }
        // Deliberately not memoized: interpolated textures have frame-scoped identities.
        return RenderTypeAccessor.cobblemon$create("cobblemon_posable_layer", setup.createRenderSetup());
    }
}
