package com.example.demo.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Persistent meta-progression state: banked truffles + permanent perk levels.
 * Stored as flat key=value lines in ~/.trufflerush/meta.txt.
 */
public class MetaProgression {

    private static final String DIR_NAME  = ".trufflerush";
    private static final String FILE_NAME = "meta.txt";

    private int truffleBank;
    private int truffleLifetime;
    private long bestDailyDate;
    private int bestDailyScore;
    private final Map<Perk, Integer> perkLevels = new EnumMap<>(Perk.class);
    private final Set<Achievement> achievements = EnumSet.noneOf(Achievement.class);
    private final Set<BoonCombo> discoveredCombos = EnumSet.noneOf(BoonCombo.class);

    // Settings (B1)
    private double masterVolume = 1.0;
    private boolean shakeEnabled = true;
    private boolean hitStopEnabled = true;

    // Endless mode (S3)
    private boolean clearedTen;
    private int endlessBest;

    // Narrative (N3)
    private String pigName = "Truffles";

    // Heat ladder (S1)
    private int lastHeatPicked;
    private int bestHeatBeaten;

    public MetaProgression() {
        for (Perk p : Perk.values()) perkLevels.put(p, 0);
    }

    public int getTruffleBank() { return truffleBank; }
    public int getTruffleLifetime() { return truffleLifetime; }
    public int getPerkLevel(Perk p) { return perkLevels.getOrDefault(p, 0); }
    public long getBestDailyDate() { return bestDailyDate; }
    public int getBestDailyScore() { return bestDailyScore; }
    public Set<Achievement> getAchievements() { return achievements; }
    public boolean hasAchievement(Achievement a) { return achievements.contains(a); }

    public double getMasterVolume() { return masterVolume; }
    public void setMasterVolume(double v) { this.masterVolume = Math.max(0.0, Math.min(1.0, v)); }
    public boolean isShakeEnabled() { return shakeEnabled; }
    public void setShakeEnabled(boolean v) { this.shakeEnabled = v; }
    public boolean isHitStopEnabled() { return hitStopEnabled; }
    public void setHitStopEnabled(boolean v) { this.hitStopEnabled = v; }

    public boolean hasClearedTen() { return clearedTen; }
    public void setClearedTen(boolean v) { this.clearedTen = v; }
    public int getEndlessBest() { return endlessBest; }
    /** Records the deepest endless level reached if it improves on the prior best. */
    public void recordEndlessDepth(int depth) {
        if (depth > endlessBest) endlessBest = depth;
    }

    /** Records a new achievement. Returns true if this was a fresh unlock. */
    public boolean unlockAchievement(Achievement a) {
        return achievements.add(a);
    }

    public Set<BoonCombo> getDiscoveredCombos() { return discoveredCombos; }
    /** Records a freshly observed combo. Returns true if it was a first-time discovery. */
    public boolean discoverCombo(BoonCombo c) {
        return discoveredCombos.add(c);
    }
    public boolean hasDiscoveredAllCombos() {
        return discoveredCombos.size() == BoonCombo.values().length;
    }

