/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokeball;

import com.bedrockk.molang.runtime.struct.QueryStruct;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.entity.EntitySideDelegate;
import com.cobblemon.mod.common.api.reactive.SimpleObservable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;
import net.minecraft.SharedConstants;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class EmptyPokeBallEntityTrackedDataTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var lookup = net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private static <T> EntityDataAccessor<T> accessor(Class<?> owner, String name) throws Exception {
        return (EntityDataAccessor<T>) field(owner, name).get(null);
    }

    private record Fixture(EmptyPokeBallEntity ball, EntityDataAccessor<ItemStack> item,
                           EntityDataAccessor<Boolean> gravity) {}

    private static Fixture beforeSubclassInitialization() throws Exception {
        // Reproduce JVM zero-initialized subclass fields during the native super
        // constructor callback, without creating a fake world or running a game.
        var unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        var ball = (EmptyPokeBallEntity) unsafe.allocateInstance(EmptyPokeBallEntity.class);
        var item = EmptyPokeBallEntityTrackedDataTest.<ItemStack>accessor(ThrowableItemProjectile.class, "DATA_ITEM_STACK");
        var gravity = EmptyPokeBallEntityTrackedDataTest.<Boolean>accessor(Entity.class, "DATA_NO_GRAVITY");
        var capture = EmptyPokeBallEntity.Companion.getCAPTURE_STATE();
        var values = new SynchedEntityData.DataItem<?>[Math.max(item.id(), Math.max(gravity.id(), capture.id())) + 1];
        values[item.id()] = new SynchedEntityData.DataItem<>(item, new ItemStack(Items.SNOWBALL));
        values[gravity.id()] = new SynchedEntityData.DataItem<>(gravity, false);
        values[capture.id()] = new SynchedEntityData.DataItem<>(capture, (byte) EmptyPokeBallEntity.CaptureState.NOT.ordinal());
        var constructor = SynchedEntityData.class.getDeclaredConstructor(SyncedDataHolder.class, SynchedEntityData.DataItem[].class);
        constructor.setAccessible(true);
        field(Entity.class, "entityData").set(ball, constructor.newInstance(ball, values));
        return new Fixture(ball, item, gravity);
    }

    @Test
    void nativeConstructorItemUpdateBeforeObservableInitializationRetainsItem() throws Exception {
        var fixture = beforeSubclassInitialization();
        assertNull(fixture.ball.getDataTrackerEmitter());
        assertNull(fixture.ball.getDelegate());
        var source = new ItemStack(Items.ENDER_PEARL, 5);
        assertDoesNotThrow(() -> fixture.ball.setItem(source));
        assertTrue(fixture.ball.getItem().is(Items.ENDER_PEARL));
        assertEquals(1, fixture.ball.getItem().getCount());
        assertEquals(5, source.getCount());
        assertNotSame(source, fixture.ball.getItem());
    }

    @Test
    void initializedObservableAndDelegateReceiveNormalNativeUpdates() throws Exception {
        var fixture = beforeSubclassInitialization();
        fixture.ball.setItem(new ItemStack(Items.ENDER_PEARL));
        var observed = new ArrayList<EntityDataAccessor<?>>();
        var delegated = new ArrayList<EntityDataAccessor<?>>();
        initializeCallbacks(fixture.ball, observed, delegated);
        fixture.ball.setItem(new ItemStack(Items.EGG, 4));
        assertEquals(List.of(fixture.item), observed);
        assertEquals(List.of(fixture.item), delegated);
        assertTrue(fixture.ball.getItem().is(Items.EGG));
        assertEquals(1, fixture.ball.getItem().getCount());
    }

    @Test
    void captureStateStillUpdatesGravityAndEmitsEveryChangedAccessor() throws Exception {
        var fixture = beforeSubclassInitialization();
        var observed = new ArrayList<EntityDataAccessor<?>>();
        var delegated = new ArrayList<EntityDataAccessor<?>>();
        initializeCallbacks(fixture.ball, observed, delegated);
        fixture.ball.setCaptureState(EmptyPokeBallEntity.CaptureState.SHAKE);
        assertTrue(fixture.ball.isNoGravity());
        fixture.ball.setCaptureState(EmptyPokeBallEntity.CaptureState.FALL);
        assertFalse(fixture.ball.isNoGravity());
        var capture = EmptyPokeBallEntity.Companion.getCAPTURE_STATE();
        assertEquals(List.of(fixture.gravity, capture, fixture.gravity, capture), observed);
        assertEquals(List.of(capture, fixture.gravity, capture, fixture.gravity), delegated);
    }

    private static void initializeCallbacks(EmptyPokeBallEntity ball, List<EntityDataAccessor<?>> observed,
                                            List<EntityDataAccessor<?>> delegated) throws Exception {
        var emitter = new SimpleObservable<EntityDataAccessor<?>>();
        emitter.subscribe(Priority.NORMAL, data -> { observed.add(data); return Unit.INSTANCE; });
        field(EmptyPokeBallEntity.class, "dataTrackerEmitter").set(ball, emitter);
        field(EmptyPokeBallEntity.class, "delegate").set(ball, new EntitySideDelegate<EmptyPokeBallEntity>() {
            public void initialize(EmptyPokeBallEntity entity) {}
            public void tick(EmptyPokeBallEntity entity) {}
            public void onSyncedDataUpdated(EntityDataAccessor<?> data) { delegated.add(data); }
            public void addToStruct(QueryStruct struct) {}
        });
    }
}
