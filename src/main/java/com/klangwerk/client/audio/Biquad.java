package com.klangwerk.client.audio;

/** Butterworth-Filter 2. Ordnung (RBJ-Cookbook), Transposed Direct Form II. */
final class Biquad {
    private final double b0, b1, b2, a1, a2;
    private double z1, z2;

    private Biquad(double b0, double b1, double b2, double a1, double a2) {
        this.b0 = b0; this.b1 = b1; this.b2 = b2; this.a1 = a1; this.a2 = a2;
    }

    static Biquad lowpass(double f, double fs) {
        double w = 2 * Math.PI * f / fs, c = Math.cos(w), a = Math.sin(w) / (2 * 0.70710678);
        double a0 = 1 + a;
        return new Biquad((1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0, -2 * c / a0, (1 - a) / a0);
    }

    static Biquad highpass(double f, double fs) {
        double w = 2 * Math.PI * f / fs, c = Math.cos(w), a = Math.sin(w) / (2 * 0.70710678);
        double a0 = 1 + a;
        return new Biquad((1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0, -2 * c / a0, (1 - a) / a0);
    }

    float process(float x) {
        double y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return (float) y;
    }
}
