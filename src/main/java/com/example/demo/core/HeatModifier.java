package com.example.demo.core;

/**
 * Per-Heat-level difficulty modifiers (S1 ascension ladder). Heat is a number
 * 0..N; at Heat N, the first N modifiers in declaration order are active.
 *
 * <p>Each modifier toggles a single, clearly-explained difficulty knob the player
 * can read on the Shop's Heat panel. Effects are applied at level start by
 * the consuming sites in {@code TruffleRushApp}.
 */
public enum HeatModifier {
    LEAN_PICKINGS  ("Lean Pickings", "Items worth 15% less"),
    HUNGRY_PACK    ("Hungry Pack",   "Rivals move 15% faster"),
    NO_SAFETY_NET  ("No Safety Net", "Start with 45 kg instead of 50"),
    SHORTER_FUSE   ("Shorter Fuse",  "Level timer 10% shorter"),
    CROWDED_FIELD  ("Crowded Field", "20% more obstacles"),
    DRAINED_BANK   ("Drained Bank",  "Lucky Acorn perk disabled");

    public final String displayName;
    public final String description;

    HeatModifier(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public static int maxHeat() { return values().length; }

    /** Modifier {@code i} is active iff {@code i < heatLevel}. */
    public static boolean isActive(int heatLevel, HeatModifier m) {
        for (int i = 0; i < heatLevel && i < values().length; i++) {
            if (values()[i] == m) return true;
        }
        return false;
    }
}
