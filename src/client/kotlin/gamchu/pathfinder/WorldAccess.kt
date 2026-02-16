package gamchu.pathfinder

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk

class WorldAccess(private val level: ClientLevel) {

    @Volatile private var cachedChunkX = Int.MAX_VALUE
    @Volatile private var cachedChunkZ = Int.MAX_VALUE
    @Volatile private var cachedChunk: LevelChunk? = null

    fun getBlockState(x: Int, y: Int, z: Int): BlockState? {
        if (y < level.minY || y >= level.maxY) return null

        val chunkX = x shr 4
        val chunkZ = z shr 4

        var chunk = cachedChunk
        if (chunkX != cachedChunkX || chunkZ != cachedChunkZ || chunk == null) {
            chunk = try {
                level.getChunk(chunkX, chunkZ)
            } catch (_: Exception) {
                return null
            }
            cachedChunk = chunk
            cachedChunkX = chunkX
            cachedChunkZ = chunkZ
        }

        return try {
            chunk.getBlockState(BlockPos(x, y, z))
        } catch (_: Exception) {
            null
        }
    }

    fun isPassable(x: Int, y: Int, z: Int): Boolean {
        val state = getBlockState(x, y, z) ?: return false // unloaded = impassable
        return !state.isSolid
    }

    fun isSolid(x: Int, y: Int, z: Int): Boolean {
        val state = getBlockState(x, y, z) ?: return false // unloaded = not solid
        return state.isSolid
    }

    fun isWalkable(x: Int, y: Int, z: Int): Boolean {
        return isSolid(x, y - 1, z) && isPassable(x, y, z) && isPassable(x, y + 1, z)
    }
}
