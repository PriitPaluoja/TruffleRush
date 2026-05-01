package com.example.demo.core;

/**
 * Run-modifying boons picked between levels. Each boon trades one upside for one downside.
 * Boons stack across levels — the player's run becomes more distinctive over time.
 */
public enum Boon {
    TRUFFLE_HUNTER ("Truffle Hunter",
                    "Truffles 2x value, acorns/mushrooms 0",
                    "Specialise: ignore the small stuff."),
    GLUTTON        ("Glutton",
                    "No weight decay, but 2x wolf chance",
                    "Eat freely, draw the wolves."),
    PACIFIST       ("Pacifist",
                    "Farmer never spawns, max weight capped at 120",
                    "Stay small, stay safe."),
    GREEDY_HEART   ("Greedy Heart",
                    "All items +50% value, hazards -50% effect",
                    "Risk reward without the risk."),
    SWIFT_HOOVES   ("Swift Hooves",
                    "Permanent +25% movement speed, decay +50%",
                    "Run hotter, burn hotter."),
    SHARP_NOSE     ("Sharp Nose",
                    "Sniff cooldown halved, magnet range -1",
                    "Trade reach for awareness.");

    public final String displayName;
    public final String shortDesc;
    public final String flavor;

    Boon(String displayName, String shortDesc, String flavor) {
        this.displayName = displayName;
        this.shortDesc   = shortDesc;
        this.flavor      = flavor;
    }
}
