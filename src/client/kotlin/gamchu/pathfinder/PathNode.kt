package gamchu.pathfinder

/**
 * A* search node. Mutable for in-place updates during search.
 * Position is stored as a packed Long to avoid BlockPos allocations.
 *
 * Packing: x in bits 38..63 (26 bits), z in bits 12..37 (26 bits), y in bits 0..11 (12 bits)
 * This matches Minecraft's BlockPos.asLong() layout.
 */
class PathNode(
    /** Packed (x, y, z) position — use [PackedPos] for encode/decode. */
    val packedPos: Long,
    /** Cost from start to this node. */
    var gCost: Double,
    /** Estimated total cost (g + h). */
    var fCost: Double,
    /** Parent node for path reconstruction. */
    var parent: PathNode? = null,
    /** Index in the binary heap (for decrease-key). -1 if not in heap. */
    var heapIndex: Int = -1
)

/**
 * Utility for packing/unpacking block coordinates into a single Long.
 * Uses the same layout as Minecraft's BlockPos.asLong():
 *   x: 26 bits, z: 26 bits, y: 12 bits
 */
object PackedPos {
    private const val Y_BITS = 12
    private const val Z_BITS = 26
    private const val X_BITS = 26
    private const val Y_MASK = (1L shl Y_BITS) - 1L   // 0xFFF
    private const val Z_MASK = (1L shl Z_BITS) - 1L    // 0x3FFFFFF

    @JvmStatic
    fun pack(x: Int, y: Int, z: Int): Long {
        return (x.toLong() shl (Y_BITS + Z_BITS)) or
               ((z.toLong() and Z_MASK) shl Y_BITS) or
               (y.toLong() and Y_MASK)
    }

    // X is at the top (bits 38-63), arithmetic shr sign-extends automatically
    @JvmStatic
    fun unpackX(packed: Long): Int = (packed shr (Y_BITS + Z_BITS)).toInt()

    // Y is in bits 0-11. Shift left 52 to put sign bit at bit 63, then arithmetic shr 52.
    @JvmStatic
    fun unpackY(packed: Long): Int = (packed shl (64 - Y_BITS) shr (64 - Y_BITS)).toInt()

    // Z is in bits 12-37. Shift left 26 to put sign bit at bit 63, then arithmetic shr 38.
    @JvmStatic
    fun unpackZ(packed: Long): Int = (packed shl (64 - Z_BITS - Y_BITS) shr (64 - Z_BITS)).toInt()
}
