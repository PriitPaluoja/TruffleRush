package com.example.demo.core;

import com.example.demo.entity.Wolf;
import com.example.demo.entity.Farmer;
import com.example.demo.world.GameMap;

import java.util.Random;

/**
 * Manages random event spawning during gameplay.
 * Events: Wolf Attack, Farmer Raid, Truffle Rain, Mud Storm, Pig Stampede, Frenzy Mode.
 */
public class RandomEventManager {

    private final Random rng = new Random();
    private final int level;
    private int eventCheckCounter;
    private int lastEventTick = -1000;

    private Wolf activeWolf;
    private Farmer activeFarmer;
    private boolean truffleRainActive;
    private int truffleRainTicks;
    private boolean mudStormActive;
    private int mudStormTicks;
    private boolean frenzyActive;
    private int frenzyTicks;
    private int stampedeCount;
    private int stampedeTicks;

    private static final int EVENT_CHECK_INTERVAL = 600;
    private static final int EVENT_COOLDOWN = 900;

    public RandomEventManager(int level) {
        this.level = level;
    }

    public void tick(GameMap map, com.example.demo.entity.PlayerPig player,
                     java.util.List<com.example.demo.entity.Pig> allPigs) {
        eventCheckCounter++;

        if (eventCheckCounter >= EVENT_CHECK_INTERVAL) {
            eventCheckCounter = 0;

            int baseChance = 30 + level * 5;
            int finalChance = Math.min(baseChance, 70);

            if (rng.nextInt(100) < finalChance) {
                int eventType = rng.nextInt(6);
                switch (eventType) {
                    case 0 -> {
                        if (level >= 2 && activeWolf == null) {
                            activeWolf = new Wolf(map);
                            lastEventTick = eventCheckCounter;
                        }
                    }
                    case 1 -> {
                        if (level >= 4 && activeFarmer == null) {
                            activeFarmer = new Farmer(map, player);
                            lastEventTick = eventCheckCounter;
                        }
                    }
                    case 2 -> {
                        if (!truffleRainActive) {
                            truffleRainActive = true;
                            truffleRainTicks = 300;
                            lastEventTick = eventCheckCounter;
                        }
                    }
                    case 3 -> {
                        if (level >= 3 && !mudStormActive) {
                            mudStormActive = true;
                            mudStormTicks = 240;
                            lastEventTick = eventCheckCounter;
                        }
                    }
                    case 4 -> {
                        if (level >= 5 && stampedeCount == 0) {
                            stampedeCount = 2;
                            stampedeTicks = 480;
                            lastEventTick = eventCheckCounter;
                        }
                    }
                    case 5 -> {
                        if (!frenzyActive) {
                            frenzyActive = true;
                            frenzyTicks = 300;
                            lastEventTick = eventCheckCounter;
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

        if (stampedeCount > 0) {
            stampedeTicks--;
            if (stampedeTicks <= 0) stampedeCount = 0;
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
    public int getStampedeCount() { return stampedeCount; }
    public int getTruffleRainTicks() { return truffleRainTicks; }
    public int getMudStormTicks() { return mudStormTicks; }
    public int getFrenzyTicks() { return frenzyTicks; }

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
