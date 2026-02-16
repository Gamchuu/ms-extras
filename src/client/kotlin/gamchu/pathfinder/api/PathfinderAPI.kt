package gamchu.pathfinder.api


class PathfinderAPI private constructor() {

    companion object {

        private val provider = PathfinderProvider()

        @JvmStatic
        fun getProvider(): PathfinderProvider {
            return provider
        }
    }
}

