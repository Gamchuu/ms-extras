package gamchu.pathfinder

import net.minecraft.core.BlockPos
import kotlin.math.abs
import kotlin.math.sqrt

object PathSmoother {

    /**
     * @param path Original path from A* (must have at least 2 points)
     * @param world World access for collision checks
     * @param allow3D If true, use 3D passability checks; if false, use ground walkability
     * @return Smoothed path with fewer waypoints (never empty if input wasn't empty)
     */
    @JvmStatic
    fun smoothPath(path: List<BlockPos>, world: WorldAccess, allow3D: Boolean): List<BlockPos> {
        if (path.size <= 2) return path

        var smoothed = smoothPassLineOfSight(path, world, allow3D)

        smoothed = cleanAndStraighten(smoothed, world, allow3D)

        smoothed = smoothDiagonalPaths(smoothed, world, allow3D)

        smoothed = validateAndFixPath(smoothed, path, world, allow3D)

        return smoothed
    }

    private fun smoothPassLineOfSight(path: List<BlockPos>, world: WorldAccess, allow3D: Boolean): List<BlockPos> {
        if (path.size <= 2) return path

        val smoothed = mutableListOf<BlockPos>()
        smoothed.add(path.first())

        var i = 0
        while (i < path.size - 1) {
            val current = path[i]
            val maxSkip = minOf(12, path.size - 1 - i)
            var furthest = i + 1

            for (skipDist in maxSkip downTo 2) {
                val testIndex = i + skipDist
                val testPos = path[testIndex]

                if (!allow3D) {
                    var maxElevChange = 0
                    for (j in i + 1..testIndex) {
                        val elev = abs(path[j].y - path[j - 1].y)
                        if (elev > maxElevChange) maxElevChange = elev
                    }
                    if (maxElevChange >= 2) continue
                }

                if (hasLineOfSightLenient(current, testPos, world, allow3D)) {
                    furthest = testIndex
                    break
                }
            }

            smoothed.add(path[furthest])
            i = furthest
        }

        return smoothed
    }

    private fun cleanAndStraighten(path: List<BlockPos>, world: WorldAccess, allow3D: Boolean): List<BlockPos> {
        if (path.size <= 3) return path

        val cleaned = mutableListOf<BlockPos>()
        cleaned.add(path[0])

        var i = 1
        while (i < path.size - 1) {
            val prev = cleaned.last()
            val current = path[i]
            val next = path[i + 1]

            if (isZigzag(prev, current, next) && isWalkable(prev, next, world, allow3D)) {
                i++
                continue
            }

            if (i + 2 < path.size) {
                val next2 = path[i + 2]
                if (isZigzag4Node(prev, current, next, next2) && isWalkable(prev, next2, world, allow3D)) {
                    i += 2
                    continue
                }
            }

            if (isAlmostStraight(prev, current, next) && isWalkable(prev, next, world, allow3D)) {
                i++
                continue
            }

            cleaned.add(current)
            i++
        }

        cleaned.add(path.last())
        return cleaned
    }

    private fun isZigzag(prev: BlockPos, current: BlockPos, next: BlockPos): Boolean {
        if (prev.y != current.y || current.y != next.y) return false

        val dir1X = current.x - prev.x
        val dir1Z = current.z - prev.z
        val dir2X = next.x - current.x
        val dir2Z = next.z - current.z

        val reversedX = dir1X != 0 && dir2X == -dir1X
        val reversedZ = dir1Z != 0 && dir2Z == -dir1Z

        if (reversedX || reversedZ) {
            val directDist = sqrt(
                ((next.x - prev.x) * (next.x - prev.x) +
                        (next.z - prev.z) * (next.z - prev.z)).toDouble()
            )
            return directDist <= 3.0
        }

        val isOpposite = (dir1X == -dir2X && dir1Z == 0 && dir2Z == 0) ||
                (dir1Z == -dir2Z && dir1X == 0 && dir2X == 0)
        if (isOpposite) {
            val d1 = sqrt((dir1X * dir1X + dir1Z * dir1Z).toDouble())
            val d2 = sqrt((dir2X * dir2X + dir2Z * dir2Z).toDouble())
            return d1 <= 3.0 && d2 <= 3.0
        }

        return false
    }

    private fun isZigzag4Node(p1: BlockPos, p2: BlockPos, p3: BlockPos, p4: BlockPos): Boolean {
        if (p1.y != p2.y || p2.y != p3.y || p3.y != p4.y) return false

        val dxTotal = abs(p4.x - p1.x)
        val dzTotal = abs(p4.z - p1.z)

        if (dxTotal + dzTotal < 3) return false

        val lenWiggle = p1.distManhattan(p2) + p2.distManhattan(p3) + p3.distManhattan(p4)
        val lenDirect = p1.distManhattan(p4)

        return lenWiggle > lenDirect + 1
    }

