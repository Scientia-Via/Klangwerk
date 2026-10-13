package com.klangwerk;

/** Frequenzbereich einer Box: Hochpass- und Tiefpassfrequenz in Hz (0 = aus). */
public enum SpeakerType {
    TREBLE(3500f, 0f),
    MID(250f, 3500f),
    BASS(0f, 250f),
    GRAMOPHONE(200f, 5000f);

    public final float highpass, lowpass;

    SpeakerType(float highpass, float lowpass) {
        this.highpass = highpass;
        this.lowpass = lowpass;
    }
}
