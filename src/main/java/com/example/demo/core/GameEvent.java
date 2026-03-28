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
    ROUND_ENDED
}
