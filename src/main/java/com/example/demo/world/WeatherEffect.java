package com.example.demo.world;

/**
 * Hook interface for weather-specific side-effects on the game map.
 *
 * <p>Implementations can react to the three lifecycle events of a weather
 * state: entering, each tick while active, and exiting.  No implementations
 * are required in the current version; the interface exists as a design
 * extension point.</p>
 */
public interface WeatherEffect {

    /**
     * Called once when this weather state becomes active.
     *
     * @param map the current game map
     */
    void onEnter(GameMap map);

    /**
     * Called every game-loop tick while this weather state is active.
     *
     * @param map the current game map
     */
    void onTick(GameMap map);

    /**
     * Called once when this weather state ends and the next one takes over.
     *
     * @param map the current game map
     */
    void onExit(GameMap map);
}
