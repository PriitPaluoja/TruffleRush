package com.example.demo.world;

import javafx.scene.paint.Color;

import java.util.Random;

/**
 * State machine that drives weather transitions in TruffleRush.
 *
 * <p>Each weather state lasts a random number of ticks between
 * {@link Weather#MIN_DURATION} and {@link Weather#MAX_DURATION}.
 * When the timer expires a {@link #TRANSITION_TICKS}-tick crossfade
 * begins before the new weather takes full effect.</p>
 *
 * <p>Cycle order: SUNNY → OVERCAST → RAIN → FOG → SUNNY (repeating)</p>
 *
 * <p>Call {@link #tick()} once per game loop iteration (60 fps target).</p>
 */
public class WeatherSystem {

    /** Number of ticks the crossfade transition lasts (~3 seconds at 60 fps). */
    private static final int TRANSITION_TICKS = 180;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Weather currentWeather   = Weather.SUNNY;
    private Weather nextWeather;

    /** Ticks remaining before the next transition begins. */
    private int     ticksRemaining;

    /** 0.0 = start of transition, 1.0 = transition complete. */
    private double  transitionProgress;

    /** {@code true} while a crossfade is in progress. */
    private boolean transitioning;

    /** Tick counter used during the active transition. */
    private int     transitionTick;

    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public WeatherSystem() {
        ticksRemaining     = randomDuration();
        transitionProgress = 0.0;
        transitioning      = false;
        transitionTick     = 0;
        nextWeather        = currentWeather.next();
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    /**
     * Advances the weather state machine by one tick.
     * Should be called once per game-loop iteration.
     */
    public void tick() {
        if (transitioning) {
            transitionTick++;
            transitionProgress = Math.min(1.0, (double) transitionTick / TRANSITION_TICKS);

            if (transitionTick >= TRANSITION_TICKS) {
                // Transition complete – commit to new weather
                currentWeather     = nextWeather;
                transitioning      = false;
                transitionProgress = 0.0;
                transitionTick     = 0;
                ticksRemaining     = randomDuration();
                nextWeather        = currentWeather.next();
            }
        } else {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                // Begin transition to next weather
                transitioning      = true;
                transitionTick     = 0;
                transitionProgress = 0.0;
                // nextWeather was already set when the previous transition ended
            }
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns the weather that is currently active (or being transitioned from). */
    public Weather getCurrentWeather() {
        return currentWeather;
    }

    /**
     * Returns {@code true} while a crossfade between two weather states is
     * in progress.
     */
    public boolean isTransitioning() {
        return transitioning;
    }

    /**
     * Returns a value in [0.0, 1.0] representing how far through the current
     * transition we are.  Always {@code 0.0} when not transitioning.
     */
    public double getTransitionProgress() {
        return transitionProgress;
    }

    /**
     * Returns the interpolated tint colour for the current weather state.
     * During a transition the colour is lerped between the current and next
     * tint using JavaFX's {@link Color#interpolate(Color, double)}.
     */
    public Color getInterpolatedTintColor() {
        if (!transitioning) {
            return currentWeather.tintColor;
        }
        return currentWeather.tintColor.interpolate(nextWeather.tintColor, transitionProgress);
    }

    /**
     * Returns the effective movement-speed multiplier.
     * Interpolates linearly between current and next values during a transition.
     */
    public double getSpeedMultiplier() {
        if (!transitioning) {
            return currentWeather.speedMultiplier;
        }
        double cur  = currentWeather.speedMultiplier;
        double next = nextWeather.speedMultiplier;
        return cur + (next - cur) * transitionProgress;
    }

    /**
     * Returns the item spawn-rate multiplier for the current (or arriving)
     * weather.  Not interpolated – switches at the end of the transition.
     */
    public double getSpawnRateMultiplier() {
        return currentWeather.spawnRateMultiplier;
    }

    /**
     * Returns the visibility radius in cells.
     * {@code -1} means unlimited.
     * During a transition the arriving weather's radius is returned as soon
     * as the transition crosses 50 % so gameplay feedback is not too delayed.
     */
    public int getVisibilityRadius() {
        if (!transitioning) {
            return currentWeather.visibilityRadius;
        }
        return transitionProgress >= 0.5
                ? nextWeather.visibilityRadius
                : currentWeather.visibilityRadius;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns a random duration between MIN_DURATION and MAX_DURATION (inclusive). */
    private int randomDuration() {
        return Weather.MIN_DURATION
                + random.nextInt(Weather.MAX_DURATION - Weather.MIN_DURATION + 1);
    }
}
