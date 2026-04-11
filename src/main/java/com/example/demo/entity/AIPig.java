package com.example.demo.entity;

import com.example.demo.world.GameMap;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * An AI-controlled pig that delegates movement decisions to a {@link PigBehavior}.
 *
 * <p>The pig recomputes its desired direction every {@code recomputeInterval} ticks
 * and actually moves once every 15 ticks (approximately 4 moves per second at 60 ticks/s).
 */
public class AIPig extends Pig {

    /** Number of ticks that have elapsed since this pig was created. */
    private int tickCounter;

    /** Cached direction computed by the behaviour strategy. */
    private Direction currentDirection;

    /** Counter used to rate-limit actual movement to once every 15 ticks. */
    private int moveTickCounter;

    /** How often (in ticks) the behaviour is asked to recompute the direction. */
    private final int recomputeInterval;

    /** The strategy that decides where this pig moves. */
    private final PigBehavior behavior;

    /** Number of ticks between actual moves (default: 4 moves/sec at 60 ticks/sec). */
    private int moveInterval = 15;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new AI pig.
     *
     * @param name               display name
     * @param color              body colour
     * @param startCol           starting column
     * @param startRow           starting row
     * @param behavior           the movement strategy
     * @param recomputeInterval  how many ticks between direction re-evaluations
     */
    public AIPig(String name, Color color, int startCol, int startRow,
                 PigBehavior behavior, int recomputeInterval) {
        super(name, color, startCol, startRow);
        this.behavior           = behavior;
        this.recomputeInterval  = recomputeInterval;
        this.tickCounter        = 0;
        this.moveTickCounter    = 0;
        this.currentDirection   = Direction.NONE;
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    /**
     * Advances the pig by one game tick.
     *
     * <ul>
     *   <li>Increments {@code tickCounter} and {@code moveTickCounter}.</li>
     *   <li>Re-asks the behaviour for a new direction every {@code recomputeInterval} ticks.</li>
     *   <li>Attempts to move once every {@value #MOVE_INTERVAL} ticks.</li>
     * </ul>
     *
     * @param map     the current game map
     * @param allPigs all pigs in the game (including the player)
     */
    public void tick(GameMap map, List<Pig> allPigs) {
        if (stunned) return;
        tickCounter++;
        moveTickCounter++;

        // Recompute direction at the specified interval
        if (tickCounter % recomputeInterval == 0) {
            currentDirection = behavior.nextMove(this, map, allPigs);
        }

        // Effective move interval scaled by cell (mud pit) and external (weather) speed
        double cellMult = map.getCell(col, row).getSpeedMultiplier();
        int effectiveMoveInterval = (int) Math.ceil(moveInterval / (cellMult * externalSpeedMult));

        // Actually move once every effectiveMoveInterval ticks
        if (moveTickCounter >= effectiveMoveInterval) {
            moveTickCounter = 0;

            if (currentDirection != null && currentDirection != Direction.NONE) {
                int newCol = col + currentDirection.dc;
                int newRow = row + currentDirection.dr;
                if (canMove(newCol, newRow, map)) {
                    facing = currentDirection;
                    moveTo(newCol, newRow);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the cached direction computed by the last behaviour call.
     *
     * @return the current cached direction
     */
    public Direction getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Returns the behaviour strategy used by this pig.
     *
     * @return the pig's behaviour
     */
    public PigBehavior getBehavior() {
        return behavior;
    }

    /** Sets the base move interval in ticks (lower = faster AI). */
    public void setMoveInterval(int interval) {
        this.moveInterval = Math.max(4, interval);
    }
}
