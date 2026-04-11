package com.example.demo.item;

import com.example.demo.world.GameMap;

import java.util.Random;

/**
 * Manages spawning of the Super Acorn power-up.
 * Spawns once every other level (3, 5, 7...) at a random tick in the middle 40% of the round.
 */
public class SuperAcornManager {

    private static final int DESPAWN_TICKS = 600;
    private static final int MAX_SPAWN_ATTEMPTS = 50;

    private Item superAcorn;
    private boolean spawned;
    private final int spawnTick;
    private int despawnCountdown;
    private final boolean enabled;
    private final Random random = new Random();

    public SuperAcornManager(int roundTicks, int level) {
        // Enabled on odd levels >= 3
        this.enabled = level >= 3 && level % 2 == 1;
        if (enabled) {
            double start = roundTicks * 0.3;
            double window = roundTicks * 0.4;
            this.spawnTick = (int) (start + random.nextDouble() * window);
        } else {
            this.spawnTick = Integer.MAX_VALUE;
        }
        this.spawned = false;
    }

    public Item tick(int currentTick, GameMap map) {
        if (!enabled || spawned) {
            // Handle despawn
            if (superAcorn != null && !superAcorn.isCollected()) {
                despawnCountdown--;
                if (despawnCountdown <= 0) {
                    superAcorn.collect();
                    superAcorn = null;
                }
            }
            if (superAcorn != null && superAcorn.isCollected()) {
                superAcorn = null;
            }
            return null;
        }

        if (currentTick >= spawnTick) {
            spawned = true;
            int cols = map.getColumns();
            int rows = map.getRows();
            for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
                int col = random.nextInt(cols);
                int row = random.nextInt(rows);
                if (map.isPassable(col, row)) {
                    superAcorn = new Item(ItemType.SUPER_ACORN, col, row);
                    despawnCountdown = DESPAWN_TICKS;
                    return superAcorn;
                }
            }
        }
        return null;
    }

    public Item getSuperAcorn() { return superAcorn; }

    public void onCollected() {
        if (superAcorn != null) {
            superAcorn.collect();
            superAcorn = null;
        }
    }
}
