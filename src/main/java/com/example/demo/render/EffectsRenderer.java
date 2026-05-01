package com.example.demo.render;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Lightweight particle system + screen-shake + hit-stop manager.
 * Owned by the game loop; tick() advances all live effects each frame.
 *
 * <p>All shapes are JavaFX primitives (no images). Particles are fading circles
 * that drift outward from a spawn point.
 */
public class EffectsRenderer {

    private final Group group = new Group();
    private final Group particleLayer = new Group();
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    /** Remaining shake ticks (0 = no shake). */
    private int shakeTicks;
    private double shakeIntensity;

    /** Remaining hit-stop ticks. While > 0, the game loop should skip simulation. */
    private int hitStopTicks;

    public EffectsRenderer() {
        group.getChildren().add(particleLayer);
    }

    public Group getGroup() { return group; }

    /** Spawn a radial burst of {@code count} particles at the given grid position. */
    public void spawnBurst(int col, int row, Color color, int count) {
        double cx = col * 40 + 20;
        double cy = row * 40 + 20;
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double speed = 1.5 + rng.nextDouble() * 2.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double radius = 2 + rng.nextDouble() * 3;
            int life = 18 + rng.nextInt(10);
            Particle p = new Particle(cx, cy, vx, vy, radius, color, life);
            particles.add(p);
            particleLayer.getChildren().add(p.shape);
        }
    }

    /** Trigger a screen shake. {@code intensity} ≈ pixel amplitude. */
    public void shake(int ticks, double intensity) {
        if (ticks > shakeTicks) {
            shakeTicks = ticks;
            shakeIntensity = intensity;
        }
    }

    /** Pause simulation for {@code ticks} frames (used on big events). */
    public void hitStop(int ticks) {
        if (ticks > hitStopTicks) hitStopTicks = ticks;
    }

    /** True if the game loop should freeze this frame. Decrements the counter. */
    public boolean consumeHitStop() {
        if (hitStopTicks > 0) {
            hitStopTicks--;
            return true;
        }
        return false;
    }

    /** Returns current X shake offset. */
    public double getShakeX() {
        if (shakeTicks <= 0) return 0;
        return (rng.nextDouble() - 0.5) * 2 * shakeIntensity;
    }

    /** Returns current Y shake offset. */
    public double getShakeY() {
        if (shakeTicks <= 0) return 0;
        return (rng.nextDouble() - 0.5) * 2 * shakeIntensity;
    }

    /** Advances particles, fades them, removes dead ones. Decrements shake counter. */
    public void tick() {
        if (shakeTicks > 0) shakeTicks--;
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.tick();
            if (p.dead()) {
                particleLayer.getChildren().remove(p.shape);
                it.remove();
            }
        }
    }

    private static final class Particle {
        final Circle shape;
        double x, y, vx, vy;
        int life;
        final int maxLife;

        Particle(double x, double y, double vx, double vy, double radius, Color color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.shape = new Circle(x, y, radius);
            this.shape.setFill(color);
        }

        void tick() {
            x += vx;
            y += vy;
            vx *= 0.92;
            vy *= 0.92;
            life--;
            shape.setCenterX(x);
            shape.setCenterY(y);
            shape.setOpacity(Math.max(0, (double) life / maxLife));
        }

        boolean dead() {
            return life <= 0;
        }
    }
}
