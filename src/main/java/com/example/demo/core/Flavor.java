package com.example.demo.core;

import com.example.demo.world.Biome;

import java.util.List;
import java.util.Random;

/**
 * Static string tables for narrative texture: rival taunts, biome blurbs, and
 * endless-mode flavor. Lookups are stateless apart from a single shared RNG so
 * repeated taunts feel varied. Lines are short (one log entry each) so they fit
 * the existing {@code SidePanelRenderer} scrolling event log.
 */
public final class Flavor {

    private static final Random RNG = new Random();

    private Flavor() {}

    // -------------------------------------------------------------------------
    // Rival taunts (N1)
    // -------------------------------------------------------------------------

    private static final List<String> HOGGART_TRUFFLE = List.of(
        "Hoggart: Yoink!",
        "Hoggart: Mine now, slowpoke.",
        "Hoggart: Snortle!",
        "Hoggart: Truffle? What truffle?"
    );
    private static final List<String> WHISKERS_TRUFFLE = List.of(
        "Whiskers: Predictable.",
        "Whiskers: I had it tracked since dawn.",
        "Whiskers: You blinked first.",
        "Whiskers: Some of us prepare."
    );
    private static final List<String> BRAMBLE_TRUFFLE = List.of(
        "Bramble: Mine. As usual.",
        "Bramble: Step aside, sausage.",
        "Bramble: Don't make this awkward.",
        "Bramble: Move."
    );

    /** Returns a flavour line when the named rival snags a truffle. Empty if none configured. */
    public static String tauntForTruffle(String rivalName) {
        return switch (rivalName) {
            case "Hoggart" -> pick(HOGGART_TRUFFLE);
            case "Whiskers" -> pick(WHISKERS_TRUFFLE);
            case "Bramble" -> pick(BRAMBLE_TRUFFLE);
            default -> "";
        };
    }

    // -------------------------------------------------------------------------
    // Biome blurbs (N4)
    // -------------------------------------------------------------------------

    public static String blurbFor(Biome biome) {
        return switch (biome) {
            case FOREST -> "The forest air smells of last week's rain.";
            case SWAMP  -> "Mud sucks at every hoof. Move with care.";
            case FARM   -> "Old wire glints between the rows. Eyes up.";
        };
    }

    // -------------------------------------------------------------------------
    // Endless-mode lore (N5)
    // -------------------------------------------------------------------------

    private static final List<String> ENDLESS_BEATS = List.of(
        "The truffles here glow faintly.",
        "The forest has stopped breathing.",
        "Hoggart's eyes do not blink anymore.",
        "Something deep underground is listening.",
        "You feel the crown getting heavier.",
        "The wolves have learned your name.",
        "Bramble's shadow is too long for this hour."
    );

    /** Returns a lore beat at the given endless depth. Drops a line every 5 levels. */
    public static String endlessBeat(int endlessDepth) {
        if (endlessDepth <= 0 || endlessDepth % 5 != 0) return "";
        int idx = ((endlessDepth / 5) - 1) % ENDLESS_BEATS.size();
        return ENDLESS_BEATS.get(idx);
    }

    private static String pick(List<String> options) {
        return options.get(RNG.nextInt(options.size()));
    }
}
