package gamchu.pathfinder.goals

import kotlin.math.abs

/**
 * Goal for an exact (X, Y, Z) block position.
 * Heuristic uses octile 3D distance (admissible).
 */
class GoalBlock(
    @JvmField val targetX: Int,
    @JvmField val targetY: Int,
    @JvmField val targetZ: Int
) : Goal {

    override fun isAtGoal(x: Int, y: Int, z: Int): Boolean {
        return x == targetX && y == targetY && z == targetZ
    }

    override fun heuristic(x: Int, y: Int, z: Int): Double {
        val dx = abs(x - targetX)
        val dy = abs(y - targetY)
        val dz = abs(z - targetZ)
        // 3D octile distance
        // Sort: smallest, middle, largest
        var a = dx; var b = dy; var c = dz
        if (a > b) { val t = a; a = b; b = t }
        if (b > c) { val t = b; b = c; c = t }
        if (a > b) { val t = a; a = b; b = t }
        // a <= b <= c
        // Cost = a*(√3-√2) + b*(√2-1) + c
        return a * 0.31783724519578205 + b * 0.41421356237309515 + c.toDouble()
    }
}
