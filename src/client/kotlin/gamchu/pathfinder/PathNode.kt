package gamchu.pathfinder


class PathNode(
    val packedPos: Long,
    var gCost: Double,
    var fCost: Double,
    var parent: PathNode? = null,
    var heapIndex: Int = -1
)
object PackedPos {
    private const val Y_BITS = 12
    private const val Z_BITS = 26
    private const val X_BITS = 26
    private const val Y_MASK = (1L shl Y_BITS) - 1L
    private const val Z_MASK = (1L shl Z_BITS) - 1L

    @JvmStatic
    fun pack(x: Int, y: Int, z: Int): Long {
        return (x.toLong() shl (Y_BITS + Z_BITS)) or
               ((z.toLong() and Z_MASK) shl Y_BITS) or
               (y.toLong() and Y_MASK)
    }

    @JvmStatic
    fun unpackX(packed: Long): Int = (packed shr (Y_BITS + Z_BITS)).toInt()
        .
    @JvmStatic
    fun unpackY(packed: Long): Int = (packed shl (64 - Y_BITS) shr (64 - Y_BITS)).toInt()

    @JvmStatic
    fun unpackZ(packed: Long): Int = (packed shl (64 - Z_BITS - Y_BITS) shr (64 - Z_BITS)).toInt()
}
