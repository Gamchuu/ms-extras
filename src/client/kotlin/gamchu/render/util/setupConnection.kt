package gamchu.render.util

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Implementation from OdinFabric by odtheking
 * Original work: https://github.com/odtheking/OdinFabric
 *
 * @author OdinFabric
 */
fun setupConnection(url: String, timeout: Int = 5000, useCaches: Boolean = true): InputStream {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.setRequestMethod("GET")
    connection.setUseCaches(useCaches)
    connection.addRequestProperty("User-Agent", "MS-Extras")
    connection.setReadTimeout(timeout)
    connection.setConnectTimeout(timeout)
    connection.setDoOutput(true)
    return connection.inputStream
}