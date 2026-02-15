package gamchu.pathfinder

import net.minecraft.core.BlockPos

/**
 * Immutable result of a pathfinding computation.
 * The path list is materialized from packed longs only once, on construction.
 *
 * Kotlin properties auto-generate getters (getSuccess(), getPath(), etc.)
 * which Minescript can call through JavaClass reflection.
 */
class PathResult(
    /** Whether a path to the goal was found. */
    @JvmField val success: Boolean,
    /** Ordered list of block positions from start to goal. */
    @JvmField val path: List<BlockPos>,
    /** Number of nodes evaluated during A*. */
    @JvmField val nodesEvaluated: Int,
    /** Wall-clock time spent pathfinding, in milliseconds. */
    @JvmField val timeMs: Long
) {
    // --- Minescript-friendly accessors ---
    // Using explicit methods because @JvmField exposes raw fields.
    // Minescript's JavaClass can call these for clean access.

    fun getSuccess(): Boolean = success
    fun getPathSize(): Int = path.size
    fun getPathPoint(index: Int): BlockPos = path[index]
    fun getNodesEvaluated(): Int = nodesEvaluated
    fun getTimeMs(): Long = timeMs
    fun getPath(): List<BlockPos> = path

    companion object {
        /** Build a PathResult from the goal node by walking parent links. */
        @JvmStatic
        fun fromGoalNode(goalNode: PathNode, nodesEvaluated: Int, timeMs: Long): PathResult {
            val positions = mutableListOf<BlockPos>()
            var current: PathNode? = goalNode
            while (current != null) {
                positions.add(
                    BlockPos(
                        PackedPos.unpackX(current.packedPos),
                        PackedPos.unpackY(current.packedPos),
                        PackedPos.unpackZ(current.packedPos)
                    )
                )
                current = current.parent
            }
            positions.reverse()
            return PathResult(true, positions, nodesEvaluated, timeMs)
        }

        @JvmStatic
        fun failure(nodesEvaluated: Int, timeMs: Long): PathResult {
            return PathResult(false, emptyList(), nodesEvaluated, timeMs)
        }

        /**
         * Create a smoothed version of an existing PathResult.
         * Reduces waypoints by removing unnecessary intermediate nodes.
         *
         * Uses line-of-sight checks to skip waypoints when a straight path is possible,
         * creating more natural-looking movement with fewer direction changes.
         *
         * @param original The original PathResult from A*
         * @param world World access for collision checking during smoothing
         * @param allow3D If true, use 3D passability checks; if false, use ground walkability
         * @return New PathResult with smoothed path (or original if smoothing not applicable)
         */
        @JvmStatic
        fun smoothed(original: PathResult, world: WorldAccess, allow3D: Boolean): PathResult {
            if (!original.success || original.path.size <= 2) return original

            val smoothedPath = PathSmoother.smoothPath(original.path, world, allow3D)
            return PathResult(
                success = true,
                path = smoothedPath,
                nodesEvaluated = original.nodesEvaluated,
                timeMs = original.timeMs
            )
        }
    }
}