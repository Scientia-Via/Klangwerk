package com.klangwerk.client;

import com.klangwerk.blockentity.AudioPlayerBE;
import com.klangwerk.blockentity.AudioPlayerBE.Action;
import com.klangwerk.client.audio.Mp3;
import com.klangwerk.network.ControlPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.nio.file.Path;
import java.util.List;

/** Menue von Grammophon und Plattenspieler: Songliste, Fortschrittsbalken, Spultasten. */
public class PlayerScreen extends Screen {
    private static final int W = 300, H = 204, ROWS = 8, ROW_H = 12;
    private final BlockPos pos;
    private List<String> songs = List.of();
    private int scroll, left, top;
    private Button playBtn, loopBtn;

    public PlayerScreen(BlockPos pos) {
        super(Component.empty());
        this.pos = pos;
    }

    private AudioPlayerBE be() {
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.getBlockEntity(pos) instanceof AudioPlayerBE a ? a : null;
    }

    private int listTop() { return top + 22; }
    private int barY() { return top + 128; }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        songs = Songs.list();
        scroll = Mth.clamp(scroll, 0, Math.max(0, songs.size() - ROWS));
        int y = top + 154;
        btn("««", "gui.klangwerk.back30", left + 8, y, 38, () -> seek(-30000));
        btn("«", "gui.klangwerk.back10", left + 48, y, 38, () -> seek(-10000));
        playBtn = Button.builder(Component.translatable("gui.klangwerk.play"), b -> send(Action.PLAY_PAUSE, 0, "", ""))
                .bounds(left + 88, y, 58, 20).build();
        addRenderableWidget(playBtn);
        addRenderableWidget(Button.builder(Component.translatable("gui.klangwerk.stop"), b -> send(Action.STOP, 0, "", ""))
                .bounds(left + 148, y, 46, 20).build());
        btn("»", "gui.klangwerk.fwd10", left + 196, y, 38, () -> seek(10000));
        btn("»»", "gui.klangwerk.fwd30", left + 236, y, 38, () -> seek(30000));
        loopBtn = Button.builder(Component.translatable("gui.klangwerk.loop_off"), b -> send(Action.LOOP, 0, "", ""))
                .bounds(left + 8, y + 24, 142, 20).build();
        addRenderableWidget(loopBtn);
        addRenderableWidget(Button.builder(Component.translatable("gui.klangwerk.reload"), b -> {
            songs = Songs.list();
            scroll = 0;
        }).bounds(left + 152, y + 24, 140, 20).build());
    }

    private void btn(String label, String tip, int x, int y, int w, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run())
                .bounds(x, y, w, 20).tooltip(Tooltip.create(Component.translatable(tip))).build());
    }

    private void send(Action a, long v, String s, String h) {
        PacketDistributor.sendToServer(new ControlPacket(pos, a, v, s, h));
    }

    private void seek(long deltaMs) {
        send(Action.SEEK_REL, deltaMs, "", "");
    }

    @Override
    public void tick() {
        AudioPlayerBE b = be();
        if (b == null) {
            onClose();
            return;
        }
        playBtn.setMessage(Component.translatable(b.isPlaying() ? "gui.klangwerk.pause" : "gui.klangwerk.play"));
        loopBtn.setMessage(Component.translatable(b.isLoop() ? "gui.klangwerk.loop_on" : "gui.klangwerk.loop_off"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String time(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** Pro Frame nur einmal zeichnen (egal, ob Screen.render den Hintergrund selbst aufruft). */
    private boolean backdropDrawn;

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        drawBackdrop(g, mx, my);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        backdropDrawn = false;
        drawBackdrop(g, mx, my);
        super.render(g, mx, my, pt);
    }

    /** Abgedunkelter Hintergrund, Panel, Songliste und Fortschrittsbalken (liegt hinter den Buttons). */
    private void drawBackdrop(GuiGraphics g, int mx, int my) {
        if (backdropDrawn) return;
        backdropDrawn = true;
        g.fillGradient(0, 0, width, height, -1072689136, -804253680);
        AudioPlayerBE b = be();
        g.fill(left, top, left + W, top + H, 0xE0141008);
        g.renderOutline(left, top, W, H, 0xFF9C6B30);
        if (b != null) g.drawCenteredString(font, b.getBlockState().getBlock().getName(), width / 2, top + 7, 0xFFE0B060);

        // Songliste
        int lx = left + 8, lw = W - 24, lt = listTop();
        g.fill(lx, lt, lx + lw, lt + ROWS * ROW_H, 0xFF000000);
        if (songs.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.klangwerk.empty1"), left + W / 2, lt + 30, 0xFFAAAAAA);
            g.drawCenteredString(font, Component.translatable("gui.klangwerk.empty2"), left + W / 2, lt + 44, 0xFF888888);
        }
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= songs.size()) break;
            int y = lt + i * ROW_H;
            String name = songs.get(idx);
            boolean cur = b != null && name.equals(b.song());
            boolean hov = mx >= lx && mx < lx + lw && my >= y && my < y + ROW_H;
            if (hov) g.fill(lx, y, lx + lw, y + ROW_H, 0xFF3A2A12);
            g.drawString(font, font.plainSubstrByWidth(name, lw - 6), lx + 3, y + 2, cur ? 0xFF7CFC00 : 0xFFE8E8E8, false);
        }
        // Scrollbalken
        if (songs.size() > ROWS) {
            int bx = lx + lw + 3, bh = ROWS * ROW_H;
            int hh = Math.max(10, bh * ROWS / songs.size());
            int hy = lt + (int) ((bh - hh) * (scroll / (float) (songs.size() - ROWS)));
            g.fill(bx, lt, bx + 5, lt + bh, 0xFF222222);
            g.fill(bx, hy, bx + 5, hy + hh, 0xFF9C6B30);
        }

        // Fortschritt
        int bx = left + 8, bw = W - 16, by = barY();
        g.fill(bx, by, bx + bw, by + 8, 0xFF000000);
        if (b != null) {
            long pos = b.positionMs(minecraft.level.getGameTime());
            long dur = b.duration();
            if (dur > 0) g.fill(bx, by, bx + (int) (bw * Mth.clamp(pos / (double) dur, 0, 1)), by + 8, 0xFF9C6B30);
            g.renderOutline(bx, by, bw, 8, 0xFF5A4020);
            String t = time(pos) + " / " + time(dur);
            String title = b.song().isEmpty() ? "-" : b.song();
            g.drawString(font, t, bx, by + 12, 0xFFFFFFFF, false);
            g.drawString(font, font.plainSubstrByWidth(title, bw - font.width(t) - 12), bx + font.width(t) + 10, by + 12, 0xFFB0B0B0, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        int lx = left + 8, lw = W - 24, lt = listTop();
        if (mx >= lx && mx < lx + lw && my >= lt && my < lt + ROWS * ROW_H) {
            int idx = scroll + (int) ((my - lt) / ROW_H);
            if (idx < songs.size()) {
                String name = songs.get(idx);
                Path f = Songs.resolve(name);
                if (f != null) send(Action.SELECT, Mp3.durationMs(f), name, Songs.hashOf(f));
            }
            return true;
        }
        AudioPlayerBE b = be();
        int bx = left + 8, bw = W - 16, by = barY();
        if (b != null && b.duration() > 0 && mx >= bx && mx < bx + bw && my >= by && my < by + 8) {
            send(Action.SEEK_ABS, (long) (b.duration() * ((mx - bx) / bw)), "", "");
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int max = Math.max(0, songs.size() - ROWS);
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        return true;
    }
}
