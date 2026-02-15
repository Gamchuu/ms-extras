package gamchu.pathfinder.goals

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Goal that matches any Y-level at a specific (X, Z) coordinate.
 * Heuristic uses Chebyshev distance (admissible for 8-directional movement).
 */
class GoalXZ(
    @JvmField val targetX: Int,
    @JvmField val targetZ: Int
) : Goal {

    override fun isAtGoal(x: Int, y: Int, z: Int): Boolean {
        return x == targetX && z == targetZ
    }

    override fun heuristic(x: Int, y: Int, z: Int): Double {
        val dx = abs(x - targetX)
        val dz = abs(z - targetZ)
        // Chebyshev with diagonal cost √2: min(dx,dz)*√2 + |dx-dz|
        val minD = if (dx < dz) dx else dz
        val maxD = if (dx > dz) dx else dz
        return minD * 1.4142135623730951 + (maxD - minD).toDouble()
    }
}
