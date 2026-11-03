/*
 * Copyright (C) 2026 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.client.gui.summary;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SummaryButtonInputTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static SummaryButton button(AtomicBoolean rendered, AtomicBoolean clickable) {
        return new SummaryButton(100.5F, 200.5F, 12, 12, ignored -> {}, Component.empty(),
            Identifier.fromNamespaceAndPath("cobblemon", "test"), null,
            ignored -> rendered.get(), ignored -> clickable.get(), true, true, true, true, 1F, 0.5F);
    }

    private static AbstractContainerEventHandler screen(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() { return List.of(children); }
        };
    }

    @Test void nativeRoutingUsesScaledAndCurrentPosition() {
        var button = button(new AtomicBoolean(true), new AtomicBoolean(true));
        var screen = screen(button);
        assertSame(button, screen.getChildAt(103, 203).orElse(null));
        assertTrue(screen.getChildAt(108, 203).isEmpty(), "Unscaled padding must not claim sibling clicks");
        assertTrue(screen.getChildAt(100.25, 203).isEmpty(), "Fractional origin is part of the hit box");
        button.setButtonX(150.5F);
        button.setButtonY(250.5F);
        assertTrue(screen.getChildAt(103, 203).isEmpty());
        assertSame(button, screen.getChildAt(153, 253).orElse(null));
    }

    @Test void hiddenAndUnavailableButtonsDoNotCaptureSiblingInput() {
        var rendered = new AtomicBoolean(true);
        var clickable = new AtomicBoolean(true);
        var first = button(rendered, clickable);
        var behind = button(new AtomicBoolean(true), new AtomicBoolean(true));
        var screen = screen(first, behind);
        assertSame(first, screen.getChildAt(103, 203).orElse(null));
        rendered.set(false);
        assertSame(behind, screen.getChildAt(103, 203).orElse(null));
        rendered.set(true);
        clickable.set(false);
        assertSame(behind, screen.getChildAt(103, 203).orElse(null));
        clickable.set(true);
        first.active = false;
        assertSame(behind, screen.getChildAt(103, 203).orElse(null));
        first.active = true;
        first.visible = false;
        assertSame(behind, screen.getChildAt(103, 203).orElse(null));
    }
}
