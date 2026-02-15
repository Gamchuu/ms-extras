package gamchu.pathfinder.api

/**
 * Main entry point for the MS-Extras pathfinder.
 * Singleton — accessible from Minescript via:
 *
 *   PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
 *   provider = PathfinderAPI.INSTANCE.getProvider()
 *
 * This mirrors Baritone's BaritoneAPI.getProvider() pattern.
 */

class PathfinderAPI private constructor() {

    companion object {

        private val provider = PathfinderProvider()

        @JvmStatic
        fun getProvider(): PathfinderProvider {
            return provider
        }
    }
}

