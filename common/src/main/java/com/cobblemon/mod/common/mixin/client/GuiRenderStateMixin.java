/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.ScopedGuiBlur;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements ScopedGuiBlur.State {
    @Shadow private int firstStratumAfterBlur;
    @Unique private final List<ScreenRectangle> cobblemon$blurRegions = new ArrayList<>();

    @Override public boolean cobblemon$hasBlurBoundary() { return firstStratumAfterBlur != Integer.MAX_VALUE; }
    @Override public List<ScreenRectangle> cobblemon$blurRegions() { return cobblemon$blurRegions; }

    @Inject(method = "reset", at = @At("HEAD"))
    private void cobblemon$resetBlur(CallbackInfo ci) { cobblemon$blurRegions.clear(); }
}
