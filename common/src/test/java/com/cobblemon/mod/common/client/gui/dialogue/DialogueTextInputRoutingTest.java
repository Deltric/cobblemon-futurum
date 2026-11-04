/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.dialogue;

import com.cobblemon.mod.common.client.gui.dialogue.widgets.DialogueTextInputWidget;
import com.cobblemon.mod.common.net.messages.client.dialogue.dto.DialogueDTO;
import com.cobblemon.mod.common.net.messages.client.dialogue.dto.DialogueGibberDTO;
import com.cobblemon.mod.common.net.messages.client.dialogue.dto.DialogueInputDTO;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class DialogueTextInputRoutingTest {
    private static <T> T allocate(Class<T> type) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return type.cast(((Unsafe) unsafeField.get(null)).allocateInstance(type));
    }

    private static DialogueTextInputWidget input(DialogueInputDTO.InputType mode) throws Exception {
        var dto = new DialogueDTO();
        var input = new DialogueInputDTO();
        input.setInputType(mode);
        dto.setDialogueInput(input);
        var screen = allocate(DialogueScreen.class);
        screen.setDialogueDTO(dto);
        // Skip live client/font setup; hit-testing uses these production bounds and DTO state.
        var widget = allocate(DialogueTextInputWidget.class);
        Field owner = DialogueTextInputWidget.class.getDeclaredField("dialogueScreen");
        owner.setAccessible(true);
        owner.set(widget, screen);
        widget.setRectangle(196, 21, 100, 200);
        widget.visible = true;
        widget.active = true;
        return widget;
    }

    private static AbstractContainerEventHandler host(GuiEventListener... children) {
        return new AbstractContainerEventHandler() {
            @Override public List<? extends GuiEventListener> children() { return List.of(children); }
        };
    }

    @Test void undisplayedTextInputDoesNotMaskDialogueChoices() throws Exception {
        var text = input(DialogueInputDTO.InputType.OPTION);
        int[] chosen = {0};
        GuiEventListener choice = new GuiEventListener() {
            @Override public boolean isMouseOver(double x, double y) { return x >= 100 && x < 196 && y >= 200 && y < 221; }
            @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { chosen[0]++; return true; }
            @Override public void setFocused(boolean focused) {}
            @Override public boolean isFocused() { return false; }
        };
        var host = host(text, choice); // Same ordering as DialogueScreen.init().
        assertSame(choice, host.getChildAt(140, 210).orElseThrow());
        assertTrue(host.mouseClicked(new MouseButtonEvent(140, 210, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false));
        assertEquals(1, chosen[0]);
        for (var mode : new DialogueInputDTO.InputType[] {DialogueInputDTO.InputType.NONE, DialogueInputDTO.InputType.AUTO_CONTINUE}) {
            text.getDialogueScreen().getDialogueDTO().getDialogueInput().setInputType(mode);
            assertSame(choice, host.getChildAt(140, 210).orElseThrow());
        }
    }

    @Test void textModeHitRegionTracksGibberWaitVisibilityAndBounds() throws Exception {
        var text = input(DialogueInputDTO.InputType.TEXT);
        var screen = text.getDialogueScreen();
        var host = host(text);
        assertSame(text, host.getChildAt(140, 210).orElseThrow());
        assertTrue(host.getChildAt(99.9, 210).isEmpty());
        assertTrue(host.getChildAt(296, 210).isEmpty());
        screen.setWaitingForServerUpdate(true);
        assertTrue(host.getChildAt(140, 210).isEmpty());
        screen.setWaitingForServerUpdate(false);
        screen.setGibber(new DialogueGibberDTO(true, true, 1, 1, 1, 1, 1, 1, List.of()));
        assertTrue(host.getChildAt(140, 210).isEmpty());
        screen.setGibberDone(true);
        assertSame(text, host.getChildAt(140, 210).orElseThrow());
        text.visible = false;
        assertTrue(host.getChildAt(140, 210).isEmpty());
        text.visible = true;
        text.active = false;
        assertTrue(host.getChildAt(140, 210).isEmpty());
    }
}
