package org.kodeekk.etta.texture

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.resources.metadata.animation.FrameSize
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceMetadata
import org.kodeekk.etta.mixin.accessor.NativeImageAccessor
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages sprite data for ETTA animations.
 * Loads frames from .etta/frames/ and creates SpriteContents for GPU upload.
 *
 * Supports hot reloading.
 */
object SpriteManager {
    private val logger = LoggerFactory.getLogger("ETTA-SpriteManager")

    // Maps texture ID to its SpriteContents for each frame
    private val spriteContents = ConcurrentHashMap<ResourceLocation, List<SpriteContents>>()

    /**
     * Loads and registers all frames for a texture from .etta/frames/ directory.
     *
     * @param textureId The texture ResourceLocation (e.g., "minecraft:textures/item/totem_of_undying")
     * @param frameCount Number of frames to load (or -1 to auto-detect)
     * @return true if successful, false otherwise
     */
    fun loadAndRegisterFrames(textureId: ResourceLocation, frameCount: Int = -1): Boolean {
        val client = Minecraft.getInstance()
        val resourceManager = client.resourceManager

        val contents = mutableListOf<SpriteContents>()
        var frameIndex = 0

        logger.debug("Loading frames for texture: $textureId")

        // Load frames until we hit the limit or can't find more
        while (frameCount == -1 || frameIndex < frameCount) {
            val framePath = "${textureId.path}.etta/frames/$frameIndex.png"
            val frameResourceId = ResourceLocation.fromNamespaceAndPath(textureId.namespace, framePath)

            try {
                val resource = resourceManager.getResource(frameResourceId).orElse(null)
                if (resource == null) {
                    if (frameIndex == 0) {
                        logger.warn("No frames found at all for $textureId")
                    } else {
                        logger.debug("Found $frameIndex frames for $textureId")
                    }
                    break
                }

                // Load NativeImage from resource
                resource.open().use { stream ->
                    val image: NativeImage = NativeImage.read(stream)
                    val imageAccess = image as NativeImageAccessor
                    val spriteContent = SpriteContents(
                        ResourceLocation.fromNamespaceAndPath(textureId.namespace, "${textureId.path}_frame_$frameIndex"),
                        FrameSize(imageAccess.getWidth(), imageAccess.getHeight()),
                        image,
                        ResourceMetadata.EMPTY
                    )

                    contents.add(spriteContent)
                    logger.debug("Loaded frame $frameIndex: ${image.width}x${image.height}")
                }

                frameIndex++
            } catch (e: Exception) {
                logger.error("Failed to load frame $frameIndex for $textureId", e)
                break
            }
        }

        if (contents.isEmpty()) {
            logger.warn("No frames found for $textureId in .etta/frames/")
            return false
        }

        // Validate frame consistency
        if (!validateFrames(contents)) {
            logger.error("Frame validation failed for $textureId")
            contents.forEach { it.close() }
            return false
        }

        // Store frames
        spriteContents[textureId] = contents
        logger.info("Registered ${contents.size} frames for $textureId")

        return true
    }

    /**
     * Gets SpriteContents for a specific frame.
     */
    fun getSpriteContents(textureId: ResourceLocation, frameIndex: Int): SpriteContents? {
        val contents = spriteContents[textureId] ?: return null
        return contents.getOrNull(frameIndex)
    }

    /**
     * Gets all SpriteContents for a texture.
     */
    fun getAllSpriteContents(textureId: ResourceLocation): List<SpriteContents>? {
        return spriteContents[textureId]
    }

    /**
     * Gets the number of frames for a texture.
     */
    fun getFrameCount(textureId: ResourceLocation): Int {
        return spriteContents[textureId]?.size ?: 0
    }

    /**
     * Checks if a texture has registered frames.
     */
    fun hasFrames(textureId: ResourceLocation): Boolean {
        return spriteContents.containsKey(textureId)
    }

    /**
     * Validates that all frames have consistent dimensions.
     */
    private fun validateFrames(contents: List<SpriteContents>): Boolean {
        if (contents.isEmpty()) return false

        val firstWidth = contents[0].width
        val firstHeight = contents[0].height

        for (i in 1 until contents.size) {
            if (contents[i].width != firstWidth || contents[i].height != firstHeight) {
                logger.error("Frame $i has inconsistent dimensions: ${contents[i].width}x${contents[i].height} (expected ${firstWidth}x${firstHeight})")
                return false
            }
        }

        return true
    }

    /**
     * Clears frames for a specific texture (for hot reload).
     */
    fun clearFrames(textureId: ResourceLocation) {
        val contents = spriteContents.remove(textureId)
        if (contents != null) {
            contents.forEach { it.close() }
            logger.debug("Cleared frames for $textureId")
        }
    }

    /**
     * Clears all frame data (e.g., on full resource reload).
     */
    fun clear() {
        // Close all SpriteContents
        spriteContents.values.forEach { contents ->
            contents.forEach { it.close() }
        }

        spriteContents.clear()
        logger.info("Cleared all sprite data")
    }

    /**
     * Gets debug information about stored sprites.
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("ETTA Sprite Manager Status:")
            appendLine("  Registered textures: ${spriteContents.size}")
            spriteContents.forEach { (id, contents) ->
                if (contents.isNotEmpty()) {
                    appendLine("  - $id: ${contents.size} frames (${contents[0].width}x${contents[0].height})")
                }
            }
        }
    }
}