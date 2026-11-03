/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.cobblemon.mod.common.client.gui.battle;

import com.cobblemon.mod.common.api.gui.ParentWidget;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import com.cobblemon.mod.common.client.gui.battle.subscreen.*;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile;
import com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleResponseButton;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class BattleActionSelectionInputTest {
    // Skip constructors that obtain the live Minecraft window and battle. Every
    // hit test, native child dispatch, and tile callback below is production code.
    private static <T> T allocate(Class<T> type) throws Exception {
        return type.cast(((Unsafe) field(Unsafe.class, "theUnsafe").get(null)).allocateInstance(type));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        var result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        field(owner, name).set(target, value);
    }

    private static <T extends BattleActionSelection> T selection(Class<T> type, int x, int y, int width, int height) throws Exception {
        T result = allocate(type);
        result.setRectangle(width, height, x, y);
        result.active = true;
        result.visible = true;
        set(result, ParentWidget.class, "children", new ArrayList<GuiEventListener>());
        return result;
    }

    private static final class Host extends AbstractContainerEventHandler {
        private final List<? extends GuiEventListener> children;
        Host(GuiEventListener... children) { this.children = List.of(children); }
        @Override public List<? extends GuiEventListener> children() { return children; }
    }

    private static MouseButtonEvent click(double x, double y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }

    private static BattleGeneralActionSelection general(List<Integer> clicks, int count) throws Exception {
        var result = selection(BattleGeneralActionSelection.class, 12, 215, 99, 35);
        var tiles = new ArrayList<BattleOptionTile>();
        for (int index = 0; index < count; index++) {
            final int action = index;
            var tile = allocate(BattleOptionTile.class);
            set(tile, BattleOptionTile.class, "x", 12 + (index % 2) * 93);
            set(tile, BattleOptionTile.class, "y", 215 + (index / 2) * 29);
            set(tile, BattleOptionTile.class, "onClick", (Function0<Unit>) () -> {
                clicks.add(action);
                return Unit.INSTANCE;
            });
            tiles.add(tile);
        }
        set(result, BattleGeneralActionSelection.class, "tiles", tiles);
        set(result, BattleGeneralActionSelection.class, "backButton", new BattleBackButton(9, 278));
        return result;
    }

    @Test void nativeDispatchReachesFightSwitchCatchAndRunTiles() throws Exception {
        var clicks = new ArrayList<Integer>();
        var panel = general(clicks, 4);
        var host = new Host(panel);
        assertAll(
            () -> assertTrue(host.mouseClicked(click(57, 228, InputConstants.MOUSE_BUTTON_LEFT), false), "Fight"),
            () -> assertTrue(host.mouseClicked(click(150, 228, InputConstants.MOUSE_BUTTON_LEFT), false), "Switch"),
            () -> assertTrue(host.mouseClicked(click(57, 257, InputConstants.MOUSE_BUTTON_LEFT), false), "Catch"),
            () -> assertTrue(host.mouseClicked(click(150, 257, InputConstants.MOUSE_BUTTON_LEFT), false), "Run")
        );
        assertEquals(List.of(0, 1, 2, 3), clicks);
    }

    @Test void gapsInactivePanelsAndNonPrimaryClicksDoNotSelectActions() throws Exception {
        var clicks = new ArrayList<Integer>();
        var panel = general(clicks, 4);
        var host = new Host(panel);
        assertTrue(host.getChildAt(103, 228).isEmpty(), "Column gap must not claim the screen click");
        assertTrue(host.getChildAt(57, 242).isEmpty(), "Row gap must not claim the screen click");
        assertFalse(host.mouseClicked(click(230, 228, InputConstants.MOUSE_BUTTON_LEFT), false));
        host.mouseClicked(click(150, 228, InputConstants.MOUSE_BUTTON_RIGHT), false);
        assertTrue(clicks.isEmpty());
        panel.active = false;
        assertTrue(host.getChildAt(57, 228).isEmpty());
        panel.active = true;
        panel.visible = false;
        assertTrue(host.getChildAt(57, 228).isEmpty());
    }

    @Test void changingAvailableActionsDoesNotLeavePhantomTilesAndRetainsClickableEdges() throws Exception {
        var clicks = new ArrayList<Integer>();
        var panel = general(clicks, 3);
        var host = new Host(panel);
        assertTrue(host.getChildAt(150, 257).isEmpty());
        assertTrue(host.mouseClicked(click(102, 241, InputConstants.MOUSE_BUTTON_LEFT), false));
        assertEquals(List.of(0), clicks, "The inclusive tile edge already accepted by its handler is retained");
    }

    @Test void previousRequestBackButtonParticipatesOnlyWhenPresent() throws Exception {
        var panel = general(new ArrayList<>(), 4);
        var host = new Host(panel);
        assertTrue(host.getChildAt(15, 285).isEmpty());
        set(panel, BattleGeneralActionSelection.class, "lastAnwseredRequest", allocate(SingleActionRequest.class));
        assertSame(panel, host.getChildAt(15, 285).orElseThrow());
    }

    @Test void bothMoveColumnsAndBackGimmickAndShiftControlsReachTheirPanel() throws Exception {
        var panel = selection(BattleMoveSelection.class, 20, 216, 100, 100);
        var tiles = new ArrayList<BattleMoveSelection.MoveTile>();
        for (int i = 0; i < 4; i++) {
            var tile = allocate(BattleMoveSelection.MoveTile.class);
            set(tile, BattleMoveSelection.MoveTile.class, "x", 20F + (i % 2) * 105F);
            set(tile, BattleMoveSelection.MoveTile.class, "y", 216F + (i / 2) * 29F);
            tiles.add(tile);
        }
        set(panel, BattleMoveSelection.class, "moveTiles", tiles);
        set(panel, BattleMoveSelection.class, "backButton", new BattleBackButton(9, 278));
        var gimmick = allocate(DynamaxButton.class);
        set(gimmick, BattleGimmickButton.class, "x", 100F);
        set(gimmick, BattleGimmickButton.class, "y", 278F);
        set(panel, BattleMoveSelection.class, "gimmickButtons", List.of(gimmick));
        set(panel, BattleMoveSelection.class, "shiftButton", new BattleShiftButton(42.5F, 278));
        var host = new Host(panel);
        for (var point : new double[][] {{66, 228}, {171, 228}, {66, 257}, {171, 257}, {15, 285}, {109, 285}, {60, 285}}) {
            assertSame(panel, host.getChildAt(point[0], point[1]).orElseThrow());
        }
        assertTrue(host.getChildAt(120, 228).isEmpty());
    }

    @Test void centeredTargetsAndTheirBackButtonReachTheirPanel() throws Exception {
        var panel = selection(BattleTargetSelection.class, 0, 74, 100, 100);
        var tile = allocate(BattleTargetSelection.TargetTile.class);
        set(tile, BattleTargetSelection.TargetTile.class, "x", 200F);
        set(tile, BattleTargetSelection.TargetTile.class, "y", 120F);
        set(panel, BattleTargetSelection.class, "targetTiles", List.of(tile));
        set(panel, BattleTargetSelection.class, "backButton", new BattleBackButton(9, 278));
        var host = new Host(panel);
        assertSame(panel, host.getChildAt(245, 135).orElseThrow());
        assertSame(panel, host.getChildAt(15, 285).orElseThrow());
        assertTrue(host.getChildAt(10, 100).isEmpty());
    }

    @Test void centeredForfeitConfirmationButtonsReachTheirPanel() throws Exception {
        var panel = selection(ForfeitConfirmationSelection.class, 0, 0, 113, 45);
        for (var name : List.of("acceptButton", "declineButton")) {
            var button = allocate(BattleResponseButton.class);
            button.setRectangle(34, 19, name.equals("acceptButton") ? 166 : 201, 145);
            set(panel, ForfeitConfirmationSelection.class, name, button);
        }
        var host = new Host(panel);
        assertSame(panel, host.getChildAt(180, 155).orElseThrow());
        assertSame(panel, host.getChildAt(215, 155).orElseThrow());
        assertTrue(host.getChildAt(10, 10).isEmpty());
    }

    @Test void switchPanelRoutesSlotsAndBackWithoutClaimingEmptyScreenAreas() throws Exception {
        var panel = selection(BattleSwitchPokemonSelection.class, 0, 74, 400, 300);
        var tile = allocate(BattleSwitchPokemonSelection.SwitchTile.class);
        set(tile, BattleSwitchPokemonSelection.SwitchTile.class, "x", 200F);
        set(tile, BattleSwitchPokemonSelection.SwitchTile.class, "y", 120F);
        set(panel, BattleSwitchPokemonSelection.class, "tiles", List.of(tile));
        set(panel, BattleSwitchPokemonSelection.class, "backButton", new BattleBackButton(9, 278));
        var host = new Host(panel);
        assertSame(panel, host.getChildAt(245, 135).orElseThrow());
        assertSame(panel, host.getChildAt(15, 285).orElseThrow());
        assertTrue(host.getChildAt(10, 100).isEmpty());
    }
}
