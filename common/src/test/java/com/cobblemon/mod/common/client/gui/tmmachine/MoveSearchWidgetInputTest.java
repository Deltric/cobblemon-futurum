/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tmmachine;

import java.util.List;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class MoveSearchWidgetInputTest {
    private static MoveSearchWidget widget(int x, int y) throws Exception {
        // Avoid constructing a live Minecraft font/window. Native screen routing
        // and the production widget's hit test are exercised without rendering.
        var field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var widget = (MoveSearchWidget) ((Unsafe) field.get(null)).allocateInstance(MoveSearchWidget.class);
        widget.setRectangle(91, 14, x, y);
        widget.active = true;
        widget.visible = true;
        return widget;
    }

    private static AbstractContainerEventHandler screen(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() { return List.of(children); }
        };
    }

    @Test void nativeScreenFindsBothTheSearchIconAndTextFieldAfterRepositioning() throws Exception {
        var widget = widget(16, 13);
        var screen = screen(widget);
        for (int[] origin : new int[][] {{16, 13}, {240, 86}, {0, 0}}) {
            widget.setPosition(origin[0], origin[1]);
            // The icon is entirely left of the EditBox rectangle.
            assertSame(widget, screen.getChildAt(origin[0] - 6.5, origin[1] + 6.5).orElse(null));
            assertSame(widget, screen.getChildAt(origin[0] + 20, origin[1] + 6.5).orElse(null));
        }
    }

    @Test void iconGapsAndHiddenOrInactiveSearchDoNotBlockOtherWidgets() throws Exception {
        var widget = widget(100, 50);
        GuiEventListener other = new GuiEventListener() {
            @Override public boolean isMouseOver(double x, double y) { return true; }
            @Override public void setFocused(boolean focused) {}
            @Override public boolean isFocused() { return false; }
        };
        var screen = screen(widget, other);
        assertSame(other, screen.getChildAt(99, 56.5).orElse(null), "Gap between icon and text");
        assertSame(other, screen.getChildAt(89.25, 56.5).orElse(null), "Left of fractional icon edge");
        assertSame(other, screen.getChildAt(93.5, 52.25).orElse(null), "Above fractional icon edge");
        widget.visible = false;
        assertSame(other, screen.getChildAt(93.5, 56.5).orElse(null));
        assertSame(other, screen.getChildAt(120, 56.5).orElse(null));
        widget.visible = true;
        widget.active = false;
        assertSame(other, screen.getChildAt(93.5, 56.5).orElse(null));
        assertSame(other, screen.getChildAt(120, 56.5).orElse(null));
    }
}
