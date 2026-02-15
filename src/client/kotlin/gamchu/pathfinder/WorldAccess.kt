package gamchu.pathfinder

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk

/**
 * Thread-safe, cached read-only access to the client world's block states.
 *
 * Caches the last accessed [LevelChunk] to avoid repeated hash lookups
 * when scanning neighbors in the same chunk (which is the common case).
 *
 * Unloaded chunks are treated as impassable (returns null BlockState).
 */
class WorldAccess(private val level: ClientLevel) {

    // Cached chunk to avoid repeated ChunkSource lookups
    @Volatile private var cachedChunkX = Int.MAX_VALUE
    @Volatile private var cachedChunkZ = Int.MAX_VALUE
    @Volatile private var cachedChunk: LevelChunk? = null

    /**
     * Get the block state at (x, y, z).
     * Returns null if the chunk is not loaded or y is out of range.
     */
    fun getBlockState(x: Int, y: Int, z: Int): BlockState? {
        if (y < level.minY || y >= level.maxY) return null

        val chunkX = x shr 4
        val chunkZ = z shr 4

        // Fast path: same chunk as last call
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

        // Direct section access for speed
        return try {
            chunk.getBlockState(BlockPos(x, y, z))
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Check if a block position is passable (air, plants, etc.) —
     * entities can occupy this space.
     */
    fun isPassable(x: Int, y: Int, z: Int): Boolean {
        val state = getBlockState(x, y, z) ?: return false // unloaded = impassable
        return !state.isSolid
    }

    /**
     * Check if a block is solid (can be stood on).
     */
    fun isSolid(x: Int, y: Int, z: Int): Boolean {
        val state = getBlockState(x, y, z) ?: return false // unloaded = not solid
        return state.isSolid
    }

    /**
     * Check if a position is walkable:
     * - Block below is solid (can stand on)
     * - Block at feet is passable
     * - Block at head is passable
     */
    fun isWalkable(x: Int, y: Int, z: Int): Boolean {
        return isSolid(x, y - 1, z) && isPassable(x, y, z) && isPassable(x, y + 1, z)
    }
}
