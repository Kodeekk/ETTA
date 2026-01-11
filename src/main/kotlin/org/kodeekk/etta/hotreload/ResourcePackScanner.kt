package org.kodeekk.etta.hotreload

import net.minecraft.client.Minecraft
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.PathPackResources
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.jvm.java

object ResourcePackScanner {
    private val logger = LoggerFactory.getLogger("ETTA-ResourcePackScanner")

    fun scanAndWatch() {
        val client = Minecraft.getInstance()
        val resourceManager = client.resourceManager

        logger.info("Scanning active resource packs...")

        var watchedCount = 0

        try {
            val packs = resourceManager.listPacks()

            for (pack in packs) {
                try {
                    val packPath = extractPackPath(pack) ?: continue
                    val packName = pack.location().title.toString()

                    FileWatcher.watchResourcePack(packPath, packName)

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

    private fun extractPackPath(pack: PackResources): Path? {
        return when (pack) {
            is PathPackResources -> {
                try {
                    val field = PathPackResources::class.java.getDeclaredField("root")
                    field.isAccessible = true
                    field.get(pack) as? Path
                } catch (e: Exception) {
                    logger.debug("Could not extract path from DirectoryResourcePack", e)
                    null
                }
            }
            is FilePackResources -> {
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
                logger.debug("Unknown/unsupported pack type: ${pack::class.java.simpleName}")
                null
            }
        }
    }

    private fun extractNamespace(pack: PackResources): String {
        return try {
            val namespaces = pack.getNamespaces(PackType.CLIENT_RESOURCES)
            namespaces.firstOrNull() ?: "minecraft"
        } catch (e: Exception) {
            "minecraft"
        }
    }
}