/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.client.gui;

import com.cobblemon.mod.common.client.gui.pc.*;
import com.cobblemon.mod.common.client.gui.summary.SummaryButton;
import com.cobblemon.mod.common.client.gui.summary.widgets.MarkingButton;
import com.cobblemon.mod.common.client.gui.summary.widgets.MarkingsWidget;
import com.cobblemon.mod.common.client.gui.summary.widgets.PartyWidget;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.stats.StatWidget;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class StorageSummaryInputTest {
    private static final Identifier TEXTURE = Identifier.parse("cobblemon:test");

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    // Only constructors requiring live Minecraft/storage/species are skipped.
    // Hit tests and native ordered screen-child selection are real methods.
    private static <T> T allocate(Class<T> type) throws Exception {
        return type.cast(((Unsafe) field(Unsafe.class, "theUnsafe").get(null)).allocateInstance(type));
    }

    private static void set(Object object, Class<?> owner, String name, Object value) throws Exception {
        field(owner, name).set(object, value);
    }

    private static <T extends AbstractWidget> T bounds(T widget, int x, int y, int width, int height) throws Exception {
        set(widget, AbstractWidget.class, "x", x);
        set(widget, AbstractWidget.class, "y", y);
        set(widget, AbstractWidget.class, "width", width);
        set(widget, AbstractWidget.class, "height", height);
        widget.active = true;
        widget.visible = true;
        return widget;
    }

    private static AbstractContainerEventHandler screen(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() { return List.of(children); }
        };
    }

    private static SummaryButton button(float x, float y, int width, int height, float scale) {
        return new SummaryButton(x, y, width, height, ignored -> {}, Component.empty(), TEXTURE,
                null, ignored -> true, ignored -> true, true, true, true, true, 1F, scale);
    }

    @Test void storageDefersOnlyToVisibleOptionsAndWallpaperWhileRetainingItsExistingRegions() throws Exception {
        var pc = allocate(PCGUI.class);
        var storage = bounds(allocate(StorageWidget.class), 85, 27, 263, 155);
        set(storage, StorageWidget.class, "pcGui", pc);
        var sort = new IconButton(92, 31, 20, 20, TEXTURE, null, null, "sort", ignored -> {});
        var wallpaperButton = new IconButton(242, 31, 20, 20, TEXTURE, null, null, "wallpaper", ignored -> {});
        var wallpaper = bounds(allocate(WallpapersScrollingWidget.class), 274, 29, 68, 146);
        set(pc, PCGUI.class, "optionButtons", List.of(sort, wallpaperButton));
        set(pc, PCGUI.class, "wallpaperWidget", wallpaper);
        var screen = screen(storage, wallpaper, wallpaperButton, sort);
        sort.visible = false;
        wallpaperButton.visible = false;
        wallpaper.visible = false;
        assertSame(storage, screen.getChildAt(97, 36).orElse(null));
        sort.visible = true;
        wallpaperButton.visible = true;
        assertSame(sort, screen.getChildAt(97, 36).orElse(null));
        assertSame(wallpaperButton, screen.getChildAt(247, 36).orElse(null));
        assertSame(storage, screen.getChildAt(300, 60).orElse(null));
        wallpaper.visible = true;
        assertSame(wallpaper, screen.getChildAt(300, 60).orElse(null));
        wallpaper.visible = false;
        for (var point : new double[][] {{100, 55}, {180, 160}, {310, 170}, {280, 45}}) {
            assertSame(storage, screen.getChildAt(point[0], point[1]).orElse(null),
                    "Box/blank/release/pasture regions retain their existing storage dispatch");
        }
        // Pasture PCs never construct a wallpaper widget or option buttons.
        set(pc, PCGUI.class, "wallpaperWidget", null);
        set(pc, PCGUI.class, "optionButtons", List.of());
        assertSame(storage, screen.getChildAt(300, 60).orElse(null));
        assertTrue(screen.getChildAt(350, 60).isEmpty());
    }

    @Test void filterIconAndTextBothReachTheEditWidgetButHiddenFilterDoesNot() throws Exception {
        var filter = bounds(allocate(FilterWidget.class), 126, 183, 91, 14);
        var screen = screen(filter);
        assertSame(filter, screen.getChildAt(121, 190).orElse(null));
        assertSame(filter, screen.getChildAt(170, 190).orElse(null));
        assertTrue(screen.getChildAt(116, 190).isEmpty());
        filter.visible = false;
        assertTrue(screen.getChildAt(121, 190).isEmpty());
    }

    @Test void partySwapButtonAboveThePanelStillReceivesNativeScreenDispatch() throws Exception {
        var party = bounds(allocate(PartyWidget.class), 216, 24, 114, 113);
        set(party, PartyWidget.class, "swapButton", button(296, 15, 26, 14, 1));
        var screen = screen(party);
        assertSame(party, screen.getChildAt(309, 22).orElse(null));
        assertSame(party, screen.getChildAt(240, 60).orElse(null));
        assertTrue(screen.getChildAt(280, 22).isEmpty());
        party.active = false;
        assertTrue(screen.getChildAt(309, 22).isEmpty());
    }

    @Test void scaledMarkingsDoNotInterceptCosmeticToggleOrAdjacentPcSlots() {
        for (int y : new int[] {96, 102}) {
            var markings = new MarkingsWidget(29, y, null, true);
            for (int i = 0; i < 6; i++) {
                markings.getChildren().add(new MarkingButton(29F + i * 7F, y, 0, true, TEXTURE, ignored -> {}));
            }
            var cosmetic = button(67, y + 11, 12, 12, 0.5F);
            GuiEventListener pcSlot = new GuiEventListener() {
                @Override public boolean isMouseOver(double x, double mouseY) { return x >= 92 && x < 117 && mouseY >= y && mouseY < y + 25; }
                @Override public void setFocused(boolean focused) {}
                @Override public boolean isFocused() { return false; }
            };
            var screen = screen(markings, cosmetic, pcSlot);
            assertSame(markings, screen.getChildAt(32, y + 3).orElse(null));
            assertSame(cosmetic, screen.getChildAt(70, y + 11.5).orElse(null));
            assertSame(pcSlot, screen.getChildAt(98, y + 3).orElse(null));
            assertTrue(screen.getChildAt(35.5, y + 3).isEmpty());
        }
    }

    @Test void visibleOtherStatsPagerIncludesItsOuterPixelsWithoutExpandingOtherTabs() throws Exception {
        var stats = bounds(allocate(StatWidget.class), 77, 12, 134, 148);
        set(stats, StatWidget.class, "statOptions", List.of("stats", "other"));
        set(stats, StatWidget.class, "_statTabIndex", 1);
        stats.setOtherStatLeftButton(button(76, 79, 14, 14, 0.5F));
        stats.setOtherStatRightButton(button(205, 79, 14, 14, 0.5F));
        var screen = screen(stats);
        assertSame(stats, screen.getChildAt(76.5, 82).orElse(null));
        assertSame(stats, screen.getChildAt(211.5, 82).orElse(null));
        stats.setStatTabIndex(0);
        assertTrue(screen.getChildAt(76.5, 82).isEmpty());
        assertTrue(screen.getChildAt(211.5, 82).isEmpty());
    }
}
