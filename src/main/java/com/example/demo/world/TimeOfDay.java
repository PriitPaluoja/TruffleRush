package com.example.demo.world;

import javafx.scene.paint.Color;

/**
 * Represents the time-of-day cycle, divided into four phases.
 * Provides methods to determine the current phase and interpolate tile base colors.
 *
 * Progress is a value in [0.0, 1.0] representing how far through the full day cycle we are.
 *
 * Phase boundaries:
 *   DAWN  —  0% to 20%
 *   DAY   — 20% to 60%
 *   DUSK  — 60% to 80%
 *   NIGHT — 80% to 100%
 */
public final class TimeOfDay {

    /** Discrete phases of the day. */
    public enum Phase {
        DAWN, DAY, DUSK, NIGHT
    }

    // Representative colors for each phase
    private static final Color COLOR_DAWN  = Color.rgb(255, 200, 150);
    private static final Color COLOR_DAY   = Color.rgb(120, 180,  80);
    private static final Color COLOR_DUSK  = Color.rgb(200, 150,  80);
    private static final Color COLOR_NIGHT = Color.rgb( 40,  50,  70);

    // Phase boundary progress values
    private static final double DAWN_END  = 0.20;
    private static final double DAY_END   = 0.60;
    private static final double DUSK_END  = 0.80;

    private TimeOfDay() {
        // Utility class – no instances
    }

    /**
     * Returns the {@link Phase} that corresponds to the given progress value.
     *
     * @param progress a value in [0.0, 1.0]
     * @return the current phase
     */
    public static Phase getPhase(double progress) {
        if (progress < DAWN_END)  return Phase.DAWN;
        if (progress < DAY_END)   return Phase.DAY;
        if (progress < DUSK_END)  return Phase.DUSK;
        return Phase.NIGHT;
    }

    /**
     * Returns the tile base {@link Color} for the given progress by smoothly interpolating
     * between the four phase colors.
     *
     * @param progress a value in [0.0, 1.0]
     * @return the interpolated color
     */
    public static Color getTileBaseColor(double progress) {
        progress = Math.max(0.0, Math.min(1.0, progress));

        if (progress < DAWN_END) {
            // DAWN → DAY  (leading edge only — dawn to day blends at the end of dawn)
            double t = progress / DAWN_END;          // 0..1 within dawn
            return interpolate(COLOR_DAWN, COLOR_DAY, t);
        } else if (progress < DAY_END) {
            // DAY solid (center) — blend from dawn-end to dusk-start
            double t = (progress - DAWN_END) / (DAY_END - DAWN_END);
            return interpolate(COLOR_DAY, COLOR_DUSK, t);
        } else if (progress < DUSK_END) {
            // DUSK → NIGHT
            double t = (progress - DAY_END) / (DUSK_END - DAY_END);
            return interpolate(COLOR_DUSK, COLOR_NIGHT, t);
        } else {
            // NIGHT (holds until end, then wraps — treated as solid night)
            double t = (progress - DUSK_END) / (1.0 - DUSK_END);
            // Cycle back towards dawn color at the very end so wrapping is smooth
            return interpolate(COLOR_NIGHT, COLOR_DAWN, t);
        }
    }

    /** Linearly interpolates between two colors using JavaFX's built-in interpolator. */
    private static Color interpolate(Color a, Color b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return a.interpolate(b, t);
    }
}