    private fun isAlmostStraight(p1: BlockPos, p2: BlockPos, p3: BlockPos): Boolean {
        val dx1 = p2.x - p1.x
        val dz1 = p2.z - p1.z
        val dx2 = p3.x - p1.x
        val dz2 = p3.z - p1.z
        return abs(dx1 * dz2 - dz1 * dx2) <= 1
    }

    private fun smoothDiagonalPaths(path: List<BlockPos>, world: WorldAccess, allow3D: Boolean): List<BlockPos> {
        if (path.size <= 3) return path

        val smoothed = mutableListOf<BlockPos>()
        smoothed.add(path[0])

        var i = 1
        while (i < path.size - 1) {
            val prev = smoothed.last()
            val current = path[i]
            val next = path[i + 1]

            val dx1 = current.x - prev.x
            val dz1 = current.z - prev.z
            val dx2 = next.x - current.x
            val dz2 = next.z - current.z

            val isPerpendicular = (
                    (dx1 != 0 && dz1 == 0 && dx2 == 0 && dz2 != 0) ||
                            (dx1 == 0 && dz1 != 0 && dx2 != 0 && dz2 == 0)
                    )

            if (isPerpendicular && prev.y == current.y && current.y == next.y) {
                val diagonal = BlockPos(prev.x + dx1 + dx2, prev.y, prev.z + dz1 + dz2)
                if (isPositionWalkable(diagonal, world, allow3D) &&
                    isWalkable(prev, diagonal, world, allow3D) &&
                    isWalkable(diagonal, next, world, allow3D)
                ) {
                    smoothed.add(diagonal)
                    i += 2
                    continue
                }
            }

            smoothed.add(current)
            i++
        }

        smoothed.add(path.last())
        return smoothed
    }

    private fun validateAndFixPath(
        path: List<BlockPos>,
        originalPath: List<BlockPos>,
        world: WorldAccess,
        allow3D: Boolean
    ): List<BlockPos> {
        if (path.size <= 1) return path

        val validated = mutableListOf<BlockPos>()
        validated.add(path[0])

        for (i in 1 until path.size) {
            val from = validated.last()
            val to = path[i]

            if (isWalkable(from, to, world, allow3D)) {
                validated.add(to)
            } else {
                val intermediate = findWalkablePathBetween(from, to, originalPath, world, allow3D)
                if (intermediate.isNotEmpty()) {
                    validated.addAll(intermediate)
                } else {
                    validated.add(to)
                }
            }
        }

        return validated
    }

    private fun findWalkablePathBetween(
        from: BlockPos,
        to: BlockPos,
        originalPath: List<BlockPos>,
        world: WorldAccess,
        allow3D: Boolean
    ): List<BlockPos> {
        val result = mutableListOf<BlockPos>()

        var fromIdx = -1
        var toIdx = -1
        for (i in originalPath.indices) {
            if (originalPath[i] == from) fromIdx = i
            if (originalPath[i] == to) toIdx = i
        }

        if (fromIdx >= 0 && toIdx >= 0 && toIdx > fromIdx) {
            for (i in (fromIdx + 1) until toIdx) {
                result.add(originalPath[i])
            }
        }

        result.add(to)
        return result
    }

    private fun hasLineOfSightLenient(from: BlockPos, to: BlockPos, world: WorldAccess, allow3D: Boolean): Boolean {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z

        val steps = maxOf(abs(dx), abs(dy), abs(dz))
        if (steps == 0) return true

        val stepX = dx.toDouble() / steps
        val stepY = dy.toDouble() / steps
        val stepZ = dz.toDouble() / steps

        var x = from.x.toDouble()
        var y = from.y.toDouble()
        var z = from.z.toDouble()

        for (step in 0..steps) {
            val blockX = kotlin.math.round(x).toInt()
            val blockY = kotlin.math.round(y).toInt()
            val blockZ = kotlin.math.round(z).toInt()

            if (!isPositionWalkable(BlockPos(blockX, blockY, blockZ), world, allow3D)) {
                return false
            }

            x += stepX
            y += stepY
            z += stepZ
        }

        return true
    }

    private fun isPositionWalkable(pos: BlockPos, world: WorldAccess, allow3D: Boolean): Boolean {
        return if (allow3D) {
            world.isPassable(pos.x, pos.y, pos.z) && world.isPassable(pos.x, pos.y + 1, pos.z)
        } else {
            world.isWalkable(pos.x, pos.y, pos.z)
        }
    }

    private fun isWalkable(from: BlockPos, to: BlockPos, world: WorldAccess, allow3D: Boolean): Boolean {
        // Check both positions are walkable
        if (!isPositionWalkable(from, world, allow3D) || !isPositionWalkable(to, world, allow3D)) {
            return false
        }

        val dx = abs(to.x - from.x)
        val dy = abs(to.y - from.y)
        val dz = abs(to.z - from.z)

        if (dx <= 1 && dy <= 1 && dz <= 1) {
            return true
        }

        return hasLineOfSightLenient(from, to, world, allow3D)
    }
}