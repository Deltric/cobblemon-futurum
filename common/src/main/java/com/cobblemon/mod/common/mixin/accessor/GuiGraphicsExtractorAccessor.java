/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.accessor;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor("guiRenderState")
    GuiRenderState cobblemon$getGuiRenderState();

    @Accessor("scissorStack")
    GuiGraphicsExtractor.ScissorStack cobblemon$getScissorStack();

    @Invoker("componentHoverEffect")
    void cobblemon$componentHoverEffect(Font font, Style style, int mouseX, int mouseY);

    @Invoker("innerBlit")
    void cobblemon$blit(RenderPipeline pipeline, Identifier texture,
                        int x0, int x1, int y0, int y1,
                        float minU, float maxU, float minV, float maxV, int color);
}
