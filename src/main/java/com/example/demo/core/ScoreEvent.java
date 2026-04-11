package com.example.demo.core;

/**
 * Point values awarded for various in-game actions.
 */
public enum ScoreEvent {

    ACORN_COLLECTED(10),
    MUSHROOM_COLLECTED(30),
    BLACK_TRUFFLE_COLLECTED(80),
    WHITE_TRUFFLE_COLLECTED(150),
    GOLDEN_TRUFFLE_COLLECTED(300),
    WOLF_STUNNED(200),
    FARMER_ESCAPED(500),
    LEVEL_COMPLETE(500),       // multiplied by level number
    WEIGHT_BONUS(5),           // multiplied by floor(finalWeight)
    SURVIVAL_TICK(2);          // awarded every 60 ticks (1 second)

    public final int points;

    ScoreEvent(int points) {
        this.points = points;
    }
}
