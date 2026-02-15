package gamchu.pathfinder.goals

/**
 * A pathfinding goal. All methods take raw int coordinates to avoid
 * object allocation in the A* hot loop.
 */
interface Goal {
    /** Returns true if (x, y, z) satisfies this goal. */
    fun isAtGoal(x: Int, y: Int, z: Int): Boolean

    /**
     * Admissible heuristic estimate of cost from (x, y, z) to the goal.
     * Must never overestimate the true cost.
     */
    fun heuristic(x: Int, y: Int, z: Int): Double
}
