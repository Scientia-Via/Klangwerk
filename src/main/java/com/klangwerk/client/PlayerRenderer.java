package com.klangwerk.client;

import com.klangwerk.Channel;
import com.klangwerk.Klangwerk;
import com.klangwerk.block.AudioPlayerBlock;
import com.klangwerk.blockentity.AudioPlayerBE;
import com.klangwerk.blockentity.TurntableBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;

/** Zeichnet die sich drehende Schallplatte (33 1/3 U/min) und - beim Plattenspieler - die Kabel zu den Boxen. */
public class PlayerRenderer<T extends AudioPlayerBE> implements BlockEntityRenderer<T> {
    private static final ResourceLocation RECORD = ResourceLocation.fromNamespaceAndPath(Klangwerk.MODID, "textures/block/record.png");
    private final float cx, cy, cz, radius;

    public PlayerRenderer(float cx, float cy, float cz, float radius) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.radius = radius;
    }

    @Override
    public void render(T be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        Direction facing = be.getBlockState().getValue(AudioPlayerBlock.FACING);
        int lit = LevelRenderer.getLightColor(level, be.getBlockPos().above());

        if (!be.song().isEmpty()) {
            double ms = be.positionMs(level.getGameTime()) + (be.isPlaying() ? pt * 50.0 : 0.0);
            float angle = (float) ((ms * 0.2) % 360.0); // 200 Grad/s = 33 1/3 U/min
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            ps.translate((cx - 8f) / 16f, cy / 16f, (cz - 8f) / 16f);
            ps.mulPose(Axis.YP.rotationDegrees(angle));
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(RECORD));
            PoseStack.Pose pose = ps.last();
            float s = radius / 16f;
            vert(vc, pose, -s, 0, -s, 0, 0, lit);
            vert(vc, pose, -s, 0, s, 0, 1, lit);
            vert(vc, pose, s, 0, s, 1, 1, lit);
            vert(vc, pose, s, 0, -s, 1, 0, lit);
            ps.popPose();
        }

        if (be instanceof TurntableBE tb) drawCables(tb, facing, ps, buf);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private void drawCables(TurntableBE tb, Direction facing, PoseStack ps, MultiBufferSource buf) {
        Direction back = facing.getOpposite();
        Direction right = back.getCounterClockWise();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        PoseStack.Pose pose = ps.last();
        BlockPos origin = tb.getBlockPos();
        for (Map.Entry<Channel, List<BlockPos>> e : tb.speakers().entrySet()) {
            Channel ch = e.getKey();
            double off = (ch.col - 1) * 0.28;
            Vec3 start = new Vec3(
                    0.5 + back.getStepX() * 0.52 + right.getStepX() * off,
                    ch.row == 0 ? 0.375 : 0.125,
                    0.5 + back.getStepZ() * 0.52 + right.getStepZ() * off);
            for (BlockPos sp : e.getValue()) {
                Vec3 end = Vec3.atCenterOf(sp).subtract(Vec3.atLowerCornerOf(origin));
                double sag = 0.25 + start.distanceTo(end) * 0.06;
                final int segs = 14;
                Vec3 prev = start;
                for (int i = 1; i <= segs; i++) {
                    double t = i / (double) segs;
                    Vec3 p = start.lerp(end, t).add(0, -4.0 * sag * t * (1 - t), 0);
                    line(vc, pose, prev, p);
                    prev = p;
                }
            }
        }
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-6) return;
        float nx = (float) (d.x / len), ny = (float) (d.y / len), nz = (float) (d.z / len);
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(25, 25, 25, 255).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(25, 25, 25, 255).setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
