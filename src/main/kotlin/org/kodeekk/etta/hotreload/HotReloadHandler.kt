package org.kodeekk.etta.hotreload

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.resources.metadata.animation.FrameSize
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceMetadata
import org.kodeekk.etta.animation.AnimationController
import org.kodeekk.etta.parser.McmetaxParser
import org.kodeekk.etta.texture.ReloadTexture
import org.kodeekk.etta.texture.SpriteManager
import org.kodeekk.etta.texture.SpriteUploader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Handles hot reload events from FileWatcher.
 * Provides API for uploading custom frames.
 * Now integrates with ReloadTexture.java for GPU uploads.
 */
object HotReloadHandler {
    private val logger = LoggerFactory.getLogger("ETTA-HotReload")

    // Track resource pack paths to resource identifiers
    private val pathToIdentifier = mutableMapOf<String, ResourceLocation>()

    // Create instance of ReloadTexture for GPU uploads
    private val reloadTexture = ReloadTexture()

    /**
     * Registers a resource pack path mapping.
     *
     * @param basePath Base path of the resource pack
     * @param namespace The namespace (e.g., "minecraft")
     */
    fun registerResourcePack(basePath: Path, namespace: String) {
        pathToIdentifier[basePath.toString()] = ResourceLocation.fromNamespaceAndPath(namespace, "")
        logger.debug("Registered resource pack mapping: $basePath -> $namespace")
    }

    /**
     * Handles file changes detected by FileWatcher.
     */
    fun handleChanges(changes: Map<Path, FileWatcher.ChangeType>) {
        val affectedTextures = mutableMapOf<ResourceLocation, MutableList<Path>>()

        // Group changes by texture ID
        for ((path, changeType) in changes) {
            val textureId = extractTextureId(path) ?: continue

            affectedTextures.getOrPut(textureId) { mutableListOf() }.add(path)

            logger.debug("Change $changeType: $path -> $textureId")
        }

        // Reload affected textures
        for ((textureId, changedFiles) in affectedTextures) {
            try {
                reloadTexture(textureId, changedFiles)
            } catch (e: Exception) {
                logger.error("Failed to hot reload texture: $textureId", e)
            }
        }

        logger.info("Hot reloaded ${affectedTextures.size} textures")
    }

    /**
     * Reloads a specific texture and its animation.
     */
    private fun reloadTexture(textureId: ResourceLocation, changedFiles: List<Path>) {
        logger.info("Hot reloading texture: $textureId")

        // Check if .mcmetax was changed
        val mcmetaxChanged = changedFiles.any { it.toString().endsWith(".mcmetax") }

        if (mcmetaxChanged) {
            // Reload animation metadata
            reloadAnimationMetadata(textureId, changedFiles)
        }

        // Check if frames were changed
        val frameChanges = changedFiles.filter {
            it.toString().contains(".etta") && it.toString().contains("frames")
        }

        if (frameChanges.isNotEmpty()) {
            // Reload specific frames using ReloadTexture
            reloadFramesWithReloadTexture(textureId, frameChanges)
        }
    }

    /**
     * Reloads animation metadata from .mcmetax file.
     */
    private fun reloadAnimationMetadata(textureId: ResourceLocation, changedFiles: List<Path>) {
        val mcmetaxPath = changedFiles.find { it.toString().endsWith(".mcmetax") } ?: return

        if (!Files.exists(mcmetaxPath)) {
            logger.warn("Mcmetax file not found: $mcmetaxPath")
            return
        }

        try {
            val content = Files.readString(mcmetaxPath)
            val metadata = McmetaxParser.parse(content, textureId)

            if (metadata != null) {
                AnimationController.registerAnimation(metadata)
                logger.info("Reloaded animation metadata: $textureId")
            } else {
                logger.error("Failed to parse mcmetax: $mcmetaxPath")
            }
        } catch (e: Exception) {
            logger.error("Error reading mcmetax file: $mcmetaxPath", e)
        }
    }

    /**
     * Reloads specific frames from disk using ReloadTexture.
     * This method uses the ReloadTexture.java class for proper GPU uploads.
     */
    private fun reloadFramesWithReloadTexture(textureId: ResourceLocation, frameFiles: List<Path>) {
        logger.info("Reloading ${frameFiles.size} frames for $textureId using ReloadTexture")

        for (framePath in frameFiles) {
            val frameIndex = extractFrameIndex(framePath) ?: continue

            try {
                // Use ReloadTexture to handle the GPU upload
                // Convert Path to String for ReloadTexture
                val pathString = framePath.toString()

                logger.info("Hot reloading frame $frameIndex from: $pathString")

                // Call ReloadTexture.onHotReload with the specific texture
                reloadTexture.onHotReload(pathString, textureId)

                logger.info("✓ Hot reloaded frame $frameIndex for $textureId")
            } catch (e: Exception) {
                logger.error("Failed to reload frame $frameIndex: $framePath", e)
            }
        }
    }

