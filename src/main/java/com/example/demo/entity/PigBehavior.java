package com.example.demo.entity;

import com.example.demo.world.GameMap;

import java.util.List;

/**
 * Strategy interface for AI pig movement behaviours.
 *
 * <p>Implementations decide which {@link Direction} an {@link AIPig} should
 * move next given the current game state.
 */
public interface PigBehavior {

    /**
     * Computes the next move for the given AI pig.
     *
     * @param self    the pig that is making the decision
     * @param map     the current game map
     * @param allPigs all pigs currently in the game (including the player)
     * @return the direction to move, or {@link Direction#NONE} to stay still
     */
    Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs);
}
