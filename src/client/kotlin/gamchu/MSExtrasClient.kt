package gamchu

import gamchu.pathfinder.api.PathfinderAPI
import gamchu.render.api.NvgAPI
import gamchu.render.api.NvgHudRenderer
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object MSExtrasClient : ClientModInitializer {
	private val logger = LoggerFactory.getLogger("ms-extras")

	override fun onInitializeClient() {
		PathfinderAPI.getProvider()
        NvgAPI.getProvider()
        NvgHudRenderer
		logger.info("MS-Extras initialized")
	}
}