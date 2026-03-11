package gamchu.pathfinder.api

import gamchu.pathfinder.PathResult
import gamchu.pathfinder.goals.Goal
import java.util.concurrent.CompletableFuture

interface IPathfinder {

    fun path(goal: Goal): PathRequest

    fun isPathing(): Boolean

    fun cancel()

    interface PathRequest {
        fun from(x: Int, y: Int, z: Int): PathRequest
        fun in3D(): PathRequest
        fun smoothed(): PathRequest
        fun maxNodes(limit: Int): PathRequest
        fun execute(): CompletableFuture<PathResult>
    }
}