/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.cobblemon.mod.common.mixin.accessor.GuiGraphicsExtractorAccessor;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public final class ScopedGuiBlur {
    private ScopedGuiBlur() {}

    public interface State {
        boolean cobblemon$hasBlurBoundary();
        List<ScreenRectangle> cobblemon$blurRegions();
    }

    public static void extract(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1) {
        if (x1 <= x0 || y1 <= y0) return;
        var accessor = (GuiGraphicsExtractorAccessor) context;
        ScreenRectangle rectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(context.pose());
        ScreenRectangle clip = accessor.cobblemon$getScissorStack().peek();
        if (clip != null) rectangle = rectangle.intersection(clip);
        if (rectangle == null) return;
        State state = (State) accessor.cobblemon$getGuiRenderState();
        if (!state.cobblemon$hasBlurBoundary()) {
            context.blurBeforeThisStratum();
        } else if (state.cobblemon$blurRegions().isEmpty()) {
            // A vanilla screen already requested full backdrop blur. Keep that request.
            return;
        }
        state.cobblemon$blurRegions().add(rectangle);
    }

}
