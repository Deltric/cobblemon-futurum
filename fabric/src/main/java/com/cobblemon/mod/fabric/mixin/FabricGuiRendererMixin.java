/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.client.render.ModelPreviewRenderer;
import com.cobblemon.mod.common.client.render.ModelPreviewRenderState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla stores one renderer per state type; NeoForge supplies its own native pool. */
@Mixin(GuiRenderer.class)
public abstract class FabricGuiRendererMixin {
    @Shadow @Final private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PictureInPictureRenderer<?>> cobblemon$addModelPreviewRenderer(List<PictureInPictureRenderer<?>> renderers) {
        List<PictureInPictureRenderer<?>> result = new ArrayList<>(renderers);
        result.add(new ModelPreviewRenderer());
        return result;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cobblemon$finishModelPreviews(CallbackInfo ci) {
        // Native render has drawn every queued blit and reset its GUI state.
        // Reusing a target during prepare would overwrite earlier previews.
        if (pictureInPictureRenderers.get(ModelPreviewRenderState.class) instanceof ModelPreviewRenderer renderer) {
            renderer.endFrame();
        }
    }
}
