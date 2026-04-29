package gamchu.render.api

class NvgAPI private constructor() {

    companion object {

        private val provider = NvgProvider()

        @JvmStatic
        fun getProvider(): NvgProvider = provider
    }
}
