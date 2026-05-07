package com.example.demo.render;

import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Renders the sniff ability visual: two concentric green pulses cycling
 * outward from the player's tile plus drifting "scent" particles that fade
 * over time. On the rising edge of {@code sniffActive} a small particle burst
 * fires for tactile feedback.
 *
 * <p>Both rings repeat every {@link #CYCLE_TICKS} frames while sniff is active,
 * with the outer ring offset by half a cycle so the effect feels alive instead
 * of one static circle. Particles continue to tick (and fade) even after sniff
 * ends so the trail dies down naturally.
 */
public class SniffRenderer {

    private static final int    TILE       = GameMap.TILE_SIZE;
    private static final double MAX_RADIUS = 2.0 * TILE;
    private static final int    CYCLE_TICKS = 40;
    private static final int    PARTICLE_SPAWN_INTERVAL = 6;

    private final Circle innerRing;
    private final Circle outerRing;
    private final Group  ringLayer;
    private final Group  particleLayer;
    private final Group  group;

    private final List<ScentParticle> particles = new ArrayList<>();
    private final Random rng = new Random();

    private boolean prevSniffActive = false;
    private boolean active          = false;
    private int     innerTick       = 0;
    private int     outerTick       = -CYCLE_TICKS / 2;
    private int     spawnCounter    = 0;

    public SniffRenderer() {
        innerRing = makeRing();
        outerRing = makeRing();
        ringLayer = new Group(innerRing, outerRing);
        particleLayer = new Group();
        // Particles below rings so the rings outline the bloom.
        group = new Group(particleLayer, ringLayer);
    }

    public Group getGroup() {
        return group;
    }

    public void update(boolean sniffActive, int playerCol, int playerRow) {
        double cx = playerCol * TILE + TILE / 2.0;
        double cy = playerRow * TILE + TILE / 2.0;

        // Rising edge — start cycling, fire the activation burst.
        if (sniffActive && !prevSniffActive) {
            active = true;
            innerTick = 0;
            outerTick = -CYCLE_TICKS / 2;
            spawnCounter = 0;
            spawnInitialBurst(cx, cy);
        }
        // Falling edge — hide rings; particles keep fading.
        if (!sniffActive && prevSniffActive) {
            active = false;
            innerRing.setVisible(false);
            outerRing.setVisible(false);
        }
        prevSniffActive = sniffActive;

        if (active) {
            innerTick++;
            outerTick++;
            updateRing(innerRing, innerTick, cx, cy);
            updateRing(outerRing, outerTick, cx, cy);

            spawnCounter++;
            if (spawnCounter >= PARTICLE_SPAWN_INTERVAL) {
                spawnCounter = 0;
                spawnScentDrift(cx, cy);
            }
        }

        // Always advance particles so they fade after sniff ends.
        Iterator<ScentParticle> it = particles.iterator();
        while (it.hasNext()) {
            ScentParticle p = it.next();
            p.tick();
            if (p.dead()) {
                particleLayer.getChildren().remove(p.shape);
                it.remove();
            }
        }
    }

    private void updateRing(Circle ring, int tick, double cx, double cy) {
        if (tick < 0) {
            ring.setVisible(false);
            return;
        }
        double progress = (tick % CYCLE_TICKS) / (double) CYCLE_TICKS;
        ring.setCenterX(cx);
        ring.setCenterY(cy);
        ring.setRadius(MAX_RADIUS * progress);
        ring.setStroke(Color.rgb(200, 255, 200, (1.0 - progress) * 0.7));
        ring.setVisible(true);
    }

    private void spawnInitialBurst(double cx, double cy) {
        for (int i = 0; i < 14; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = 0.9 + rng.nextDouble() * 1.7;
            spawnParticle(cx, cy, Math.cos(a) * sp, Math.sin(a) * sp,
                          1.6 + rng.nextDouble() * 1.2, 28 + rng.nextInt(10));
        }
    }

    private void spawnScentDrift(double cx, double cy) {
        for (int i = 0; i < 2; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = 0.4 + rng.nextDouble() * 0.9;
            spawnParticle(cx, cy, Math.cos(a) * sp, Math.sin(a) * sp,
                          1.4 + rng.nextDouble(), 22 + rng.nextInt(12));
        }
    }

    private void spawnParticle(double x, double y, double vx, double vy,
                               double radius, int life) {
        Circle s = new Circle(x, y, radius);
        s.setFill(Color.rgb(180, 240, 200, 0.6));
        particles.add(new ScentParticle(s, x, y, vx, vy, life));
        particleLayer.getChildren().add(s);
    }

    private static Circle makeRing() {
        Circle c = new Circle(0, 0, 0);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(Color.rgb(200, 255, 200, 0.5));
        c.setStrokeWidth(2.0);
        c.setVisible(false);
        return c;
    }

    private static final class ScentParticle {
        final Circle shape;
        double x, y, vx, vy;
        int life;
        final int max;

        ScentParticle(Circle s, double x, double y, double vx, double vy, int life) {
            this.shape = s;
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.life = life;
            this.max = life;
        }

        void tick() {
            x += vx;
            y += vy;
            vx *= 0.95;
            vy *= 0.95;
            life--;
            shape.setCenterX(x);
            shape.setCenterY(y);
            shape.setOpacity(Math.max(0, (double) life / max));
        }

        boolean dead() {
            return life <= 0;
        }
    }
}
