/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.BeforeAll;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelGeometryTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private record Call(String name, Object[] args) {}

    private List<Call> replay(ModelGeometry geometry, PoseStack matrix) {
        List<Call> calls = new ArrayList<>();
        VertexConsumer output = (VertexConsumer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {VertexConsumer.class},
            (proxy, method, args) -> {
                if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                calls.add(new Call(method.getName(), args));
                return proxy;
            });
        SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
                assertEquals("submitCustomGeometry", method.getName());
                ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), output);
                return null;
            });
        geometry.submit(matrix, collector);
        return calls;
    }

    @Test void snapshotDoesNotObserveSubsequentAnimationVerticesOrTintChanges() {
        var builder = new ModelGeometry.Builder();
        var consumer = builder.getBuffer(RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png")));
        consumer.addVertex(1, 2, 3).setColor(0x7f123456).setUv(0.25F, 0.75F).setOverlay(0x23450067)
            .setLight(0x00ef0034).setNormal(0, 1, 0).setUv3(0.4F, 0.6F).setLineWidth(2);
        var snapshot = builder.build();
        consumer.setColor(-1);
        consumer.addVertex(90, 100, 110);
        var calls = replay(snapshot, new PoseStack());
        assertEquals(1, calls.stream().filter(c -> c.name.equals("addVertex")).count());
        assertArrayEquals(new Object[] {0x7f123456}, calls.stream().filter(c -> c.name.equals("setColor")).findFirst().orElseThrow().args);
        assertArrayEquals(new Object[] {0x67, 0x2345}, calls.stream().filter(c -> c.name.equals("setUv1")).findFirst().orElseThrow().args);
        assertArrayEquals(new Object[] {0x34, 0xef}, calls.stream().filter(c -> c.name.equals("setUv2")).findFirst().orElseThrow().args);
        assertEquals(1, snapshot.minX());
        assertEquals(2, snapshot.maxY());
        assertEquals(100, builder.build().maxY());
    }

    @Test void replayAppliesSubmissionTransformToPositionsAndInverseTransposeToNormals() {
        var builder = new ModelGeometry.Builder();
        builder.getBuffer(RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png")))
            .addVertex(1, 2, 3).setNormal(1, 1, 0);
        var pose = new PoseStack();
        pose.translate(10, 20, 30);
        pose.scale(2, 1, 1);
        var calls = replay(builder.build(), pose);
        assertArrayEquals(new Object[] {12F, 22F, 33F}, calls.stream().filter(c -> c.name.equals("addVertex")).findFirst().orElseThrow().args);
        Object[] normal = calls.stream().filter(c -> c.name.equals("setNormal")).findFirst().orElseThrow().args;
        assertEquals(1 / Math.sqrt(5), (float) normal[0], 0.00001);
        assertEquals(2 / Math.sqrt(5), (float) normal[1], 0.00001);
    }

    @Test void emptyAndNonFiniteVerticesAreRejectedBeforePreviewExtraction() {
        var builder = new ModelGeometry.Builder();
        assertTrue(builder.build().isEmpty());
        var buffer = builder.getBuffer(RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png")));
        assertThrows(IllegalStateException.class, () -> buffer.setColor(-1));
        assertThrows(IllegalArgumentException.class, () -> buffer.addVertex(Float.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> buffer.addVertex(0, Float.POSITIVE_INFINITY, 0));
    }

    @Test void glowingGeometryAlsoSubmitsToMinecraftsOutlinePhase() {
        var builder = new ModelGeometry.Builder();
        builder.getBuffer(RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png")))
            .addVertex(1, 2, 3).setColor(0xff123456);
        List<Boolean> outlinePasses = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        VertexConsumer output = (VertexConsumer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {VertexConsumer.class},
            (proxy, method, args) -> {
                if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                if (method.getName().equals("setColor")) colors.add((Integer) args[0]);
                return proxy;
            });
        SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
                outlinePasses.add(((net.minecraft.client.renderer.rendertype.RenderType) args[1]).isOutline());
                ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), output);
                return null;
            });
        builder.build().submit(new PoseStack(), collector, 0xffaabbcc);
        assertEquals(List.of(false, true), outlinePasses);
        assertEquals(List.of(0xff123456, 0xffaabbcc), colors);
    }

    @Test void layerBatchesDrawAfterTheBaseModelInsteadOfSharingItsHashOrderedPhase() {
        var base = RenderTypes.entityCutout(Identifier.withDefaultNamespace("base.png"));
        var layer = RenderTypes.entityCutoutCull(Identifier.withDefaultNamespace("alpha_eyes.png"));
        var builder = new ModelGeometry.Builder();
        for (var type : List.of(base, layer)) {
            builder.getBuffer(type).addVertex(1, 2, 3).setColor(-1).setNormal(0, 1, 0)
                .setUv(0, 0).setLight(0).setOverlay(0);
        }
        var storage = new SubmitNodeStorage();
        builder.build().submit(new PoseStack(), storage);
        assertSame(base, customType(drain(storage.order(0).solid)));
        assertSame(layer, customType(drain(storage.order(1).solid)));
    }

    @Test void invisibleGlowingGeometryUsesOnlyTheNativeOutlinePhaseAndCurrentTeamColor() {
        var builder = new ModelGeometry.Builder();
        var outline = RenderTypes.outline(Identifier.withDefaultNamespace("test.png"));
        assertTrue(outline.isOutline());
        assertTrue(outline.outline().isEmpty(), "outline-only materials have no secondary outline material");
        builder.getBuffer(outline).addVertex(1, 2, 3).setColor(0xff123456)
            .setLight(0x00100020).setOverlay(0x00300040);
        var geometry = builder.build();
        for (int teamColor : new int[] {0xffaabbcc, 0xff1234ab, 0}) {
            var storage = new SubmitNodeStorage();
            var pose = new PoseStack();
            pose.translate(4, 5, 6);
            geometry.submitWithLight(pose, storage, 0x00500060, 0x00700080, teamColor);
            pose.translate(100, 100, 100);
            assertTrue(drain(storage.order(0).solid).isEmpty());
            assertTrue(drain(storage.order(0).translucentCustomGeometry).isEmpty());
            var nodes = drain(storage.order(0).outline);
            assertEquals(teamColor == 0 ? 0 : 1, nodes.size());
            if (teamColor == 0) continue;
            var submit = (net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit) nodes.getFirst();
            assertSame(outline, submit.renderType());
            List<Call> calls = new ArrayList<>();
            submit.customGeometryRenderer().render(submit.pose(), recordingVertices(calls));
            assertArrayEquals(new Object[] {5F, 7F, 9F}, named(calls, "addVertex").getFirst().args);
            assertArrayEquals(new Object[] {teamColor}, named(calls, "setColor").getFirst().args);
            assertArrayEquals(new Object[] {240, 240}, named(calls, "setUv2").getFirst().args);
            assertArrayEquals(new Object[] {0, 10}, named(calls, "setUv1").getFirst().args);
        }
    }

    @Test void nativeItemExtentsShareThePreviewBoundsAndCopyTheLocatorTransform() {
        var builder = new ModelGeometry.Builder();
        var item = new net.minecraft.client.renderer.item.ItemStackRenderState();
        item.newLayer().setExtents(() -> new org.joml.Vector3fc[] {
            new org.joml.Vector3f(-1, -2, 0), new org.joml.Vector3f(1, 2, 0)
        });
        var pose = new PoseStack();
        pose.translate(5, 10, 0);
        builder.addItem(item, pose, 15728880, 0);
        pose.translate(100, 100, 0);
        var snapshot = builder.build();
        assertFalse(snapshot.isEmpty());
        // Native item transforms include their standard half-block centering.
        assertEquals(3.5F, snapshot.minX());
        assertEquals(5.5F, snapshot.maxX());
        assertEquals(7.5F, snapshot.minY());
        assertEquals(11.5F, snapshot.maxY());
    }

    @Test void labelsFreezeStyledTextAndPoseBeforeDeferredSubmission() {
        var builder = new ModelGeometry.Builder();
        var label = net.minecraft.network.chat.Component.literal("Pikachu").withStyle(net.minecraft.ChatFormatting.YELLOW);
        var pose = new PoseStack();
        pose.translate(3, 4, 5);
        builder.addText(label, pose, -12, 0, -1, 0, 15728880, net.minecraft.client.gui.Font.DisplayMode.NORMAL);
        var geometry = builder.build();
        label.append(" changed");
        pose.translate(100, 100, 100);
        List<String> labels = new ArrayList<>();
        SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
                assertEquals("submitText", method.getName());
                assertEquals(3f, ((PoseStack) args[0]).last().pose().m30());
                StringBuilder value = new StringBuilder();
                ((net.minecraft.util.FormattedCharSequence) args[3]).accept((index, style, codepoint) -> {
                    assertEquals(0xFFFF55, style.getColor().getValue());
                    value.appendCodePoint(codepoint);
                    return true;
                });
                labels.add(value.toString());
                return null;
            });
        geometry.submit(new PoseStack(), collector);
        assertEquals(List.of("Pikachu"), labels);
    }

    private static VertexConsumer recordingVertices(List<Call> calls) {
        return (VertexConsumer) Proxy.newProxyInstance(ModelGeometryTest.class.getClassLoader(), new Class<?>[] {VertexConsumer.class},
            (proxy, method, args) -> {
                if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                calls.add(new Call(method.getName(), args.clone()));
                return proxy;
            });
    }

    private static List<Call> capture(BiConsumer<PoseStack, SubmitNodeCollector> submit) {
        List<Call> calls = new ArrayList<>();
        VertexConsumer vertices = recordingVertices(calls);
        SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(ModelGeometryTest.class.getClassLoader(),
            new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
                if (method.getName().equals("submitCustomGeometry")) {
                    ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), vertices);
                } else {
                    Object[] copied = args.clone();
                    if (copied[0] instanceof PoseStack pose) copied[0] = pose.last().copy();
                    calls.add(new Call(method.getName(), copied));
                }
                return null;
            });
        submit.accept(new PoseStack(), collector);
        return calls;
    }

    private static List<SubmitNode> drain(FeatureRenderPhase<?> phase) {
        List<SubmitNode> nodes = new ArrayList<>();
        phase.sortInto((node, ordered) -> nodes.add(node));
        return nodes;
    }

    private static RenderType customType(List<SubmitNode> nodes) {
        var custom = nodes.stream().filter(net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit.class::isInstance).toList();
        assertEquals(1, custom.size());
        return ((net.minecraft.client.renderer.feature.CustomFeatureRenderer.Submit) custom.getFirst()).renderType();
    }

    private static List<ModelFeatureRenderer.Submit<?>> models(List<SubmitNode> nodes) {
        return nodes.stream().filter(ModelFeatureRenderer.Submit.class::isInstance)
            .<ModelFeatureRenderer.Submit<?>>map(node -> (ModelFeatureRenderer.Submit<?>) node).toList();
    }

    private static List<Call> render(ModelFeatureRenderer.Submit<?> submit, boolean projectCracks) {
        List<Call> calls = new ArrayList<>();
        VertexConsumer output = recordingVertices(calls);
        if (projectCracks) output = new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(output, submit.sheetedDecalPose(), 1);
        PoseStack pose = new PoseStack();
        pose.last().set(submit.pose());
        submit.model().renderToBuffer(pose, output, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
        return calls;
    }

    private static List<Call> named(List<Call> calls, String name) {
        return calls.stream().filter(call -> call.name.equals(name)).toList();
    }

    /** Only the client option queried by native phase selection is initialized; no window or GPU is created. */
    private static final class NativeClientOptions implements AutoCloseable {
        private final Field instance;
        private final Object previous;

        NativeClientOptions(boolean improvedTransparency) throws Exception {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe allocator = (sun.misc.Unsafe) unsafeField.get(null);
            Minecraft client = (Minecraft) allocator.allocateInstance(Minecraft.class);
            GameRenderer renderer = (GameRenderer) allocator.allocateInstance(GameRenderer.class);
            GameRenderState state = new GameRenderState();
            state.optionsRenderState.improvedTransparency = improvedTransparency;
            Field stateField = GameRenderer.class.getDeclaredField("gameRenderState");
            stateField.setAccessible(true);
            stateField.set(renderer, state);
            Field rendererField = Minecraft.class.getDeclaredField("gameRenderer");
            rendererField.setAccessible(true);
            rendererField.set(client, renderer);
            instance = Minecraft.class.getDeclaredField("instance");
            instance.setAccessible(true);
            previous = instance.get(null);
            instance.set(null, client);
        }

        @Override public void close() throws Exception { instance.set(null, previous); }
    }

    @Test void breakingBatchesUseNativeOpaqueTranslucentAndOitPhasesWithFrozenProjection() throws Exception {
        var id = Identifier.withDefaultNamespace("test.png");
        var opaque = RenderTypes.entityCutout(id);
        var translucent = RenderTypes.entityTranslucent(id);
        var nonBreaking = RenderTypes.text(id);
        assertTrue(opaque.affectsCrumbling());
        assertTrue(translucent.affectsCrumbling());
        assertFalse(nonBreaking.affectsCrumbling());
        for (boolean improved : new boolean[] {false, true}) {
            try (var ignored = new NativeClientOptions(improved)) {
                var builder = new ModelGeometry.Builder();
                for (var type : List.of(opaque, translucent, nonBreaking)) {
                    builder.getBuffer(type).addVertex(1, 2, 3).setColor(0xff123456).setNormal(0, 1, 0)
                        .setUv(0.25F, 0.75F).setLight(0x00340056).setOverlay(0x00120078);
                }
                var pose = new PoseStack();
                pose.translate(4, 5, 6);
                var camera = new PoseStack();
                camera.translate(20, 30, 40);
                var storage = new SubmitNodeStorage();
                storage.setUseImprovedTransparency(improved);
                builder.build().submitWithCrumbling(pose, storage, new ModelFeatureRenderer.CrumblingOverlay(7, camera.last()));
                camera.translate(100, 100, 100);
                pose.translate(100, 100, 100);
                // Batches take orders 0..2 in recording order; each batch's cracks follow every normal order.
                var normalSolid = drain(storage.order(0).solid);
                var opaqueCracks = models(drain(storage.order(3).solid));
                var translucentNodes = drain(storage.order(4).breakingOverlay);
                var translucentCracks = models(translucentNodes);
                assertEquals(1, opaqueCracks.size());
                assertEquals(1, translucentCracks.size());
                assertSame(ModelBakery.DESTROY_TYPES.get(7), opaqueCracks.getFirst().renderType());
                assertSame((improved ? ModelBakery.DESTROY_TYPES_OIT : ModelBakery.DESTROY_TYPES).get(7), translucentCracks.getFirst().renderType());
                for (var crack : List.of(opaqueCracks.getFirst(), translucentCracks.getFirst())) {
                    assertEquals(20F, crack.sheetedDecalPose().pose().m30());
                    var calls = render(crack, true);
                    assertArrayEquals(new Object[] {5F, 7F, 9F}, named(calls, "addVertex").getFirst().args);
                    assertArrayEquals(new Object[] {0x56, 0x34}, named(calls, "setUv2").getFirst().args);
                    assertArrayEquals(new Object[] {0x78, 0x12}, named(calls, "setUv1").getFirst().args);
                    assertFalse(named(calls, "setUv").stream().anyMatch(call -> java.util.Arrays.equals(call.args, new Object[] {0.25F, 0.75F})));
                }
                // Normal geometry still occupies the engine's original custom-geometry phases, one order per batch.
                assertSame(opaque, customType(normalSolid));
                assertTrue(drain(storage.order(0).translucentCustomGeometry).isEmpty());
                assertSame(translucent, customType(drain(storage.order(1).translucentCustomGeometry)));
                assertSame(nonBreaking, customType(drain(storage.order(2).translucentCustomGeometry)));
            }
        }
    }

    private static BakedQuad quad(RenderType type, float x) {
        var material = new BakedQuad.MaterialInfo(null, type.hasBlending() ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.SOLID,
            type, type, type, -1, null, 0);
        return new BakedQuad(new Vector3f(x, 0, 0), new Vector3f(x + 1, 0, 0), new Vector3f(x + 1, 1, 0), new Vector3f(x, 1, 0),
            0, 0, 0, 0, Direction.NORTH, material);
    }

    private static BlockStateModelPart part(BakedQuad directional, BakedQuad unculled) {
        return new BlockStateModelPart() {
            @Override public List<BakedQuad> getQuads(Direction direction) {
                return direction == Direction.NORTH ? List.of(directional) : direction == null ? List.of(unculled) : List.of();
            }
            @Override public boolean useAmbientOcclusion() { return false; }
            @Override public Material.Baked particleMaterial() { return null; }
            @Override public int materialFlags() { return 0; }
        };
    }

    @Test void nativeItemAndBlockChildrenKeepTheirTransformsLightsAndEveryEligibleQuadInCracks() throws Exception {
        try (var ignored = new NativeClientOptions(false)) {
            var id = Identifier.withDefaultNamespace("test.png");
            var opaque = RenderTypes.entityCutout(id);
            var translucent = RenderTypes.entityTranslucent(id);
            var builder = new ModelGeometry.Builder();
            var item = new ItemStackRenderState();
            item.newLayer().setQuads(ItemQuads.split(List.of(quad(opaque, 0), quad(translucent, 2), quad(RenderTypes.text(id), 4))));
            var itemPose = new PoseStack();
            itemPose.translate(10, 20, 30);
            builder.addItem(item, itemPose, 0x00a000b0, 0x00200030);
            var block = new BlockModelRenderState();
            block.setupModel(new Matrix4f().translation(2, 3, 4), false).add(part(quad(opaque, 0), quad(opaque, 2)));
            block.blockLightCoords = 0x00400050;
            var blockPose = new PoseStack();
            blockPose.translate(40, 50, 60);
            builder.addBlock(block, blockPose, 0x00b000a0, 0x00300020);
            var submissionPose = new PoseStack();
            submissionPose.translate(3, 4, 5);
            var storage = new SubmitNodeStorage();
            builder.build().submitWithCrumbling(submissionPose, storage, new ModelFeatureRenderer.CrumblingOverlay(2, new PoseStack().last()));
            var solid = models(drain(storage.order(1).solid));
            var translucentCracks = models(drain(storage.order(1).breakingOverlay));
            assertEquals(2, solid.size());
            assertEquals(1, translucentCracks.size());
            var itemCrack = solid.stream().filter(it -> it.pose().pose().m30() < 20).findFirst().orElseThrow();
            var blockCrack = solid.stream().filter(it -> it.pose().pose().m30() > 20).findFirst().orElseThrow();
            var itemCalls = render(itemCrack, false);
            assertEquals(4, named(itemCalls, "addVertex").size());
            assertArrayEquals(new Object[] {12.5F, 23.5F, 34.5F}, named(itemCalls, "addVertex").getFirst().args);
            assertArrayEquals(new Object[] {0xb0, 0xa0}, named(itemCalls, "setUv2").getFirst().args);
            assertArrayEquals(new Object[] {0x30, 0x20}, named(itemCalls, "setUv1").getFirst().args);
            var blockCalls = render(blockCrack, false);
            assertEquals(8, named(blockCalls, "addVertex").size(), "directional and unculled quads both survive");
            assertArrayEquals(new Object[] {45F, 57F, 69F}, named(blockCalls, "addVertex").getFirst().args);
            assertArrayEquals(new Object[] {0xa0, 0xb0}, named(blockCalls, "setUv2").getFirst().args);
            assertArrayEquals(new Object[] {0x20, 0x30}, named(blockCalls, "setUv1").getFirst().args);
            assertEquals(4, named(render(translucentCracks.getFirst(), false), "addVertex").size());
        }
    }

    @Test void nativeSpecialModelsKeepExplicitOrderAndNativeOverlayPoseAndLighting() throws Exception {
        try (var ignored = new NativeClientOptions(false)) {
            var type = RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png"));
            var model = new Model.Simple(new ModelPart(List.of(), Map.of()), texture -> type);
            var item = new ItemStackRenderState();
            item.newLayer().setupSpecialModel(new SpecialModelRenderer<Unit>() {
                @Override public void submit(Unit argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
                    collector.order(6).submitModel(model, Unit.INSTANCE, pose, type, light, overlay, 0xffaabbcc, null, outline);
                }
                @Override public void getExtents(Consumer<Vector3fc> output) { output.accept(new Vector3f()); }
                @Override public Unit extractArgument(ItemStack stack) { return Unit.INSTANCE; }
            }, Unit.INSTANCE);
            var builder = new ModelGeometry.Builder();
            var capturePose = new PoseStack();
            capturePose.translate(8, 9, 10);
            builder.addItem(item, capturePose, 0x00700080, 0x009000a0);
            var storage = new SubmitNodeStorage();
            builder.build().submitWithCrumbling(new PoseStack(), storage, new ModelFeatureRenderer.CrumblingOverlay(3, new PoseStack().last()));
            assertEquals(Set.of(6, 7), storage.getSubmitsPerOrder().keySet());
            var normal = models(drain(storage.order(6).solid)).getFirst();
            var crack = models(drain(storage.order(7).solid)).getFirst();
            assertNull(normal.sheetedDecalPose());
            assertNotNull(crack.sheetedDecalPose());
            assertSame(model, crack.model());
            assertEquals(normal.pose().pose(), crack.pose().pose());
            assertEquals(7.5F, crack.pose().pose().m30());
            assertEquals(0x00700080, crack.lightCoords());
            assertEquals(0x009000a0, crack.overlayCoords());
            assertEquals(0xffaabbcc, crack.tintedColor());
            assertSame(ModelBakery.DESTROY_TYPES.get(3), crack.renderType());
        }
    }

    @Test void crackPassesDrainAfterAllNormalMaterialsAndPreserveExplicitChildOrder() throws Exception {
        var type = RenderTypes.entityCutoutCull(Identifier.withDefaultNamespace("gilded_chest_test.png"));
        assertTrue(type.affectsCrumbling(), "The real gilded chest material is eligible for native cracks");
        for (boolean improved : new boolean[] {false, true}) {
            try (var ignored = new NativeClientOptions(improved)) {
                var builder = new ModelGeometry.Builder();
                builder.getBuffer(type).addVertex(1, 2, 3).setColor(-1).setNormal(0, 1, 0);
                var model = new Model.Simple(new ModelPart(List.of(), Map.of()), texture -> type);
                var item = new ItemStackRenderState();
                item.newLayer().setupSpecialModel(new SpecialModelRenderer<Unit>() {
                    @Override public void submit(Unit argument, PoseStack pose, SubmitNodeCollector collector,
                            int light, int overlay, boolean foil, int outline) {
                        // Intentionally submit higher order first: native order is absolute,
                        // and every crack must follow even later normal child geometry.
                        collector.order(6).submitModel(model, Unit.INSTANCE, pose, type, light, overlay, 0xff667788, null, outline);
                        collector.order(3).submitModel(model, Unit.INSTANCE, pose, type, light, overlay, 0xff334455, null, outline);
                    }
                    @Override public void getExtents(Consumer<Vector3fc> output) { output.accept(new Vector3f()); }
                    @Override public Unit extractArgument(ItemStack stack) { return Unit.INSTANCE; }
                }, Unit.INSTANCE);
                builder.addItem(item, new PoseStack(), 15728880, 0);
                var geometry = builder.build();
                var storage = new SubmitNodeStorage();
                storage.setUseImprovedTransparency(improved);
                geometry.submitWithCrumbling(new PoseStack(), storage,
                    new ModelFeatureRenderer.CrumblingOverlay(4, new PoseStack().last()));
                assertEquals(Set.of(0, 3, 6, 7, 8, 9), storage.getSubmitsPerOrder().keySet());
                List<SubmitNode> actualDrawOrder = new ArrayList<>();
                // Match native PreparedFrame.executeSolid: ascending collector order,
                // with each real phase applying its own feature/material batching.
                storage.getSubmitsPerOrder().values().forEach(collection -> actualDrawOrder.addAll(drain(collection.solid)));
                assertEquals(6, actualDrawOrder.size());
                for (int i = 0; i < actualDrawOrder.size(); i++) {
                    var node = actualDrawOrder.get(i);
                    boolean crack = node instanceof ModelFeatureRenderer.Submit<?> submit && submit.sheetedDecalPose() != null;
                    assertEquals(i >= 3, crack, "Native batching must never place a crack before normal geometry");
                }
                assertEquals(List.of(-1, 0xff334455, 0xff667788), models(actualDrawOrder).stream()
                    .filter(submit -> submit.sheetedDecalPose() != null).map(ModelFeatureRenderer.Submit::tintedColor).toList());

                var cancelled = new SubmitNodeStorage();
                cancelled.setUseImprovedTransparency(improved);
                geometry.submit(new PoseStack(), cancelled);
                assertEquals(Set.of(0, 3, 6), cancelled.getSubmitsPerOrder().keySet());
                for (var collection : cancelled.getSubmitsPerOrder().values()) {
                    assertTrue(models(drain(collection.solid)).stream().allMatch(submit -> submit.sheetedDecalPose() == null));
                }
            }
        }
    }

    @Test void xyzExtentsAreFreshOwnedCornersAndNeverObserveLaterBuilderEdits() {
        var builder = new ModelGeometry.Builder();
        var vertices = builder.getBuffer(RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png")));
        vertices.addVertex(-2, -3, -4);
        vertices.addVertex(5, 6, 7);
        var geometry = builder.build();
        var first = geometry.extents();
        assertEquals(8, first.length);
        Set<List<Float>> corners = new java.util.HashSet<>();
        for (Vector3fc corner : first) corners.add(List.of(corner.x(), corner.y(), corner.z()));
        for (float x : new float[] {-2, 5}) for (float y : new float[] {-3, 6}) for (float z : new float[] {-4, 7}) {
            assertTrue(corners.contains(List.of(x, y, z)));
        }
        ((Vector3f) first[0]).set(99, 99, 99);
        first[1] = new Vector3f(99, 99, 99);
        vertices.addVertex(-100, -100, -100);
        assertEquals(new Vector3f(-2, -3, -4), geometry.extents()[0]);
        assertEquals(new Vector3f(5, -3, -4), geometry.extents()[1]);
        assertNotSame(geometry.extents()[0], geometry.extents()[0]);
        assertEquals(0, new ModelGeometry.Builder().build().extents().length);
    }

    @Test void deferredLightAndOverlayOverrideCapturedBatchesAndNativeChildrenWithoutMutatingSnapshot() {
        var type = RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png"));
        var builder = new ModelGeometry.Builder();
        builder.getBuffer(type).addVertex(0, 0, 0).setLight(0x00100020).setOverlay(0x00300040);
        var item = new ItemStackRenderState();
        item.newLayer().setQuads(ItemQuads.split(List.of(quad(type, 0))));
        builder.addItem(item, new PoseStack(), 0x00100020, 0x00300040);
        var block = new BlockModelRenderState();
        block.setupModel(new Matrix4f(), false).add(part(quad(type, 0), quad(type, 2)));
        builder.addBlock(block, new PoseStack(), 0x00100020, 0x00300040);
        builder.addText(net.minecraft.network.chat.Component.literal("label"), new PoseStack(), 0, 0, -1, 0, 0x00100020, net.minecraft.client.gui.Font.DisplayMode.NORMAL);
        var geometry = builder.build();
        var overridden = capture((pose, collector) -> geometry.submitWithLight(pose, collector, 0x00500060, 0x00700080, 0));
        assertArrayEquals(new Object[] {0x60, 0x50}, named(overridden, "setUv2").getFirst().args);
        assertArrayEquals(new Object[] {0x80, 0x70}, named(overridden, "setUv1").getFirst().args);
        assertEquals(0x00500060, named(overridden, "submitItem").getFirst().args[2]);
        assertEquals(0x00700080, named(overridden, "submitItem").getFirst().args[3]);
        assertEquals(0x00500060, named(overridden, "submitBlockModel").getFirst().args[4]);
        assertEquals(0x00700080, named(overridden, "submitBlockModel").getFirst().args[5]);
        assertEquals(0x00500060, named(overridden, "submitText").getFirst().args[6]);
        var original = capture(geometry::submit);
        assertArrayEquals(new Object[] {0x20, 0x10}, named(original, "setUv2").getFirst().args);
        assertArrayEquals(new Object[] {0x40, 0x30}, named(original, "setUv1").getFirst().args);
        assertEquals(0x00100020, named(original, "submitItem").getFirst().args[2]);
        assertEquals(0x00300040, named(original, "submitBlockModel").getFirst().args[5]);
    }

    @Test void nestedGeometryFlattensPosesOnceForVerticesNativeChildrenAndLabels() {
        var type = RenderTypes.entityCutout(Identifier.withDefaultNamespace("test.png"));
        var child = new ModelGeometry.Builder();
        child.getBuffer(type).addVertex(1, 2, 3).setNormal(1, 1, 0);
        var item = new ItemStackRenderState();
        item.newLayer().setQuads(ItemQuads.split(List.of(quad(type, 0))));
        var itemPose = new PoseStack(); itemPose.translate(2, 0, 0);
        child.addItem(item, itemPose, 1, 2);
        var block = new BlockModelRenderState();
        block.setupModel(new Matrix4f(), false).add(part(quad(type, 0), quad(type, 2)));
        var blockPose = new PoseStack(); blockPose.translate(0, 3, 0);
        child.addBlock(block, blockPose, 3, 4);
        var textPose = new PoseStack(); textPose.translate(0, 0, 4);
        child.addText(net.minecraft.network.chat.Component.literal("nested"), textPose, 0, 0, -1, 0, 5, net.minecraft.client.gui.Font.DisplayMode.NORMAL);
        var firstPose = new PoseStack(); firstPose.translate(10, 20, 30); firstPose.scale(2, 3, 4);
        var firstMatrix = new Matrix4f(firstPose.last().pose());
        var first = new ModelGeometry.Builder(); first.addGeometry(child.build(), firstPose);
        firstPose.translate(100, 100, 100); itemPose.translate(100, 100, 100); blockPose.translate(100, 100, 100); textPose.translate(100, 100, 100);
        var secondPose = new PoseStack(); secondPose.translate(-5, 7, 11); secondPose.scale(1, 0.5F, 2);
        var expected = new Matrix4f(secondPose.last().pose()).mul(firstMatrix);
        var outer = new ModelGeometry.Builder(); outer.addGeometry(first.build(), secondPose);
        secondPose.translate(100, 100, 100);
        var calls = capture(outer.build()::submit);
        var point = new Vector3f(1, 2, 3).mulPosition(expected);
        assertArrayEquals(new Object[] {point.x, point.y, point.z}, named(calls, "addVertex").getFirst().args);
        var expectedNormalPose = new PoseStack(); expectedNormalPose.mulPose(expected);
        var normal = expectedNormalPose.last().transformNormal(new Vector3f(1, 1, 0), new Vector3f());
        var actualNormal = named(calls, "setNormal").getFirst().args;
        assertEquals(normal.x, (float) actualNormal[0], 0.00001F);
        assertEquals(normal.y, (float) actualNormal[1], 0.00001F);
        assertEquals(normal.z, (float) actualNormal[2], 0.00001F);
        assertEquals(new Matrix4f(expected).translate(2, 0, 0).translate(-0.5F, -0.5F, -0.5F), ((PoseStack.Pose) named(calls, "submitItem").getFirst().args[0]).pose());
        assertEquals(new Matrix4f(expected).translate(0, 3, 0), ((PoseStack.Pose) named(calls, "submitBlockModel").getFirst().args[0]).pose());
        assertEquals(new Matrix4f(expected).translate(0, 0, 4), ((PoseStack.Pose) named(calls, "submitText").getFirst().args[0]).pose());
    }
}
