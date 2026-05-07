package com.example.demo.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Hidden two-boon synergies. A combo is "active" whenever both of its boons are
 * present in {@link GameSession#getActiveBoons()}; the consuming code checks
 * {@link GameSession#hasComboActive(BoonCombo)} at the relevant hook sites
 * (item collection, decay, weight cap, etc.).
 *
 * <p>Combos are display-only metadata + a set membership test — the *effect* of
 * each combo is implemented inline at the corresponding game-loop hook in
 * {@code TruffleRushApp}. Keeping the effect adjacent to the existing boon
 * branches avoids a parallel hook surface.
 */
public enum BoonCombo {
    BLACK_TRUFFLE_BANQUET(
        "Black Truffle Banquet",
        "Truffles +50% extra value; acorns deal 3 weight damage",
        Boon.GLUTTON, Boon.TRUFFLE_HUNTER),
    SAINTLY_SNOUT(
        "Saintly Snout",
        "Mushrooms heal +5 extra weight",
        Boon.PACIFIST, Boon.GREEDY_HEART),
    WIND_WALKER(
        "Wind-Walker",
        "Sniff cooldown halved",
        Boon.SWIFT_HOOVES, Boon.SHARP_NOSE),
    GLASS_CANNON(
        "Glass Cannon",
        "+25% item value, decay doubled",
        Boon.SWIFT_HOOVES, Boon.TRUFFLE_HUNTER),
    IRON_BELLY(
        "Iron Belly",
        "Hazards do nothing",
        Boon.GLUTTON, Boon.GREEDY_HEART),
    GHOST_PIG(
        "Ghost Pig",
        "4-second speed boost after every hit",
        Boon.PACIFIST, Boon.SWIFT_HOOVES),
    HOARDER(
        "Hoarder",
        "+1 magnet range, +0.5 weight per item",
        Boon.GREEDY_HEART, Boon.SHARP_NOSE),
    TRUFFLE_KING_COMBO(
        "Truffle King's Crown",
        "Truffles grant +1 bonus weight",
        Boon.TRUFFLE_HUNTER, Boon.GREEDY_HEART);

    public final String displayName;
    public final String description;
    public final Boon a;
    public final Boon b;

    BoonCombo(String displayName, String description, Boon a, Boon b) {
        this.displayName = displayName;
        this.description = description;
        this.a = a;
        this.b = b;
    }

    public boolean isActive(List<Boon> activeBoons) {
        return activeBoons.contains(a) && activeBoons.contains(b);
    }

    /** All combos triggered by the current boon set. */
    public static Set<BoonCombo> activeFor(List<Boon> activeBoons) {
        EnumSet<BoonCombo> out = EnumSet.noneOf(BoonCombo.class);
        for (BoonCombo c : values()) {
            if (c.isActive(activeBoons)) out.add(c);
        }
        return out;
    }
}
