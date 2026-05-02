package com.example.demo.core;

/**
 * Persistent meta-game achievements. Conditions are checked by
 * {@link AchievementTracker} during a run; once unlocked they stay unlocked
 * across sessions (stored in MetaProgression).
 */
public enum Achievement {
    FIRST_TRUFFLE         ("Sniff Test",         "Collect your first truffle"),
    HUNDRED_TRUFFLES      ("Connoisseur",        "Bank 100 truffles total"),
    THOUSAND_TRUFFLES     ("Truffle Tycoon",     "Bank 1000 truffles total"),
    REACH_LEVEL_5         ("Persistent",         "Reach level 5"),
    REACH_LEVEL_10        ("Iron Hoof",          "Reach level 10"),
    REACH_LEVEL_15        ("Legend",             "Reach level 15"),
    SURVIVE_WOLF          ("Lucky",              "Survive a wolf at <20 kg"),
    STUN_WOLF             ("Stunner",            "Stun a wolf with Super Pig"),
    ESCAPE_FARMER         ("Houdini",            "Escape from the farmer"),
    GOLDEN_TRUFFLE        ("Worth Its Weight",   "Eat a golden truffle"),
    CLEAN_ROUND           ("Untouched",          "Finish a level taking no hits"),
    FIRST_BOON            ("Charmed",            "Pick your first boon"),
    GLUTTON_PACIFIST_RUN  ("Lazy Vegan",         "Win a level with Glutton + Pacifist active"),
    DAILY_RUN             ("Daily Grind",        "Finish a daily run"),
    MAX_PERK              ("Maxed Out",          "Buy any perk to its max level");

    public final String displayName;
    public final String description;

    Achievement(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
