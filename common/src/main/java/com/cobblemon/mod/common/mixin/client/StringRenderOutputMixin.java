/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.TextClipping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Capture dialogue reveal limits while extracting, before the deferred text state is prepared. */
@Mixin(GuiGraphicsExtractor.class)
public class StringRenderOutputMixin {
    @ModifyVariable(
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"), argsOnly = true
    )
    private FormattedCharSequence cobblemon$captureVisibleText(FormattedCharSequence text) {
        return TextClipping.captureVisibleText(text);
    }
}
