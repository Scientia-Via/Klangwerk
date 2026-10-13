package com.klangwerk.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.nio.file.Files;
import java.util.List;

/** Zeigt alle Dateien im Music-Ordner und oeffnet den Ordner im Datei-Explorer. */
public class MusicFolderScreen extends Screen {
    private static final int ROWS = 10, ROW_H = 12;
    private final Screen parent;
    private List<String> files = List.of();
    private int scroll;

    public MusicFolderScreen(Screen parent) {
        super(Component.translatable("gui.klangwerk.music_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        files = Songs.listAll();
        int cx = width / 2, y = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.klangwerk.open_folder"), b -> {
            Songs.ensureDir();
            Util.getPlatform().openPath(Songs.dir());
        }).bounds(cx - 154, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.klangwerk.reload"), b -> {
            files = Songs.listAll();
            scroll = 0;
        }).bounds(cx - 50, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.klangwerk.done"), b -> onClose())
                .bounds(cx + 54, y, 100, 20).build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.klangwerk.music_hint"), width / 2, 24, 0xFFAAAAAA);
        int lw = Math.min(300, width - 40), lx = (width - lw) / 2, lt = 42;
        g.fill(lx, lt, lx + lw, lt + ROWS * ROW_H + 4, 0xC0000000);
        g.drawString(font, Songs.dir().toString(), lx, lt + ROWS * ROW_H + 8, 0xFF777777, false);
        if (files.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.klangwerk.empty1"), width / 2, lt + 40, 0xFFAAAAAA);
            return;
        }
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= files.size()) break;
            String name = files.get(idx);
            boolean mp3 = Songs.isMp3(name);
            String label = mp3 ? name : name + " " + Component.translatable("gui.klangwerk.unsupported").getString();
            g.drawString(font, font.plainSubstrByWidth(label, lw - 8), lx + 4, lt + 3 + i * ROW_H, mp3 ? 0xFFE8E8E8 : 0xFF777777, false);
        }
        g.drawString(font, files.size() + "", lx + lw - 6 - font.width(files.size() + ""), lt + ROWS * ROW_H + 8, 0xFF777777, false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Mth.clamp(scroll - (int) Math.signum(sy), 0, Math.max(0, files.size() - ROWS));
        return true;
    }
}
