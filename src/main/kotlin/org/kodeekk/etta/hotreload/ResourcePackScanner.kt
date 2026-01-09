package org.kodeekk.etta.hotreload

import net.minecraft.client.Minecraft
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.PathPackResources
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.jvm.java

/**
 * Scans active resource packs and registers them for file watching.
 */
object ResourcePackScanner {
    private val logger = LoggerFactory.getLogger("ETTA-ResourcePackScanner")

    /**
     * Scans all active resource packs and registers them with FileWatcher.
     */
    fun scanAndWatch() {
        val client = Minecraft.getInstance()
        val resourceManager = client.resourceManager

        logger.info("Scanning active resource packs...")

        var watchedCount = 0

        // Get all resource packs
        try {
            val packs = resourceManager.listPacks()

            for (pack in packs) {
                try {
                    val packPath = extractPackPath(pack) ?: continue
                    val packName = pack.location().title.toString()

                    // Register with FileWatcher
                    FileWatcher.watchResourcePack(packPath, packName)

                    // Register path mapping for hot reload
                    HotReloadHandler.registerResourcePack(packPath, extractNamespace(pack))

                    watchedCount++
                    logger.info("Watching pack: $packName at $packPath")
                } catch (e: Exception) {
                    logger.debug("Could not watch pack: ${pack.location().title.toString()} (${e.message})")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to iterate resource packs", e)
        }

        logger.info("Watching $watchedCount resource packs for hot reload")
    }

    /**
     * Extracts the file system path from a resource pack.
     */
    private fun extractPackPath(pack: PackResources): Path? {
        return when (pack) {
            is PathPackResources -> {
                // Development resource pack (folder)
                try {
                    // Access private field via reflection
                    val field = PathPackResources::class.java.getDeclaredField("root")
                    field.isAccessible = true
                    field.get(pack) as? Path
                } catch (e: Exception) {
                    logger.debug("Could not extract path from DirectoryResourcePack", e)
                    null
                }
            }
            is FilePackResources -> {
                // Zipped resource pack
                try {
                    val field = FilePackResources::class.java.getDeclaredField("file")
                    field.isAccessible = true
                    (field.get(pack) as? java.io.File)?.toPath()
                } catch (e: Exception) {
                    logger.debug("Could not extract path from ZipResourcePack", e)
                    null
                }
            }
            else -> {
                // Built-in resources or other types - skip
                logger.debug("Unknown/unsupported pack type: ${pack::class.java.simpleName}")
                null
            }
        }
    }

    /**
     * Extracts the namespace from a resource pack.
     * Defaults to "minecraft" if unable to determine.
     */
    private fun extractNamespace(pack: PackResources): String {
        return try {
            // Try to get namespaces from the pack
            val namespaces = pack.getNamespaces(PackType.CLIENT_RESOURCES)
            namespaces.firstOrNull() ?: "minecraft"
        } catch (e: Exception) {
            "minecraft"
        }
    }
}