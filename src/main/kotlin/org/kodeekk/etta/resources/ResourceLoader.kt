package org.kodeekk.etta.resources

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import org.kodeekk.etta.animation.AnimationController
import org.kodeekk.etta.parser.McmetaParser
import org.kodeekk.etta.parser.McmetaxParser
import org.kodeekk.etta.texture.SpriteManager
import org.slf4j.LoggerFactory

/**
 * Loads ETTA animations and frames from resource packs.
 *
 * Supports two formats:
 * 1. .mcmetax (ETTA format) with .etta/frames/ directory
 * 2. .mcmeta (vanilla format) with single texture file
 */
object ResourceLoader {
    private val logger = LoggerFactory.getLogger("ETTA-Resources")

    fun loadAnimations(resourceManager: ResourceManager) {
        logger.info("===========================================")
        logger.info("Loading ETTA animations from all resource packs...")
        logger.info("===========================================")

        // Clear existing data
        AnimationController.clear()
        SpriteManager.clear()

        var mcmetaxCount = 0
        var mcmetaCount = 0

        // Scan all namespaces
        logger.info("Available namespaces: ${resourceManager.namespaces}")

        resourceManager.namespaces.forEach { namespace ->
            try {
                logger.info("Scanning namespace: $namespace")

                // Load .mcmetax files (ETTA format with frames)
                val mcmetaxFound = loadMcmetaxFiles(resourceManager, namespace)
                mcmetaxCount += mcmetaxFound
                logger.info("Found $mcmetaxFound .mcmetax files in namespace $namespace")

                // Load .mcmeta files (vanilla format)
                val mcmetaFound = loadMcmetaFiles(resourceManager, namespace)
                mcmetaCount += mcmetaFound
                logger.info("Found $mcmetaFound .mcmeta files in namespace $namespace")
            } catch (e: Exception) {
                logger.error("Failed to scan namespace: $namespace", e)
            }
        }

        logger.info("===========================================")
        logger.info("Loaded $mcmetaxCount ETTA (.mcmetax) + $mcmetaCount standard (.mcmeta) animations")
        logger.info("===========================================")
        logger.debug(SpriteManager.getDebugInfo())
    }

    /**
     * Loads .mcmetax files and their associated frames.
     *
     * Expected structure:
     * textures/item/totem_of_undying.etta/
     *   ├── totem_of_undying.mcmetax (or any .mcmetax file)
     *   └── frames/
     *       ├── 0.png
     *       ├── 1.png
     *       └── ...
     */
    private fun loadMcmetaxFiles(resourceManager: ResourceManager, namespace: String): Int {
        var count = 0

        try {
            // FIXED: Properly scan for .mcmetax files using listResources
            // listResources expects (path, filter) where filter checks the ResourceLocation
            val mcmetaxResources = resourceManager.listResources("textures") { location ->
                location.namespace == namespace &&
                        location.path.endsWith(".mcmetax") &&
                        location.path.contains(".etta/")
            }

            logger.info("Found ${mcmetaxResources.size} potential .mcmetax files in namespace $namespace")

            mcmetaxResources.forEach { (mcmetaxId, resource) ->
                try {
                    logger.info("Processing mcmetax: $mcmetaxId")

                    resource.open().use { inputStream ->
                        val content = inputStream.bufferedReader().use { it.readText() }

                        // Parse path: textures/item/totem_of_undying.etta/totem_of_undying.mcmetax
                        // Extract: textures/item/totem_of_undying
                        val fullPath = mcmetaxId.path
                        logger.debug("Full mcmetax path: $fullPath")

                        // Find .etta directory
                        val ettaIndex = fullPath.indexOf(".etta/")
                        if (ettaIndex == -1) {
                            logger.warn("Mcmetax file not in .etta directory: $mcmetaxId")
                            return@forEach
                        }

                        // Extract base texture path (everything before .etta/)
                        val texturePath = fullPath.substring(0, ettaIndex)
                        val textureId = ResourceLocation.fromNamespaceAndPath(mcmetaxId.namespace, texturePath)

                        logger.info("Extracted texture ID: $textureId from $mcmetaxId")

                        // Parse animation metadata
                        val metadata = McmetaxParser.parse(content, textureId)
                        if (metadata != null) {
                            logger.info("Successfully parsed mcmetax for $textureId")

                            // Load frames using SpriteManager
                            val success = SpriteManager.loadAndRegisterFrames(textureId)

                            if (success) {
                                // Register animation
                                AnimationController.registerAnimation(metadata)

                                count++
                                logger.info("✓ Loaded ETTA animation: $textureId (${SpriteManager.getFrameCount(textureId)} frames)")
                            } else {
                                logger.warn("✗ No frames found for $textureId, skipping animation registration")
                            }
                        } else {
                            logger.error("✗ Failed to parse mcmetax: $mcmetaxId")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to load mcmetax: $mcmetaxId", e)
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to list mcmetax resources in namespace $namespace", e)
        }

        return count
    }

    /**
     * Loads vanilla .mcmeta files.
     */
    private fun loadMcmetaFiles(resourceManager: ResourceManager, namespace: String): Int {
        var count = 0

        try {
            // FIXED: Properly scan for .png.mcmeta files
            val mcmetaResources = resourceManager.listResources("textures") { location ->
                location.namespace == namespace &&
                        location.path.endsWith(".png.mcmeta") &&
                        !location.path.contains(".etta/") // Exclude ETTA directories
            }

            logger.debug("Found ${mcmetaResources.size} potential .mcmeta files in namespace $namespace")

            mcmetaResources.forEach { (mcmetaId, resource) ->
                try {
                    resource.open().use { inputStream ->
                        val content = inputStream.bufferedReader().use { it.readText() }
                        val texturePath = mcmetaId.path.removeSuffix(".mcmeta")
                        val textureId = ResourceLocation.fromNamespaceAndPath(mcmetaId.namespace, texturePath)

                        // Skip if we already have mcmetax for this texture
                        if (AnimationController.isAnimated(textureId)) {
                            logger.debug("Skipping mcmeta for $textureId (mcmetax already loaded)")
                            return@forEach
                        }

                        val metadata = McmetaParser.parse(content, textureId)

                        if (metadata != null) {
                            AnimationController.registerAnimation(metadata)
                            count++
                            logger.info("✓ Loaded vanilla animation: $textureId")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to load mcmeta: $mcmetaId", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to list mcmeta resources in namespace $namespace", e)
        }

        return count
    }
}