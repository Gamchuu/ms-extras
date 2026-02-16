package gamchu.pathfinder

import gamchu.pathfinder.goals.Goal
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance A* pathfinder for the Minecraft client world.
 *
 * Supports two modes:
 * - **Ground mode** (allow3D=false): horizontal 8-dir + step up/down, requires solid ground
 * - **3D mode** (allow3D=true): full 26-directional movement through any passable block (flying/swimming)
 *
 * Optimizations:
 * - Long-packed coordinates (no BlockPos in hot loop)
 * - Custom binary heap with decrease-key
 * - HashMap<Long, PathNode> for O(1) visited lookups
 * - Pre-allocated neighbor offsets
 * - Chunk-cached world access
 * - Configurable maxNodes + timeout for guaranteed termination
 * - Cancellable via AtomicBoolean flag
 */
class PathFinder(private val world: WorldAccess) {

    companion object {
        const val DEFAULT_MAX_NODES = 100_000
        const val DEFAULT_TIMEOUT_MS = 10_000L

        private const val CARDINAL_COST = 1.0
        private const val DIAGONAL_2D_COST = 1.4142135623730951
        private const val DIAGONAL_3D_COST = 1.7320508075688772
        private const val VERTICAL_STEP_COST = 1.2


        private val HORIZONTAL_OFFSETS = arrayOf(
            intArrayOf( 1,  0),
            intArrayOf(-1,  0),
            intArrayOf( 0,  1),
            intArrayOf( 0, -1),
            intArrayOf( 1,  1),
            intArrayOf( 1, -1),
            intArrayOf(-1,  1),
            intArrayOf(-1, -1),
        )

        private val HORIZONTAL_COSTS = doubleArrayOf(
            CARDINAL_COST, CARDINAL_COST, CARDINAL_COST, CARDINAL_COST,
            DIAGONAL_2D_COST, DIAGONAL_2D_COST, DIAGONAL_2D_COST, DIAGONAL_2D_COST
        )

        private val OFFSETS_3D: Array<IntArray>
        private val COSTS_3D: DoubleArray

        init {
            val offsets = mutableListOf<IntArray>()
            val costs = mutableListOf<Double>()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        offsets.add(intArrayOf(dx, dy, dz))
                        val axis = (if (dx != 0) 1 else 0) + (if (dy != 0) 1 else 0) + (if (dz != 0) 1 else 0)
                        costs.add(when (axis) {
                            1 -> CARDINAL_COST
                            2 -> DIAGONAL_2D_COST
                            3 -> DIAGONAL_3D_COST
                            else -> CARDINAL_COST
                        })
                    }
                }
            }
            OFFSETS_3D = offsets.toTypedArray()
            COSTS_3D = costs.toDoubleArray()
        }
    }

    /**
     * Run A* from (startX, startY, startZ) toward the given [goal].
     *
     * @param allow3D if true, use full 26-directional 3D movement (flying/swimming);
     *                if false, use ground-based movement with step up/down
     * @param cancelFlag set to true from another thread to abort the search
     */
    fun findPath(
        startX: Int, startY: Int, startZ: Int,
        goal: Goal,
        maxNodes: Int = DEFAULT_MAX_NODES,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        allow3D: Boolean = false,
        cancelFlag: AtomicBoolean = AtomicBoolean(false)
    ): PathResult {
        val startTime = System.nanoTime()
        val deadlineNanos = startTime + timeoutMs * 1_000_000

        val startPacked = PackedPos.pack(startX, startY, startZ)
        val startH = goal.heuristic(startX, startY, startZ)
        val startNode = PathNode(startPacked, 0.0, startH)

        // Already at goal
        if (goal.isAtGoal(startX, startY, startZ)) {
            val elapsed = (System.nanoTime() - startTime) / 1_000_000
            return PathResult.fromGoalNode(startNode, 1, elapsed)
        }

        val openSet = BinaryHeapOpenSet(1024)
        val nodeMap = HashMap<Long, PathNode>(4096)

        openSet.insert(startNode)
        nodeMap[startPacked] = startNode

        var nodesEvaluated = 0

        while (!openSet.isEmpty() && nodesEvaluated < maxNodes) {
            if ((nodesEvaluated and 0xFF) == 0) {
                if (cancelFlag.get()) break
                if (System.nanoTime() > deadlineNanos) break
            }

            val current = openSet.poll()
            nodesEvaluated++

            val cx = PackedPos.unpackX(current.packedPos)
            val cy = PackedPos.unpackY(current.packedPos)
            val cz = PackedPos.unpackZ(current.packedPos)

            if (goal.isAtGoal(cx, cy, cz)) {
                val elapsed = (System.nanoTime() - startTime) / 1_000_000
                return PathResult.fromGoalNode(current, nodesEvaluated, elapsed)
            }

            if (allow3D) {
                for (i in OFFSETS_3D.indices) {
                    val o = OFFSETS_3D[i]
                    val nx = cx + o[0]
                    val ny = cy + o[1]
                    val nz = cz + o[2]
                    if (!world.isPassable(nx, ny, nz)) continue
                    if (!world.isPassable(nx, ny + 1, nz)) continue
                    processNeighbor(nx, ny, nz, current, COSTS_3D[i], goal, openSet, nodeMap)
                }
            } else {
                for (i in HORIZONTAL_OFFSETS.indices) {
                    val dx = HORIZONTAL_OFFSETS[i][0]
                    val dz = HORIZONTAL_OFFSETS[i][1]
                    val baseCost = HORIZONTAL_COSTS[i]
                    val nx = cx + dx
                    val nz = cz + dz

                    // Same-level walk
                    processNeighborGround(nx, cy, nz, current, baseCost, goal, openSet, nodeMap)

                    if (world.isPassable(cx, cy + 2, cz) && world.isWalkable(nx, cy + 1, nz)) {
                        processNeighborGround(nx, cy + 1, nz, current, baseCost + VERTICAL_STEP_COST, goal, openSet, nodeMap)
                    }

                    if (world.isWalkable(nx, cy - 1, nz)) {
                        processNeighborGround(nx, cy - 1, nz, current, baseCost + VERTICAL_STEP_COST, goal, openSet, nodeMap)
                    }
                }

                if (world.isWalkable(cx, cy - 1, cz)) {
                    processNeighborGround(cx, cy - 1, cz, current, VERTICAL_STEP_COST, goal, openSet, nodeMap)
                }
                if (world.isPassable(cx, cy + 2, cz) && world.isWalkable(cx, cy + 1, cz)) {
                    processNeighborGround(cx, cy + 1, cz, current, VERTICAL_STEP_COST, goal, openSet, nodeMap)
                }
            }
        }

        val elapsed = (System.nanoTime() - startTime) / 1_000_000
        return PathResult.failure(nodesEvaluated, elapsed)
    }

    private fun processNeighborGround(
        nx: Int, ny: Int, nz: Int,
        current: PathNode, moveCost: Double,
        goal: Goal, openSet: BinaryHeapOpenSet, nodeMap: HashMap<Long, PathNode>
    ) {
        if (!world.isWalkable(nx, ny, nz)) return
        addOrUpdateNode(nx, ny, nz, current, moveCost, goal, openSet, nodeMap)
    }

    private fun addOrUpdateNode(
        nx: Int, ny: Int, nz: Int,
        current: PathNode, moveCost: Double,
        goal: Goal, openSet: BinaryHeapOpenSet, nodeMap: HashMap<Long, PathNode>
    ) {
        val packedN = PackedPos.pack(nx, ny, nz)
        val newG = current.gCost + moveCost

        val existing = nodeMap[packedN]
        if (existing != null) {
            if (newG < existing.gCost) {
                existing.gCost = newG
                existing.fCost = newG + goal.heuristic(nx, ny, nz)
                existing.parent = current
                if (existing.heapIndex >= 0) {
                    openSet.decreaseKey(existing)
                } else {
                    openSet.insert(existing)
                }
            }
            return
        }

        val h = goal.heuristic(nx, ny, nz)
        val node = PathNode(packedN, newG, newG + h, current)
        nodeMap[packedN] = node
        openSet.insert(node)
    }

    private fun processNeighbor(
        nx: Int, ny: Int, nz: Int,
        current: PathNode, moveCost: Double,
        goal: Goal, openSet: BinaryHeapOpenSet, nodeMap: HashMap<Long, PathNode>
    ) {
        addOrUpdateNode(nx, ny, nz, current, moveCost, goal, openSet, nodeMap)
    }
}
