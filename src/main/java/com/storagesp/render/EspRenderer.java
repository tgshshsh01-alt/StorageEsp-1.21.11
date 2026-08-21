package com.storagesp.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.storagesp.config.ModConfig;
import com.storagesp.config.TrackedBlock;
import com.storagesp.scan.ChunkScanner;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Рисует ESP: полупрозрачные боксы вокруг отслеживаемых блоков и линии-трейсеры
 * от камеры игрока к каждому найденному хранилищу.
 * Работает через WorldRenderEvents.AFTER_TRANSLUCENT (Fabric API), рисует поверх
 * геометрии мира. "Сквозь стены" достигается отключением depth test.
 */
public class EspRenderer {

    // Отдельный буфер для линий и заливок, не завязанный на дефолтный тесселятор мира
    private static final BufferAllocator ALLOCATOR = new BufferAllocator(1536 * 10);

    public static void render(WorldRenderContext context) {
        if (!ModConfig.INSTANCE.masterEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        List<ChunkScanner.FoundEntry> entries = ChunkScanner.getCached();
        if (entries.isEmpty()) return;

        Vec3d camPos = context.camera().getPos();
        Matrix4f matrix = context.positionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        boolean throughWalls = ModConfig.INSTANCE.renderThroughWalls;
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableCull();

        if (ModConfig.INSTANCE.renderBoxes) {
            drawBoxes(entries, matrix, camPos);
        }
        if (ModConfig.INSTANCE.renderTracers) {
            drawTracers(entries, matrix, camPos, client);
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawBoxes(List<ChunkScanner.FoundEntry> entries, Matrix4f matrix, Vec3d camPos) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float alpha = ModConfig.INSTANCE.boxAlpha;
        for (ChunkScanner.FoundEntry entry : entries) {
            BlockPos p = entry.pos;
            TrackedBlock rule = entry.rule;

            double x0 = p.getX() - camPos.x;
            double y0 = p.getY() - camPos.y;
            double z0 = p.getZ() - camPos.z;
            double x1 = x0 + 1.0;
            double y1 = y0 + 1.0;
            double z1 = z0 + 1.0;

            addBoxQuads(buffer, matrix, x0, y0, z0, x1, y1, z1,
                    rule.colorR, rule.colorG, rule.colorB, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawTracers(List<ChunkScanner.FoundEntry> entries, Matrix4f matrix, Vec3d camPos, MinecraftClient client) {
        Entity cam = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
        // Линия идёт из точки чуть ниже камеры (примерно от груди) для более естественного вида
        Vec3d start = new Vec3d(0, cam.getStandingEyeHeight() * 0.5, 0);

        RenderSystem.lineWidth(ModConfig.INSTANCE.lineWidth);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (ChunkScanner.FoundEntry entry : entries) {
            BlockPos p = entry.pos;
            TrackedBlock rule = entry.rule;

            double ex = (p.getX() + 0.5) - camPos.x;
            double ey = (p.getY() + 0.5) - camPos.y;
            double ez = (p.getZ() + 0.5) - camPos.z;

            buffer.vertex(matrix, (float) start.x, (float) start.y, (float) start.z)
                    .color(rule.colorR, rule.colorG, rule.colorB, 0.9f);
            buffer.vertex(matrix, (float) ex, (float) ey, (float) ez)
                    .color(rule.colorR, rule.colorG, rule.colorB, 0.9f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);
    }

    private static void addBoxQuads(BufferBuilder buffer, Matrix4f m,
                                     double x0, double y0, double z0,
                                     double x1, double y1, double z1,
                                     float r, float g, float b, float a) {
        // 6 граней куба, каждая — 4 вершины (QUADS)
        // -X
        quad(buffer, m, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        // +X
        quad(buffer, m, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, r, g, b, a);
        // -Y
        quad(buffer, m, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r, g, b, a);
        // +Y
        quad(buffer, m, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1, r, g, b, a);
        // -Z
        quad(buffer, m, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, r, g, b, a);
        // +Z
        quad(buffer, m, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
    }

    private static void quad(BufferBuilder buffer, Matrix4f m,
                              double ax, double ay, double az,
                              double bx, double by, double bz,
                              double cx, double cy, double cz,
                              double dx, double dy, double dz,
                              float r, float g, float b, float a) {
        buffer.vertex(m, (float) ax, (float) ay, (float) az).color(r, g, b, a);
        buffer.vertex(m, (float) bx, (float) by, (float) bz).color(r, g, b, a);
        buffer.vertex(m, (float) cx, (float) cy, (float) cz).color(r, g, b, a);
        buffer.vertex(m, (float) dx, (float) dy, (float) dz).color(r, g, b, a);
    }
}
