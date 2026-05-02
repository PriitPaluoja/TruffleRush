package com.example.demo.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.util.EnumMap;
import java.util.Map;

/**
 * Lightweight synthesized audio. Generates simple PCM tones at startup so we
 * don't need any external sound files. Each {@link Sfx} is a short envelope-
 * shaped tone. Failures (no audio device, sandbox, etc.) are silently swallowed.
 */
public class AudioManager {

    public enum Sfx {
        COLLECT,        // bright high blip
        BIG_COLLECT,    // golden truffle / super acorn
        HIT,            // wolf catches you / starvation
        SHIELD_BLOCK,   // shield consumed
        LEVEL_UP,       // round-end fanfare (rising arpeggio)
        WOLF_HOWL,      // wolf spawn warning (low rumble)
        HEARTBEAT,      // low weight (low thump)
        BOON_PICK,      // chime when boon picked
        ACHIEVEMENT     // achievement unlock
    }

    private static final float SAMPLE_RATE = 44_100f;
    private final Map<Sfx, byte[]> samples = new EnumMap<>(Sfx.class);
    private boolean enabled = true;

    public AudioManager() {
        try {
            samples.put(Sfx.COLLECT,       envelope(tone(880,  0.07), 0.005, 0.05));
            samples.put(Sfx.BIG_COLLECT,   envelope(arpeggio(new int[]{660, 880, 1320}, 0.06), 0.005, 0.10));
            samples.put(Sfx.HIT,           envelope(tone(140,  0.18), 0.005, 0.15));
            samples.put(Sfx.SHIELD_BLOCK,  envelope(tone(540,  0.10), 0.003, 0.08));
            samples.put(Sfx.LEVEL_UP,      envelope(arpeggio(new int[]{523, 659, 784, 1046}, 0.10), 0.005, 0.30));
            samples.put(Sfx.WOLF_HOWL,     envelope(tone(220,  0.30), 0.05,  0.25));
            samples.put(Sfx.HEARTBEAT,     envelope(tone(80,   0.12), 0.005, 0.10));
            samples.put(Sfx.BOON_PICK,     envelope(arpeggio(new int[]{784, 988}, 0.06), 0.005, 0.10));
            samples.put(Sfx.ACHIEVEMENT,   envelope(arpeggio(new int[]{1046, 1318, 1568}, 0.08), 0.005, 0.20));
        } catch (Throwable t) {
            enabled = false;
        }
    }

    public void play(Sfx sfx) {
        if (!enabled) return;
        byte[] data = samples.get(sfx);
        if (data == null) return;
        // Play asynchronously so the game loop never blocks on audio.
        Thread t = new Thread(() -> playRaw(data));
        t.setDaemon(true);
        t.start();
    }

    private void playRaw(byte[] data) {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, fmt);
            try (Clip clip = (Clip) AudioSystem.getLine(info)) {
                clip.open(fmt, data, 0, data.length);
                clip.start();
                // Wait for clip to finish so try-with-resources can close it.
                while (clip.isRunning()) {
                    Thread.sleep(5);
                }
            }
        } catch (Throwable t) {
            // Silently ignore — audio is non-essential.
        }
    }

    /** Generate a sine-wave PCM tone of the given frequency for the given duration. */
    private static byte[] tone(double freqHz, double seconds) {
        int n = (int) (seconds * SAMPLE_RATE);
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            short s = (short) (Math.sin(2 * Math.PI * freqHz * t) * 0.4 * Short.MAX_VALUE);
            out[i * 2]     = (byte) (s & 0xFF);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }

    /** Concatenate a series of tones, one per frequency. */
    private static byte[] arpeggio(int[] freqs, double secondsEach) {
        byte[][] parts = new byte[freqs.length][];
        int total = 0;
        for (int i = 0; i < freqs.length; i++) {
            parts[i] = tone(freqs[i], secondsEach);
            total += parts[i].length;
        }
        byte[] out = new byte[total];
        int o = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, o, p.length);
            o += p.length;
        }
        return out;
    }

    /** Apply a simple linear attack/decay envelope to a 16-bit PCM clip. */
    private static byte[] envelope(byte[] pcm, double attackSeconds, double releaseSeconds) {
        int n = pcm.length / 2;
        int attack  = (int) (attackSeconds  * SAMPLE_RATE);
        int release = (int) (releaseSeconds * SAMPLE_RATE);
        for (int i = 0; i < n; i++) {
            short s = (short) ((pcm[i * 2] & 0xFF) | (pcm[i * 2 + 1] << 8));
            double gain = 1.0;
            if (i < attack) gain = i / (double) attack;
            else if (i > n - release) gain = Math.max(0, (n - i) / (double) release);
            short g = (short) (s * gain);
            pcm[i * 2]     = (byte) (g & 0xFF);
            pcm[i * 2 + 1] = (byte) ((g >> 8) & 0xFF);
        }
        return pcm;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
}
