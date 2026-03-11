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

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MSExtras-Pathfinder").apply { isDaemon = true }
    }

    private val currentFuture = AtomicReference<CompletableFuture<PathResult>?>(null)
    private val cancelFlag = AtomicBoolean(false)

    override fun path(goal: Goal): IPathfinder.PathRequest = PathRequestImpl(goal)

    override fun isPathing(): Boolean {
        val f = currentFuture.get() ?: return false
        return !f.isDone
    }

    override fun cancel() {
        cancelFlag.set(true)
        val f = currentFuture.getAndSet(null)
        f?.cancel(false)
    }

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

    private fun failedFuture(): CompletableFuture<PathResult> =
        CompletableFuture.completedFuture(PathResult.failure(0, 0))

    // --- Builder ---

    private inner class PathRequestImpl(
        private val goal: Goal,
        private var startX: Int? = null,
        private var startY: Int? = null,
        private var startZ: Int? = null,
        private var allow3D: Boolean = false,
        private var useSmoothing: Boolean = false,
        private var nodeLimit: Int = PathFinder.DEFAULT_MAX_NODES
    ) : IPathfinder.PathRequest {

        override fun from(x: Int, y: Int, z: Int): IPathfinder.PathRequest {
            startX = x; startY = y; startZ = z
            return this
        }

        override fun in3D(): IPathfinder.PathRequest {
            allow3D = true
            return this
        }

        override fun smoothed(): IPathfinder.PathRequest {
            useSmoothing = true
            return this
        }

        override fun maxNodes(limit: Int): IPathfinder.PathRequest {
            nodeLimit = limit
            return this
        }

        override fun execute(): CompletableFuture<PathResult> {
            val (sx, sy, sz) = if (startX != null) {
                Triple(startX!!, startY!!, startZ!!)
            } else {
                getPlayerPos() ?: return failedFuture()
            }
            return runPathfinding(sx, sy, sz, goal, nodeLimit, allow3D, useSmoothing)
        }
    }
}