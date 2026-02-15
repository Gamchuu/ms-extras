package gamchu.pathfinder.api

/**
 * Provides access to the [IPathfinder] instance.
 * Mirrors Baritone's IBaritoneProvider pattern.
 */
class PathfinderProvider {
    private val pathfinder = PathfinderImpl()

    @JvmName("getPathfinder")
    fun getPathfinder(): PathfinderImpl = pathfinder

    internal fun shutdown() {
        pathfinder.shutdown()
    }
}
