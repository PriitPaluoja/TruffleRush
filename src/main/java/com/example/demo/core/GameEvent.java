package com.example.demo.core;

/**
 * Enumeration of game-wide events that can be published on the {@link EventBus}.
 *
 * <p>Listeners subscribe to a specific event and receive an {@code Object} payload
 * whose concrete type depends on the event:
 * <ul>
 *   <li>{@link #ITEM_COLLECTED}         — payload: the collected {@code Item}</li>
 *   <li>{@link #WEIGHT_CHANGED}         — payload: the updated weight as a {@code Double}</li>
 *   <li>{@link #GOLDEN_TRUFFLE_SPAWNED} — payload: the spawned {@code Item}</li>
 *   <li>{@link #ROUND_ENDED}            — payload: {@code null} or a round-result object</li>
 * </ul>
 */
public enum GameEvent {

    /** Fired when a pig collects an item from the map. */
    ITEM_COLLECTED,

    /** Fired whenever a pig's weight value changes. */
    WEIGHT_CHANGED,

    /** Fired when the rare golden truffle is placed on the map. */
    GOLDEN_TRUFFLE_SPAWNED,

    /** Fired when the current game round finishes. */
    ROUND_ENDED,

    /** Fired when the player's score changes. */
    SCORE_CHANGED,

    /** Fired when a level is completed successfully. */
    LEVEL_COMPLETE,

    /** Fired when the player loses (weight min, wolf, farmer, last place). */
    GAME_OVER,

    /** Fired when a random event starts. */
    RANDOM_EVENT_STARTED,

    /** Fired when a random event ends. */
    RANDOM_EVENT_ENDED
}
