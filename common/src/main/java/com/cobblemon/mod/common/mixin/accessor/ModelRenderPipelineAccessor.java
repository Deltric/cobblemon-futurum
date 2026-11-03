/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.accessor;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface ModelRenderPipelineAccessor {
    @Accessor("ENTITY_SNIPPET")
    static RenderPipeline.Snippet cobblemon$entitySnippet() { throw new AssertionError(); }
    @Accessor("GLINT_SNIPPET")
    static RenderPipeline.Snippet cobblemon$glintSnippet() { throw new AssertionError(); }
    @Accessor("ENTITY_EMISSIVE_SNIPPET")
    static RenderPipeline.Snippet cobblemon$emissiveSnippet() { throw new AssertionError(); }

    @Accessor("OIT_ENTITY_SNIPPET")
    static RenderPipeline.Snippet cobblemon$oitEntitySnippet() { throw new AssertionError(); }

    @Accessor("BLOCK_SNIPPET")
    static RenderPipeline.Snippet cobblemon$blockSnippet() { throw new AssertionError(); }
}
