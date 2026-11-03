/*
 * Copyright (C) 2026 Cobblemon Contributors
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.client.gui.pokedex;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class ScaledButtonRoutingInputTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static AbstractContainerEventHandler host(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() { return List.of(children); }
        };
    }

    private static ScaledButton button(float x, float y, List<Integer> clicks, int action) {
        return new ScaledButton(x, y, 8, 6, null, 0.5F, true, ignored -> clicks.add(action));
    }

    private static MouseButtonEvent click(double x, double y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }

    @Test void nativeFirstHitReachesTheDownArrowInsteadOfTheUnscaledUpRectangle() throws Exception {
        try (var ignored = new SilentClient()) {
            var clicks = new ArrayList<Integer>();
            for (int offset : new int[] {0, 100, 317}) {
                var up = button(offset + 95, offset + 14.5F, clicks, 0);
                var down = button(offset + 95, offset + 19.5F, clicks, 1);
                var host = host(up, down);
                assertSame(down, host.getChildAt(offset + 96, offset + 19.75).orElseThrow());
                assertTrue(host.mouseClicked(click(offset + 96, offset + 19.75, InputConstants.MOUSE_BUTTON_LEFT), false));
                assertSame(down, host.getFocused(), "Successful button dispatch must report consumption");
                assertTrue(host.getChildAt(offset + 96, offset + 18).isEmpty(), "Arrow gap stays transparent");
            }
            assertEquals(List.of(1, 1, 1), clicks);
        }
    }

    @Test void movedScaledBoundsAndBothVisibilityFlagsControlNativeRouting() throws Exception {
        try (var ignored = new SilentClient()) {
            var clicks = new ArrayList<Integer>();
            var moved = button(114, 20, clicks, 1);
            moved.setButtonX(126.5F);
            moved.setButtonY(40.5F);
            var host = host(moved);
            assertTrue(host.getChildAt(116, 21).isEmpty());
            assertSame(moved, host.getChildAt(130.25, 43.25).orElseThrow());
            assertTrue(moved.mouseClicked(click(130.25, 43.25, InputConstants.MOUSE_BUTTON_LEFT), false));
            assertFalse(moved.mouseClicked(click(130.25, 43.25, InputConstants.MOUSE_BUTTON_RIGHT), false));
            assertTrue(host.getChildAt(131, 44).isEmpty(), "Native source-sized rectangle must not remain active");
            moved.setVisible(false);
            assertTrue(host.getChildAt(128, 42).isEmpty());
            moved.setVisible(true);
            moved.visible = false;
            assertTrue(host.getChildAt(128, 42).isEmpty());
            moved.visible = true;
            moved.active = false;
            assertTrue(host.getChildAt(128, 42).isEmpty());
            assertEquals(List.of(1), clicks);
        }
    }

    @Test void aHiddenScaledControlDoesNotMaskALaterSibling() {
        var clicks = new ArrayList<Integer>();
        var hidden = button(10, 20, clicks, 0);
        var visible = button(10, 20, clicks, 1);
        hidden.setVisible(false);
        assertSame(visible, host(hidden, visible).getChildAt(12, 21).orElseThrow());
    }

    // Native Button obtains the sound manager even for a silent subclass. Supply
    // only those inert objects; never create a window, renderer, or audio engine.
    private static final class SilentClient implements AutoCloseable {
        private final Field instance;
        private final Object previous;

        SilentClient() throws Exception {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            var allocator = (Unsafe) unsafeField.get(null);
            var client = (Minecraft) allocator.allocateInstance(Minecraft.class);
            Field soundManager = Minecraft.class.getDeclaredField("soundManager");
            soundManager.setAccessible(true);
            soundManager.set(client, allocator.allocateInstance(SoundManager.class));
            instance = Minecraft.class.getDeclaredField("instance");
            instance.setAccessible(true);
            previous = instance.get(null);
            instance.set(null, client);
        }

        @Override public void close() throws Exception { instance.set(null, previous); }
    }
}