    public String getPigName() { return pigName; }
    public void setPigName(String name) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        // Cap length to keep UI clean.
        this.pigName = trimmed.length() > 20 ? trimmed.substring(0, 20) : trimmed;
    }

    public int getLastHeatPicked() { return lastHeatPicked; }
    public void setLastHeatPicked(int v) { this.lastHeatPicked = Math.max(0, v); }
    public int getBestHeatBeaten() { return bestHeatBeaten; }
    public void recordHeatBeaten(int v) {
        if (v > bestHeatBeaten) bestHeatBeaten = v;
    }

    /** Adds banked truffles (called when a run ends). */
    public void addToBank(int amount) {
        if (amount > 0) {
            truffleBank += amount;
            truffleLifetime += amount;
        }
    }

    /** Records a daily-run score if it beats the prior one for the same date. */
    public void recordDailyScore(long epochDay, int score) {
        if (epochDay != bestDailyDate || score > bestDailyScore) {
            bestDailyDate = epochDay;
            bestDailyScore = score;
        }
    }

    /** Attempts to buy the next level of {@code perk}. Returns true on success. */
    public boolean tryBuy(Perk perk) {
        int current = getPerkLevel(perk);
        int cost = perk.costForLevel(current);
        if (cost < 0 || truffleBank < cost) return false;
        truffleBank -= cost;
        perkLevels.put(perk, current + 1);
        return true;
    }

    /** Resets all perks and refunds 70% of spent truffles. */
    public void respec() {
        int refund = 0;
        for (Perk p : Perk.values()) {
            int level = getPerkLevel(p);
            for (int l = 0; l < level; l++) refund += p.baseCost * (l + 1);
        }
        for (Perk p : Perk.values()) perkLevels.put(p, 0);
        truffleBank += (int) Math.round(refund * 0.7);
    }

    // --- Persistence ---------------------------------------------------------

    public static MetaProgression load() {
        MetaProgression m = new MetaProgression();
        Path file = filePath();
        if (!Files.exists(file)) return m;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            Map<String, String> kv = new HashMap<>();
            while ((line = br.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                kv.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
            m.truffleBank     = parseInt(kv.get("truffleBank"), 0);
            m.truffleLifetime = parseInt(kv.get("truffleLifetime"), m.truffleBank);
            m.bestDailyDate   = parseLong(kv.get("bestDailyDate"), 0L);
            m.bestDailyScore  = parseInt(kv.get("bestDailyScore"), 0);
            for (Perk p : Perk.values()) {
                int lvl = parseInt(kv.get("perk." + p.name()), 0);
                m.perkLevels.put(p, Math.min(lvl, p.maxLevel));
            }
            String achStr = kv.get("achievements");
            if (achStr != null && !achStr.isEmpty()) {
                for (String name : achStr.split(",")) {
                    try { m.achievements.add(Achievement.valueOf(name.trim())); }
                    catch (IllegalArgumentException ignore) {}
                }
            }
            m.masterVolume   = parseDouble(kv.get("masterVolume"), 1.0);
            m.shakeEnabled   = parseBoolean(kv.get("shakeEnabled"), true);
            m.hitStopEnabled = parseBoolean(kv.get("hitStopEnabled"), true);
            m.clearedTen     = parseBoolean(kv.get("clearedTen"), false);
            m.endlessBest    = parseInt(kv.get("endlessBest"), 0);
            String comboStr  = kv.get("discoveredCombos");
            if (comboStr != null && !comboStr.isEmpty()) {
                for (String name : comboStr.split(",")) {
                    try { m.discoveredCombos.add(BoonCombo.valueOf(name.trim())); }
                    catch (IllegalArgumentException ignore) {}
                }
            }
            String savedName = kv.get("pigName");
            if (savedName != null && !savedName.isEmpty()) m.setPigName(savedName);
            m.lastHeatPicked  = parseInt(kv.get("lastHeatPicked"), 0);
            m.bestHeatBeaten  = parseInt(kv.get("bestHeatBeaten"), 0);
        } catch (IOException ex) {
            // Corrupt file — ignore and start fresh.
        }
        return m;
    }

    public void save() {
        Path file = filePath();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                bw.write("truffleBank=" + truffleBank); bw.newLine();
                bw.write("truffleLifetime=" + truffleLifetime); bw.newLine();
                bw.write("bestDailyDate=" + bestDailyDate); bw.newLine();
                bw.write("bestDailyScore=" + bestDailyScore); bw.newLine();
                for (Perk p : Perk.values()) {
                    bw.write("perk." + p.name() + "=" + getPerkLevel(p));
                    bw.newLine();
                }
                StringBuilder achList = new StringBuilder();
                boolean first = true;
                for (Achievement a : achievements) {
                    if (!first) achList.append(',');
                    achList.append(a.name());
                    first = false;
                }
                bw.write("achievements=" + achList);
                bw.newLine();
                bw.write("masterVolume=" + masterVolume); bw.newLine();
                bw.write("shakeEnabled=" + shakeEnabled); bw.newLine();
                bw.write("hitStopEnabled=" + hitStopEnabled); bw.newLine();
                bw.write("clearedTen=" + clearedTen); bw.newLine();
                bw.write("endlessBest=" + endlessBest); bw.newLine();
                StringBuilder comboList = new StringBuilder();
                boolean firstC = true;
                for (BoonCombo c : discoveredCombos) {
                    if (!firstC) comboList.append(',');
                    comboList.append(c.name());
                    firstC = false;
                }
                bw.write("discoveredCombos=" + comboList);
                bw.newLine();
                bw.write("pigName=" + pigName);
                bw.newLine();
                bw.write("lastHeatPicked=" + lastHeatPicked); bw.newLine();
                bw.write("bestHeatBeaten=" + bestHeatBeaten); bw.newLine();
            }
        } catch (IOException ex) {
            // Disk full / permission denied — nothing we can do; progress is lost.
        }
    }

    private static Path filePath() {
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, DIR_NAME, FILE_NAME);
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return def; }
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s); } catch (NumberFormatException ex) { return def; }
    }

    private static double parseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return def; }
    }

    private static boolean parseBoolean(String s, boolean def) {
        if (s == null) return def;
        return Boolean.parseBoolean(s.trim());
    }
}
