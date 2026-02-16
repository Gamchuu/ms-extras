package gamchu.pathfinder.api

import gamchu.pathfinder.PathFinder
import gamchu.pathfinder.PathResult
import gamchu.pathfinder.WorldAccess
import gamchu.pathfinder.goals.Goal
import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PathfinderImpl : IPathfinder {

    /** Single-thread executor for pathfinding — A* is inherently sequential. */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MSExtras-Pathfinder").apply { isDaemon = true }
    }

    /** Currently running future, if any. */
    private val currentFuture = AtomicReference<CompletableFuture<PathResult>?>(null)

    /** Cancellation flag shared with the PathFinder. */
    private val cancelFlag = AtomicBoolean(false)

    // --- From player position (ground) ---

    override fun findPathAsync(goal: Goal): CompletableFuture<PathResult> {
        return findPathAsync(goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findPathAsync(goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        val (sx, sy, sz) = getPlayerPos() ?: return failedFuture()
        return runPathfinding(sx, sy, sz, goal, maxNodes, allow3D = false, smoothed = false)
    }

    // --- From custom start (ground) ---

    override fun findPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult> {
        return findPathAsync(startX, startY, startZ, goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        return runPathfinding(startX, startY, startZ, goal, maxNodes, allow3D = false, smoothed = false)
    }

    // --- From player position (3D) ---

    override fun findPathAsync3D(goal: Goal): CompletableFuture<PathResult> {
        return findPathAsync3D(goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findPathAsync3D(goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        val (sx, sy, sz) = getPlayerPos() ?: return failedFuture()
        return runPathfinding(sx, sy, sz, goal, maxNodes, allow3D = true, smoothed = false)
    }

    // --- From custom start (3D) ---

    override fun findPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult> {
        return findPathAsync3D(startX, startY, startZ, goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        return runPathfinding(startX, startY, startZ, goal, maxNodes, allow3D = true, smoothed = false)
    }

    // --- Smoothed paths (ground mode) ---

    override fun findSmoothedPathAsync(goal: Goal): CompletableFuture<PathResult> {
        return findSmoothedPathAsync(goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findSmoothedPathAsync(goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        val (sx, sy, sz) = getPlayerPos() ?: return failedFuture()
        return runPathfinding(sx, sy, sz, goal, maxNodes, allow3D = false, smoothed = true)
    }

    override fun findSmoothedPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult> {
        return findSmoothedPathAsync(startX, startY, startZ, goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findSmoothedPathAsync(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        return runPathfinding(startX, startY, startZ, goal, maxNodes, allow3D = false, smoothed = true)
    }

    // --- Smoothed paths (3D mode) ---

    override fun findSmoothedPathAsync3D(goal: Goal): CompletableFuture<PathResult> {
        return findSmoothedPathAsync3D(goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findSmoothedPathAsync3D(goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        val (sx, sy, sz) = getPlayerPos() ?: return failedFuture()
        return runPathfinding(sx, sy, sz, goal, maxNodes, allow3D = true, smoothed = true)
    }

    override fun findSmoothedPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal): CompletableFuture<PathResult> {
        return findSmoothedPathAsync3D(startX, startY, startZ, goal, PathFinder.DEFAULT_MAX_NODES)
    }

    override fun findSmoothedPathAsync3D(startX: Int, startY: Int, startZ: Int, goal: Goal, maxNodes: Int): CompletableFuture<PathResult> {
        return runPathfinding(startX, startY, startZ, goal, maxNodes, allow3D = true, smoothed = true)
    }

    // --- Control ---

    override fun isPathing(): Boolean {
        val f = currentFuture.get() ?: return false
        return !f.isDone
    }

    override fun cancel() {
        cancelFlag.set(true)
        val f = currentFuture.getAndSet(null)
        f?.cancel(false)
    }

    /** Shutdown the executor (call on mod unload). */
    fun shutdown() {
        cancel()
        executor.shutdownNow()
    }

    // --- Internal ---

    private fun runPathfinding(
        startX: Int, startY: Int, startZ: Int,
        goal: Goal, maxNodes: Int, allow3D: Boolean, smoothed: Boolean
    ): CompletableFuture<PathResult> {
        cancel()
        cancelFlag.set(false)

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return failedFuture()
        val worldAccess = WorldAccess(level)

        val future = CompletableFuture.supplyAsync({
            val pathFinder = PathFinder(worldAccess)
            val result = pathFinder.findPath(
                startX, startY, startZ, goal, maxNodes,
                allow3D = allow3D,
                cancelFlag = cancelFlag
            )

            // Apply smoothing if requested
            if (smoothed && result.success) {
                PathResult.smoothed(result, worldAccess, allow3D)
            } else {
                result
            }
        }, executor)

        currentFuture.set(future)
        future.whenComplete { _, _ -> currentFuture.compareAndSet(future, null) }
        return future
    }

    private fun getPlayerPos(): Triple<Int, Int, Int>? {
        val player = Minecraft.getInstance().player ?: return null
        val pos = player.blockPosition()
        return Triple(pos.x, pos.y, pos.z)
    }

    private fun failedFuture(): CompletableFuture<PathResult> {
        return CompletableFuture.completedFuture(PathResult.failure(0, 0))
    }
}