    /**
     * Reloads specific frames from disk (fallback method).
     */
    private fun reloadFrames(textureId: ResourceLocation, frameFiles: List<Path>) {
        for (framePath in frameFiles) {
            val frameIndex = extractFrameIndex(framePath) ?: continue

            try {
                reloadSingleFrame(textureId, framePath, frameIndex)
            } catch (e: Exception) {
                logger.error("Failed to reload frame $frameIndex: $framePath", e)
            }
        }
    }

    /**
     * Reloads a single frame from disk (fallback method).
     */
    private fun reloadSingleFrame(textureId: ResourceLocation, framePath: Path, frameIndex: Int) {
        if (!Files.exists(framePath)) {
            logger.warn("Frame file not found: $framePath")
            return
        }

        Files.newInputStream(framePath).use { stream ->
            val image = NativeImage.read(stream)

            // Create SpriteContents from NativeImage
            val spriteContents = SpriteContents(
                ResourceLocation.fromNamespaceAndPath(textureId.namespace, "${textureId.path}_frame_$frameIndex"),
                FrameSize(image.width, image.height),
                image,
                ResourceMetadata.EMPTY
            )

            // Upload to GPU
            val success = SpriteUploader.uploadSpriteContents(textureId, spriteContents)

            if (success) {
                logger.info("Hot reloaded frame $frameIndex for $textureId")
            } else {
                spriteContents.close()
                logger.error("Failed to upload frame $frameIndex for $textureId")
            }
        }
    }

    /**
     * PUBLIC API: Upload custom frame from NativeImage.
     *
     * @param textureId The texture ResourceLocation
     * @param frameImage The NativeImage containing the frame data
     * @param frameIndex The frame index
     * @return true if successful
     */
    fun uploadCustomFrame(textureId: ResourceLocation, frameImage: NativeImage, frameIndex: Int): Boolean {
        return try {
            val spriteContents = SpriteContents(
                ResourceLocation.fromNamespaceAndPath(textureId.namespace, "${textureId.path}_frame_$frameIndex"),
                FrameSize(frameImage.width, frameImage.height),
                frameImage,
                ResourceMetadata.EMPTY
            )

            val success = SpriteUploader.uploadSpriteContents(textureId, spriteContents)

            if (!success) {
                spriteContents.close()
            }

            success
        } catch (e: Exception) {
            logger.error("Failed to upload custom frame", e)
            false
        }
    }

    /**
     * PUBLIC API: Upload custom frame from SpriteContents.
     *
     * @param textureId The texture ResourceLocation
     * @param spriteContents The SpriteContents to upload
     * @return true if successful
     */
    fun uploadCustomSpriteContents(textureId: ResourceLocation, spriteContents: SpriteContents): Boolean {
        return try {
            SpriteUploader.uploadSpriteContents(textureId, spriteContents)
        } catch (e: Exception) {
            logger.error("Failed to upload custom sprite contents", e)
            false
        }
    }

    /**
     * PUBLIC API: Upload frame from file path using ReloadTexture.
     *
     * @param textureId The texture ResourceLocation
     * @param framePath Path to the PNG file
     * @param frameIndex The frame index
     * @return true if successful
     */
    fun uploadFrameFromFile(textureId: ResourceLocation, framePath: Path, frameIndex: Int): Boolean {
        return try {
            val pathString = framePath.toString()
            reloadTexture.onHotReload(pathString, textureId)
            true
        } catch (e: Exception) {
            logger.error("Failed to upload frame from file", e)
            false
        }
    }

    /**
     * Extracts texture ID from file path.
     *
     * Example:
     * /path/to/resourcepack/assets/minecraft/textures/item/totem_of_undying.etta/frames/0.png
     * -> minecraft:textures/item/totem_of_undying
     */
    private fun extractTextureId(path: Path): ResourceLocation? {
        val pathStr = path.toString().replace('\\', '/')

        // Find "assets/<namespace>/textures/..."
        val assetsIndex = pathStr.indexOf("/assets/")
        if (assetsIndex == -1) return null

        val afterAssets = pathStr.substring(assetsIndex + 8) // Skip "/assets/"
        val parts = afterAssets.split("/")

        if (parts.size < 3) return null

        val namespace = parts[0]
        val remainingPath = parts.drop(1).joinToString("/")

        // Remove .etta directory and everything after
        val texturePath = if (remainingPath.contains(".etta/")) {
            remainingPath.substringBefore(".etta/")
        } else {
            remainingPath
        }

        return ResourceLocation.fromNamespaceAndPath(namespace, texturePath)
    }

    /**
     * Extracts frame index from frame file path.
     *
     * Example: .../frames/5.png -> 5
     */
    private fun extractFrameIndex(path: Path): Int? {
        val filename = path.fileName.toString()
        return filename.removeSuffix(".png").toIntOrNull()
    }
}