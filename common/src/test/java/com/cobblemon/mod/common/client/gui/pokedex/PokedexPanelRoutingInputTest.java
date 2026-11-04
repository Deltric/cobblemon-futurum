/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex;

import com.cobblemon.mod.common.client.gui.pokedex.widgets.MovesLearnsetWidget;
import com.cobblemon.mod.common.client.gui.pokedex.widgets.StatsWidget;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class PokedexPanelRoutingInputTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static AbstractContainerEventHandler screen(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() { return List.of(children); }
        };
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test void decorativeStatsPanelDoesNotInterceptItsSeparatelyRegisteredRidingArrows() {
        var stats = new StatsWidget(180, 135);
        var screen = screen(stats, stats.getLeftButton(), stats.getRightButton(),
                stats.getLeftSubButton(), stats.getRightSubButton());
        stats.setSelectedStatTypeIndex(StatsWidget.STAT_TYPE_RIDE);
        assertSame(stats.getLeftSubButton(), screen.getChildAt(184.25, 156).orElse(null));
        assertSame(stats.getRightSubButton(), screen.getChildAt(314.75, 156).orElse(null));
        assertSame(stats.getLeftButton(), screen.getChildAt(184.25, 129.5).orElse(null));
        stats.setSelectedStatTypeIndex(StatsWidget.STAT_TYPE_BASE);
        assertTrue(screen.getChildAt(184.25, 156).isEmpty());
    }

    @Test void movesPanelRoutesItsControlsButLeavesTheLowerTabsReachable() {
        var moves = new MovesLearnsetWidget(180, 28);
        var tab = new ScaledButton(190.5F, 181.5F, 16, 16, null, 0.5F, true, ignored -> {});
        var screen = screen(moves, tab);
        assertSame(tab, screen.getChildAt(194.5, 185.5).orElse(null));
        assertSame(moves, screen.getChildAt(200, 75).orElse(null));
        assertSame(moves, screen.getChildAt(270, 158).orElse(null));
        assertTrue(screen.getChildAt(185, 30).isEmpty(), "Decorative header space is not a control");
    }

    private static final class Row extends ObjectSelectionList.Entry<Row> {
        @Override public Component getNarration() { return Component.empty(); }
        @Override public void extractContent(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, float delta) {}
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AbstractSelectionList<?> fillList(MovesLearnsetWidget moves, String name) throws Exception {
        var list = (AbstractSelectionList) field(MovesLearnsetWidget.class, name).get(moves);
        var rows = new ArrayList<Row>();
        for (int i = 0; i < 20; i++) rows.add(new Row());
        // The actual list assigns its native default row height and scroll rate.
        list.replaceEntries(rows);
        return list;
    }

    @Test void pokedexDispatchScrollsEachMovesRegionExactlyOnce() throws Exception {
        var moves = new MovesLearnsetWidget(180, 28);
        var list = fillList(moves, "listWidget");
        var description = fillList(moves, "descriptionWidget");
        // Skip only the outer GUI's live-player/pokedex constructor. Its native
        // children and tab fields reproduce the real active Moves screen.
        var unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        var pokedex = (PokedexGUI) unsafe.allocateInstance(PokedexGUI.class);
        field(Screen.class, "children").set(pokedex, List.of(moves));
        pokedex.setTabInfoIndex(PokedexGUIConstants.TAB_MOVES);
        pokedex.setTabInfoElement(moves);
        assertTrue(pokedex.mouseScrolled(200, 75, 0, -1));
        assertEquals(7.0, list.scrollAmount());
        assertEquals(0.0, description.scrollAmount());
        assertTrue(pokedex.mouseScrolled(270, 158, 0, -1));
        assertEquals(7.0, list.scrollAmount());
        assertEquals(3.0, description.scrollAmount());
        assertFalse(pokedex.mouseScrolled(194.5, 185.5, 0, -1));
        assertEquals(3.0, description.scrollAmount());
    }
}
