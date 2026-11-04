/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Geometry captured while a posable model's animation and render context are current.
 * Deferred rendering only sees independent vertex arrays and materials: no entity,
 * shared ModelPart, MoLang runtime, or mutable animation state crosses that boundary.
 */
public final class ModelGeometry {
    private final List<Batch> batches;
    private final List<ItemSnapshot> items;
    private final List<TextSnapshot> texts;
    private final List<BlockSnapshot> blocks;
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;
    private final float minZ;
    private final float maxZ;

    private ModelGeometry(List<Batch> batches, List<ItemSnapshot> items, List<TextSnapshot> texts, List<BlockSnapshot> blocks, float minX, float minY, float maxX, float maxY, float minZ, float maxZ) {
        this.batches = List.copyOf(batches);
        this.items = List.copyOf(items);
        this.texts = List.copyOf(texts);
        this.blocks = List.copyOf(blocks);
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public boolean isEmpty() { return batches.isEmpty() && items.isEmpty() && texts.isEmpty() && blocks.isEmpty(); }
    public float minX() { return minX; }
    public float minY() { return minY; }
    public float maxX() { return maxX; }
    public float maxY() { return maxY; }
    public org.joml.Vector3fc[] extents() {
        if (!Float.isFinite(minX)) return new org.joml.Vector3fc[0];
        org.joml.Vector3fc[] result = new org.joml.Vector3fc[8];
        for (int i = 0; i < 8; i++) result[i] = new Vector3f(
            (i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ);
        return result;
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector) {
        submit(pose, collector, 0);
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int outlineColor) {
        submit(pose, collector, outlineColor, -1, -1);
    }

    /** Adds the native breaking pass without changing the normal material/outline submissions. */
    public void submitWithCrumbling(PoseStack pose, SubmitNodeCollector collector, ModelFeatureRenderer.CrumblingOverlay overlay) {
        var schedule = new CrumblingSchedule(collector);
        submit(pose, new CrumblingCollector(schedule, collector, 0, overlay));
        schedule.submitOverlays();
    }

    public void submitWithLight(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        submit(pose, collector, outlineColor, light, overlay);
    }

    private void submit(PoseStack pose, SubmitNodeCollector collector, int outlineColor, int light, int overlay) {
        for (int index = 0; index < batches.size(); index++) {
            Batch batch = batches.get(index);
            // Layers redraw the base model's vertices at equal depth, so they must draw after it. Batches that
            // share an order draw in the hash order of their render types, which can change every frame, so each
            // later batch gets its own order, as vanilla's EyesLayer does.
            OrderedSubmitNodeCollector target = index == 0 ? collector : collector.order(index);
            if (!batch.type.isOutline()) {
                target.submitCustomGeometry(pose, batch.type, (matrix, buffer) -> batch.emit(matrix, buffer, 0, light, overlay));
            }
            if (outlineColor != 0) {
                RenderType outline = batch.type.isOutline() ? batch.type : batch.type.outline().orElse(null);
                if (outline != null) {
                    target.submitCustomGeometry(pose, outline, (matrix, buffer) -> batch.emit(matrix, buffer,
                        outlineColor, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY));
                }
            }
        }
        for (ItemSnapshot item : items) {
            pose.pushPose();
            pose.mulPose(item.pose);
            item.state.submit(pose, collector, light < 0 ? item.light : light, overlay < 0 ? item.overlay : overlay, outlineColor);
            pose.popPose();
        }
        for (TextSnapshot text : texts) {
            pose.pushPose();
            pose.mulPose(text.pose);
            collector.submitText(pose, text.x, text.y, text.text, false, text.mode,
                light < 0 ? text.light : light, text.color, text.background, 0);
            pose.popPose();
        }
        for (BlockSnapshot block : blocks) {
            pose.pushPose();
            pose.mulPose(block.pose);
            block.state.submit(pose, collector, light < 0 ? block.light : light, overlay < 0 ? block.overlay : overlay, outlineColor);
            pose.popPose();
        }
    }

    /**
     * Adapts frozen arbitrary geometry to the native model breaking path. The ordinary
     * Model renderer is final, but Cube.compile is a public vertex-emission hook.
     * Each adapter owns an otherwise empty model; it contains no animated model parts.
     */
    private static final class GeometryCube extends ModelPart.Cube {
        private final SubmitNodeCollector.CustomGeometryRenderer renderer;

        private GeometryCube(SubmitNodeCollector.CustomGeometryRenderer renderer) {
            super(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 1, 1, Set.of());
            this.renderer = renderer;
        }

        @Override
        public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
            renderer.render(pose, buffer);
        }
    }

    /** Native phases batch by feature/material, so cracks need a later collector order. */
    private static final class CrumblingSchedule {
        private final SubmitNodeCollector root;
        private final Map<Integer, List<Consumer<OrderedSubmitNodeCollector>>> pending = new TreeMap<>();
        private int maxNormalOrder;

        private CrumblingSchedule(SubmitNodeCollector root) { this.root = root; }

        private void submitOverlays() {
            int overlayOrder = maxNormalOrder;
            for (var submissions : pending.values()) {
                var collector = root.order(overlayOrder = Math.incrementExact(overlayOrder));
                submissions.forEach(submission -> submission.accept(collector));
            }
        }
    }

    /** Keeps special item/block models on the same breaking path as recorded geometry. */
    private static final class CrumblingCollector implements SubmitNodeCollector {
        private final CrumblingSchedule schedule;
        private final OrderedSubmitNodeCollector delegate;
        private final int order;
        private final ModelFeatureRenderer.CrumblingOverlay overlay;

        private CrumblingCollector(CrumblingSchedule schedule, OrderedSubmitNodeCollector delegate, int order, ModelFeatureRenderer.CrumblingOverlay overlay) {
            this.schedule = schedule;
            this.delegate = delegate;
            this.order = order;
            this.overlay = new ModelFeatureRenderer.CrumblingOverlay(overlay.progress(), overlay.cameraPose().copy());
        }

        @Override public OrderedSubmitNodeCollector order(int order) {
            schedule.maxNormalOrder = Math.max(schedule.maxNormalOrder, order);
            return new CrumblingCollector(schedule, schedule.root.order(order), order, overlay);
        }

        private <S> void deferOverlay(Model<? super S> model, S state, PoseStack pose, RenderType type,
                int light, int overlayCoords, int tint, ModelFeatureRenderer.CrumblingOverlay crumbling) {
            if (!type.affectsCrumbling()) return;
            var frozenPose = new PoseStack();
            frozenPose.last().set(pose.last());
            var frozenOverlay = new ModelFeatureRenderer.CrumblingOverlay(crumbling.progress(), crumbling.cameraPose().copy());
            schedule.pending.computeIfAbsent(order, ignored -> new ArrayList<>()).add(collector ->
                collector.submitCrumblingOverlay(model, state, frozenPose, type, light, overlayCoords, tint, frozenOverlay));
        }

        private void submitOverlay(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
            if (!type.affectsCrumbling()) return;
            Model.Simple model = new Model.Simple(new ModelPart(List.of(new GeometryCube(renderer)), Map.of()), ignored -> type);
            deferOverlay(model, Unit.INSTANCE, pose, type, 15728880, 0, -1, overlay);
        }

        private void submitQuads(PoseStack pose, RenderType type, List<BakedQuad> quads, int light, int overlayCoords) {
            if (!type.affectsCrumbling() || quads.isEmpty()) return;
            List<BakedQuad> snapshot = List.copyOf(quads);
            submitOverlay(pose, type, (matrix, buffer) -> {
                QuadInstance instance = new QuadInstance();
                instance.setLightCoords(light);
                instance.setOverlayCoords(overlayCoords);
                instance.setColor(-1);
                for (BakedQuad quad : snapshot) buffer.putBakedQuad(matrix, quad, instance);
            });
        }

        @Override public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
            delegate.submitCustomGeometry(pose, type, renderer);
            submitOverlay(pose, type, renderer);
        }

        @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type,
                int light, int overlayCoords, int tint, UvMapping uvMapping, int outline) {
            delegate.submitModel(model, state, pose, type, light, overlayCoords, tint, uvMapping, outline);
            deferOverlay(model, state, pose, type, light, overlayCoords, tint, overlay);
        }

