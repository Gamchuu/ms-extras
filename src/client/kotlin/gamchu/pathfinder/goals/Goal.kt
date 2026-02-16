package gamchu.pathfinder.goals


interface Goal {
    /** Returns true if (x, y, z) satisfies this goal. */
    fun isAtGoal(x: Int, y: Int, z: Int): Boolean

    fun heuristic(x: Int, y: Int, z: Int): Double
}
