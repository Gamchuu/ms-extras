package gamchu.pathfinder.api

import gamchu.pathfinder.PathResult
import gamchu.pathfinder.goals.Goal
import java.util.concurrent.CompletableFuture

interface IPathfinder {

    /** Pathfind from the player's current position (ground mode). */
    fun findPathAsync(goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from the player's current position with custom node limit. */
    fun findPathAsync(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    // --- From custom start position ---

    /** Pathfind from a custom start position (ground mode). */
    fun findPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from a custom start position with custom node limit. */
    fun findPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    // --- 3D mode (from player) ---

    /** Pathfind from the player's position in full 3D mode (flying/swimming). */
    fun findPathAsync3D(goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from the player's position in 3D mode with custom node limit. */
    fun findPathAsync3D(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    // --- 3D mode (from custom start) ---

    /** Pathfind from a custom start position in full 3D mode. */
    fun findPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from a custom start position in 3D mode with custom node limit. */
    fun findPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    // --- Smoothed paths (ground mode) ---

    /** Pathfind from the player's position and return a smoothed path (ground mode). */
    fun findSmoothedPathAsync(goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from the player's position and return a smoothed path with custom node limit. */
    fun findSmoothedPathAsync(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    /** Pathfind from a custom start position and return a smoothed path (ground mode). */
    fun findSmoothedPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from a custom start position and return a smoothed path with custom node limit. */
    fun findSmoothedPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    // --- Smoothed paths (3D mode) ---

    /** Pathfind from the player's position and return a smoothed path in 3D mode. */
    fun findSmoothedPathAsync3D(goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from the player's position and return a smoothed path in 3D mode with custom node limit. */
    fun findSmoothedPathAsync3D(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    /** Pathfind from a custom start position and return a smoothed path in 3D mode. */
    fun findSmoothedPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult>

    /** Pathfind from a custom start position and return a smoothed path in 3D mode with custom node limit. */
    fun findSmoothedPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

    // --- Control ---

    /** Returns true if a pathfinding computation is currently running. */
    fun isPathing(): Boolean

    /** Cancel the currently running pathfinding computation, if any. */
    fun cancel()
}