        @Override public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack pose,
                RenderType type, int light, int overlayCoords, int tint, ModelFeatureRenderer.CrumblingOverlay crumbling) {
            deferOverlay(model, state, pose, type, light, overlayCoords, tint, crumbling);
        }

        @Override public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlayCoords,
                int outline, int[] tints, ItemQuads quads, ItemStackRenderState.FoilType foil) {
            delegate.submitItem(pose, context, light, overlayCoords, outline, tints, quads, foil);
            Map<RenderType, List<BakedQuad>> byMaterial = new LinkedHashMap<>();
            for (BakedQuad quad : quads.all()) {
                RenderType type = quad.materialInfo().itemRenderType();
                if (type.affectsCrumbling()) byMaterial.computeIfAbsent(type, ignored -> new ArrayList<>()).add(quad);
            }
            byMaterial.forEach((type, batch) -> submitQuads(pose, type, batch, light, overlayCoords));
        }

        @Override public void submitBlockModel(PoseStack pose, RenderType type, List<BlockStateModelPart> parts,
                int[] tints, int light, int overlayCoords, int outline) {
            delegate.submitBlockModel(pose, type, parts, tints, light, overlayCoords, outline);
            if (!type.affectsCrumbling()) return;
            List<BakedQuad> quads = new ArrayList<>();
            for (BlockStateModelPart part : parts) {
                for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
                quads.addAll(part.getQuads(null));
            }
            submitQuads(pose, type, quads, light, overlayCoords);
        }

        // Preserve auxiliary submissions; native item/block model paths are handled above.
        @Override public void submitShadow(PoseStack pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
            delegate.submitShadow(pose, radius, pieces);
        }
        @Override public void submitNameTag(PoseStack pose, Vec3 attachment, int offset, Component name,
                boolean seeThrough, int light, CameraRenderState camera) {
            delegate.submitNameTag(pose, attachment, offset, name, seeThrough, light, camera);
        }
        @Override public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean shadow,
                Font.DisplayMode mode, int light, int color, int background, int outline) {
            delegate.submitText(pose, x, y, text, shadow, mode, light, color, background, outline);
        }
        @Override public void submitTextBackground(PoseStack pose, float x0, float y0, float x1, float y1,
                int color, Font.DisplayMode mode, int light) {
            delegate.submitTextBackground(pose, x0, y0, x1, y1, color, mode, light);
        }
        @Override public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
            delegate.submitFlame(pose, state, rotation);
        }
        @Override public void submitLeash(PoseStack pose, EntityRenderState.LeashState state) { delegate.submitLeash(pose, state); }
        @Override public void submitMovingBlock(PoseStack pose, MovingBlockRenderState state, int outline) {
            delegate.submitMovingBlock(pose, state, outline);
        }
        @Override public void submitBreakingBlockModel(PoseStack pose, List<BlockStateModelPart> parts, int progress, boolean translucent) {
            delegate.submitBreakingBlockModel(pose, parts, progress, translucent);
        }
        @Override public void submitShapeOutline(PoseStack pose, VoxelShape shape, RenderType type, int color, float width, boolean afterTerrain) {
            delegate.submitShapeOutline(pose, shape, type, color, width, afterTerrain);
        }
        @Override public void submitQuadParticleGroup(QuadParticleRenderState state) { delegate.submitQuadParticleGroup(state); }
        @Override public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
            delegate.submitGizmoPrimitives(group, camera, onTop);
        }
    }

    private record ItemSnapshot(ItemStackRenderState state, Matrix4f pose, int light, int overlay) {}
    private record BlockSnapshot(BlockModelRenderState state, Matrix4f pose, int light, int overlay) {}
    private record TextSnapshot(FormattedCharSequence text, Matrix4f pose, float x, float y,
                                int color, int background, int light, Font.DisplayMode mode) {}

    private record Batch(RenderType type, int[] vertices) {
        private void emit(PoseStack.Pose pose, VertexConsumer output, int outlineColor) {
            emit(pose, output, outlineColor, -1, -1);
        }
        private void emit(PoseStack.Pose pose, VertexConsumer output, int outlineColor, int light, int overlay) {
            for (int i = 0; i < vertices.length; i += RecordingConsumer.STRIDE) {
                output.addVertex(pose, value(i), value(i + 1), value(i + 2))
                    .setColor(outlineColor == 0 ? vertices[i + 3] : outlineColor)
                    .setUv(value(i + 4), value(i + 5))
                    .setOverlay(overlay < 0 ? vertices[i + 6] : overlay)
                    .setLight(light < 0 ? vertices[i + 7] : light)
                    .setNormal(pose, value(i + 8), value(i + 9), value(i + 10))
                    .setUv3(value(i + 11), value(i + 12))
                    .setLineWidth(value(i + 13));
            }
        }

        private float value(int index) { return Float.intBitsToFloat(vertices[index]); }
    }

    public static final class Builder {
        private final Map<RenderType, RecordingConsumer> buffers = new LinkedHashMap<>();
        private final List<ItemSnapshot> items = new ArrayList<>();
        private final List<TextSnapshot> texts = new ArrayList<>();
        private final List<BlockSnapshot> blocks = new ArrayList<>();

        /** Takes ownership of an independently extracted block state. */
        public void addBlock(BlockModelRenderState state, PoseStack pose, int light, int overlay) {
            if (!state.isEmpty()) blocks.add(new BlockSnapshot(state, new Matrix4f(pose.last().pose()), light, overlay));
        }

        /** Adds a frozen sub-scene, used by habitat previews without entity/shadow submission. */
        public void addGeometry(ModelGeometry geometry, PoseStack pose) {
            for (Batch batch : geometry.batches) batch.emit(pose.last(), getBuffer(batch.type), 0);
            for (ItemSnapshot item : geometry.items) items.add(new ItemSnapshot(item.state,
                new Matrix4f(pose.last().pose()).mul(item.pose), item.light, item.overlay));
            for (BlockSnapshot block : geometry.blocks) blocks.add(new BlockSnapshot(block.state,
                new Matrix4f(pose.last().pose()).mul(block.pose), block.light, block.overlay));
            for (TextSnapshot text : geometry.texts) texts.add(new TextSnapshot(text.text,
                new Matrix4f(pose.last().pose()).mul(text.pose), text.x, text.y, text.color, text.background, text.light, text.mode));
        }

        /** Resolves styled text now, so later component edits cannot affect the frame. */
        public void addText(Component text, PoseStack pose, float x, float y, int color,
                            int background, int light, Font.DisplayMode mode) {
            List<FormattedCharSequence> characters = new ArrayList<>();
            text.getVisualOrderText().accept((index, style, codepoint) -> {
                characters.add(FormattedCharSequence.codepoint(codepoint, style));
                return true;
            });
            texts.add(new TextSnapshot(FormattedCharSequence.composite(List.copyOf(characters)),
                new Matrix4f(pose.last().pose()), x, y, color, background, light, mode));
        }

        /** Takes ownership of a newly extracted item state, which must not be reused or mutated. */
        public void addItem(ItemStackRenderState state, PoseStack pose, int light, int overlay) {
            if (!state.isEmpty()) items.add(new ItemSnapshot(state, new Matrix4f(pose.last().pose()), light, overlay));
        }

        public VertexConsumer getBuffer(RenderType type) {
            return buffers.computeIfAbsent(type, ignored -> new RecordingConsumer());
        }

        /** Copies all buffers so further extraction cannot change the submitted frame. */
        public ModelGeometry build() {
            List<Batch> batches = new ArrayList<>();
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (Map.Entry<RenderType, RecordingConsumer> entry : buffers.entrySet()) {
                RecordingConsumer buffer = entry.getValue();
                if (buffer.size == 0) continue;
                batches.add(new Batch(entry.getKey(), Arrays.copyOf(buffer.vertices, buffer.size)));
                minX = Math.min(minX, buffer.minX);
                minY = Math.min(minY, buffer.minY);
                maxX = Math.max(maxX, buffer.maxX);
                maxY = Math.max(maxY, buffer.maxY);
                minZ = Math.min(minZ, buffer.minZ);
                maxZ = Math.max(maxZ, buffer.maxZ);
            }
            for (ItemSnapshot item : items) {
                var box = item.state.getModelBoundingBox();
                Vector3f position = new Vector3f();
                for (int corner = 0; corner < 8; corner++) {
                    item.pose.transformPosition((float) ((corner & 1) == 0 ? box.minX : box.maxX),
                        (float) ((corner & 2) == 0 ? box.minY : box.maxY),
                        (float) ((corner & 4) == 0 ? box.minZ : box.maxZ), position);
                    minX = Math.min(minX, position.x);
                    minY = Math.min(minY, position.y);
                    maxX = Math.max(maxX, position.x);
                    maxY = Math.max(maxY, position.y);
                    minZ = Math.min(minZ, position.z);
                    maxZ = Math.max(maxZ, position.z);
                }
            }
            return new ModelGeometry(batches, items, texts, blocks, minX, minY, maxX, maxY, minZ, maxZ);
        }
    }

    private static final class RecordingConsumer implements VertexConsumer {
        private static final int STRIDE = 14;
        private int[] vertices = new int[STRIDE * 256];
        private int size;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private VertexConsumer put(int offset, int value) {
            if (size == 0) throw new IllegalStateException("A vertex must precede its attributes");
            vertices[size - STRIDE + offset] = value;
            return this;
        }

        private VertexConsumer putFloat(int offset, float value) {
            return put(offset, Float.floatToRawIntBits(value));
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException("Non-finite extracted model vertex");
            }
            if (size + STRIDE > vertices.length) vertices = Arrays.copyOf(vertices, vertices.length * 2);
            size += STRIDE;
            putFloat(0, x);
            putFloat(1, y);
            putFloat(2, z);
            put(3, -1);
            putFloat(13, 1);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            return this;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            return setColor((a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255);
        }
        @Override public VertexConsumer setColor(int color) { return put(3, color); }
        @Override public VertexConsumer setUv(float u, float v) { putFloat(4, u); return putFloat(5, v); }
        @Override public VertexConsumer setUv1(int u, int v) { return put(6, (u & 65535) | (v & 65535) << 16); }
        @Override public VertexConsumer setUv2(int u, int v) { return put(7, (u & 65535) | (v & 65535) << 16); }
        @Override public VertexConsumer setUv3(float u, float v) { putFloat(11, u); return putFloat(12, v); }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            putFloat(8, x); putFloat(9, y); return putFloat(10, z);
        }
        @Override public VertexConsumer setLineWidth(float width) { return putFloat(13, width); }
    }
}
