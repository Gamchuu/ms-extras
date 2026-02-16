package gamchu

import gamchu.pathfinder.api.PathfinderAPI
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object MSExtrasClient : ClientModInitializer {
	private val logger = LoggerFactory.getLogger("ms-extras")

	override fun onInitializeClient() {
		PathfinderAPI.getProvider()
		logger.info("MS-Extras pathfinder API initialized")
	}
}