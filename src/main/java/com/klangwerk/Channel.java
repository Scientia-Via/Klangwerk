package com.klangwerk;

import net.minecraft.network.chat.Component;

/** Die 6 Ausgaenge auf der Rueckseite des Plattenspielers (3 Spalten x 2 Reihen). */
public enum Channel {
    LEFT(0, 0, 0), CENTER(1, 0, 2), RIGHT(2, 0, 1),
    REAR_LEFT(0, 1, 0), SUB(1, 1, 2), REAR_RIGHT(2, 1, 1);

    public final int col, row;
    /** 0 = linker Kanal, 1 = rechter Kanal, 2 = Mono-Mix */
    public final int source;

    Channel(int col, int row, int source) {
        this.col = col;
        this.row = row;
        this.source = source;
    }

    public static Channel at(int col, int row) {
        for (Channel c : values()) if (c.col == col && c.row == row) return c;
        return LEFT;
    }

    public Component label() {
        return Component.translatable("channel.klangwerk." + name().toLowerCase());
    }
}
