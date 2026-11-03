/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex;

import com.cobblemon.mod.common.client.gui.pokedex.widgets.PokemonInfoWidget;
import java.util.List;
import kotlin.Unit;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.server.Bootstrap;
import sun.misc.Unsafe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonInfoWidgetInputTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // The widget only keeps its parent screen for later callbacks; routing does not construct a screen.
    private static PokedexGUI parent() throws Exception {
        var field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (PokedexGUI) ((Unsafe) field.get(null)).allocateInstance(PokedexGUI.class);
    }

    private static AbstractContainerEventHandler screen(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() {
                return List.of(children);
            }
        };
    }

    @Test void nativeScreenRoutesTheVisibleCryAndAnimationButtonsToTheirParent() throws Exception {
        for (int[] origin : new int[][] {{0, 0}, {180, 28}, {320, 140}}) {
            var info = new PokemonInfoWidget(parent(), origin[0], origin[1], form -> Unit.INSTANCE);
            var screen = screen(info);
            // Centers of the real half-scale cry and previous/next animation controls.
            for (double x : new double[] {118, 6.5, 21.5}) {
                double mouseX = origin[0] + x;
                double mouseY = origin[1] + 86;
                assertTrue(info.getChildren().stream().anyMatch(child ->
                    child instanceof ScaledButton button && button.isButtonHovered(mouseX, mouseY)));
                assertSame(info, screen.getChildAt(mouseX, mouseY).orElse(null));
            }
            // The description/stat panel beneath the portrait must remain independently clickable.
            assertTrue(screen.getChildAt(origin[0] + 20, origin[1] + 110).isEmpty());
        }
    }

    @Test void suppressedPortraitDoesNotCaptureTheMovesOverlay() throws Exception {
        var info = new PokemonInfoWidget(parent(), 180, 28, form -> Unit.INSTANCE);
        GuiEventListener moves = new GuiEventListener() {
            @Override public boolean isMouseOver(double x, double y) { return true; }
            @Override public void setFocused(boolean focused) {}
            @Override public boolean isFocused() { return false; }
        };
        var screen = screen(info, moves);
        assertSame(info, screen.getChildAt(200, 60).orElse(null));
        info.setSuppressViewport(true);
        assertSame(moves, screen.getChildAt(200, 60).orElse(null));
        assertSame(moves, screen.getChildAt(298, 114).orElse(null));
        info.setSuppressViewport(false);
        assertSame(info, screen.getChildAt(298, 114).orElse(null));
    }
}
