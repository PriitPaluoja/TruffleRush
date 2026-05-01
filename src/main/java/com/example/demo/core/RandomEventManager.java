package com.example.demo.core;

import com.example.demo.entity.Wolf;
import com.example.demo.entity.Farmer;
import com.example.demo.world.GameMap;

import java.util.Random;

/**
 * Manages random event spawning during gameplay.
 * Events: Wolf Attack, Farmer Raid, Truffle Rain, Mud Storm, Frenzy Mode.
 */
public class RandomEventManager {

    private final Random rng;
    private final int level;
    private int eventCheckCounter;
    private int ticksSinceLastEvent = 0;
    /** GLUTTON boon: weight wolves twice as likely. */
    private boolean gluttonActive;
    /** PACIFIST boon: never spawn farmer. */
    private boolean farmerDisabled;

    private Wolf activeWolf;
    private Farmer activeFarmer;
    private boolean truffleRainActive;
    private int truffleRainTicks;
    private boolean mudStormActive;
    private int mudStormTicks;
    private boolean frenzyActive;
    private int frenzyTicks;

    private static final int EVENT_CHECK_INTERVAL = 600;
    private static final int EVENT_COOLDOWN = 300;

    public RandomEventManager(int level) {
        this(level, new Random());
    }

    public RandomEventManager(int level, Random rng) {
        this.level = level;
        this.rng = rng;
    }

    public void setGluttonActive(boolean v) { this.gluttonActive = v; }
    public void setFarmerDisabled(boolean v) { this.farmerDisabled = v; }

    /** Bumps the cooldown forward — used by risk zones to spawn wolves faster. */
    public void accelerateCooldown(int ticks) {
        ticksSinceLastEvent += ticks;
        eventCheckCounter += ticks;
    }

    public void tick(GameMap map, com.example.demo.entity.PlayerPig player,
                     java.util.List<com.example.demo.entity.Pig> allPigs) {
        eventCheckCounter++;
        ticksSinceLastEvent++;

        if (eventCheckCounter >= EVENT_CHECK_INTERVAL) {
            eventCheckCounter = 0;

            // Enforce cooldown between events
            if (ticksSinceLastEvent < EVENT_COOLDOWN) {
                // skip — too soon after last event
            } else {
                int baseChance = 30 + level * 5;
                int finalChance = Math.min(baseChance, 70);

                if (rng.nextInt(100) < finalChance) {
                    // GLUTTON: wolf weighted 2x (rolls 0,1 → wolf), shifting other types.
                    int eventType;
                    if (gluttonActive) {
                        int r = rng.nextInt(6);
                        eventType = r <= 1 ? 0 : r - 1; // map 0,1→wolf; 2→1; 3→2; 4→3; 5→4
                    } else {
                        eventType = rng.nextInt(5);
                    }
                    switch (eventType) {
                        case 0 -> {
                            if (level >= 2 && activeWolf == null) {
                                activeWolf = new Wolf(map, level);
                                ticksSinceLastEvent = 0;
                            }
                        }
                        case 1 -> {
                            if (!farmerDisabled && level >= 4 && activeFarmer == null) {
                                activeFarmer = new Farmer(map, player);
                                ticksSinceLastEvent = 0;
                            }
                        }
                        case 2 -> {
                            if (!truffleRainActive) {
                                truffleRainActive = true;
                                truffleRainTicks = 300;
                                ticksSinceLastEvent = 0;
                            }
                        }
                        case 3 -> {
                            if (level >= 3 && !mudStormActive) {
                                mudStormActive = true;
                                mudStormTicks = 240;
                                ticksSinceLastEvent = 0;
                            }
                        }
                        case 4 -> {
                            if (!frenzyActive) {
                                frenzyActive = true;
                                frenzyTicks = 300;
                                ticksSinceLastEvent = 0;
                            }
                        }
                    }
                }
            }
        }

        // Tick active events
        if (activeWolf != null && activeWolf.isActive()) {
            activeWolf.tick(map, allPigs, player);
        } else if (activeWolf != null && !activeWolf.isActive()) {
            // keep reference so caller can check hasCaughtPlayer()
        }

        if (activeFarmer != null && activeFarmer.isActive()) {
            activeFarmer.tick(map, player);
        } else if (activeFarmer != null) {
            activeFarmer = null;
        }

        if (truffleRainActive) {
            truffleRainTicks--;
            if (truffleRainTicks <= 0) truffleRainActive = false;
        }

        if (mudStormActive) {
            mudStormTicks--;
            if (mudStormTicks <= 0) mudStormActive = false;
        }

        if (frenzyActive) {
            frenzyTicks--;
            if (frenzyTicks <= 0) frenzyActive = false;
        }
    }

    public void clearWolf() { activeWolf = null; }
    public void clearFarmer() { activeFarmer = null; }

    // Getters
    public Wolf getActiveWolf() { return activeWolf; }
    public Farmer getActiveFarmer() { return activeFarmer; }
    public boolean isTruffleRainActive() { return truffleRainActive; }
    public boolean isMudStormActive() { return mudStormActive; }
    public boolean isFrenzyActive() { return frenzyActive; }

    /** Returns a short description of currently active events, or empty string. */
    public String getActiveEventLabel() {
        if (activeWolf != null && activeWolf.isActive()) return "WOLF ATTACK!";
        if (activeFarmer != null && activeFarmer.isActive()) return "FARMER RAID!";
        if (truffleRainActive) return "Truffle Rain (" + truffleRainTicks / 60 + "s)";
        if (mudStormActive)    return "Mud Storm (" + mudStormTicks / 60 + "s)";
        if (frenzyActive)      return "FRENZY! (" + frenzyTicks / 60 + "s)";
        return "";
    }
}
