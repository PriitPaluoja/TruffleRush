package com.example.demo.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal publish/subscribe event bus scoped to a single game session.
 *
 * <p>Components subscribe to a {@link GameEvent} and receive a typed
 * {@code Object} payload whenever that event is published.
 *
 * <pre>
 *   EventBus bus = new EventBus();
 *
 *   // Subscribe
 *   bus.subscribe(GameEvent.ITEM_COLLECTED, data -> {
 *       Item item = (Item) data;
 *       System.out.println("Collected: " + item.getType());
 *   });
 *
 *   // Publish
 *   bus.publish(GameEvent.ITEM_COLLECTED, collectedItem);
 * </pre>
 *
 * <p>All listeners are invoked synchronously on the calling thread.
 * This class is <em>not</em> thread-safe; use it on the JavaFX Application
 * Thread only.
 */
public class EventBus {

    private final Map<GameEvent, List<Consumer<Object>>> listeners =
            new EnumMap<>(GameEvent.class);

    /**
     * Registers {@code listener} to be called whenever {@code event} is published.
     *
     * @param event    the event to listen for
     * @param listener the callback; receives the payload passed to {@link #publish}
     */
    public void subscribe(GameEvent event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Notifies all subscribers registered for {@code event}.
     *
     * @param event the event that occurred
     * @param data  arbitrary payload; may be {@code null}
     */
    public void publish(GameEvent event, Object data) {
        List<Consumer<Object>> subs = listeners.get(event);
        if (subs == null) {
            return;
        }
        // Iterate over a snapshot to allow listeners to subscribe/unsubscribe during delivery
        for (Consumer<Object> sub : new ArrayList<>(subs)) {
            sub.accept(data);
        }
    }
}
