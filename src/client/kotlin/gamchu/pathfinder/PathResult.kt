package gamchu.pathfinder

import net.minecraft.core.BlockPos

class PathResult(
    @JvmField val success: Boolean,
    @JvmField val path: List<BlockPos>,
    @JvmField val nodesEvaluated: Int,
    @JvmField val timeMs: Long
) {

    fun getSuccess(): Boolean = success
    fun getPathSize(): Int = path.size
    fun getPathPoint(index: Int): BlockPos = path[index]
    fun getNodesEvaluated(): Int = nodesEvaluated
    fun getTimeMs(): Long = timeMs
    fun getPath(): List<BlockPos> = path

    companion object {
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