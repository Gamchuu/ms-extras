package gamchu.pathfinder.api

class PathfinderProvider {
    private val pathfinder = PathfinderImpl()

    @JvmName("getPathfinder")
    fun getPathfinder(): PathfinderImpl = pathfinder

    internal fun shutdown() {
        pathfinder.shutdown()
    }
}
