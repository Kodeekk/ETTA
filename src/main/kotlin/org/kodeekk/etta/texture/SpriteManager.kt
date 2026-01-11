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

object SpriteManager {
    private val logger = LoggerFactory.getLogger("ETTA-SpriteManager")

    private val spriteContents = ConcurrentHashMap<ResourceLocation, List<SpriteContents>>()

    fun loadAndRegisterFrames(textureId: ResourceLocation, frameCount: Int = -1): Boolean {
        val client = Minecraft.getInstance()
        val resourceManager = client.resourceManager

        val contents = mutableListOf<SpriteContents>()
        var frameIndex = 0

        logger.debug("Loading frames for texture: $textureId")

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

        if (!validateFrames(contents)) {
            logger.error("Frame validation failed for $textureId")
            contents.forEach { it.close() }
            return false
        }

        spriteContents[textureId] = contents
        logger.info("Registered ${contents.size} frames for $textureId")

        return true
    }

    fun getSpriteContents(textureId: ResourceLocation, frameIndex: Int): SpriteContents? {
        val contents = spriteContents[textureId] ?: return null
        return contents.getOrNull(frameIndex)
    }

//    fun getAllSpriteContents(textureId: ResourceLocation): List<SpriteContents>? {
//        return spriteContents[textureId]
//    }

    fun getFrameCount(textureId: ResourceLocation): Int {
        return spriteContents[textureId]?.size ?: 0
    }

    fun hasFrames(textureId: ResourceLocation): Boolean {
        return spriteContents.containsKey(textureId)
    }

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

//    fun clearFrames(textureId: ResourceLocation) {
//        val contents = spriteContents.remove(textureId)
//        if (contents != null) {
//            contents.forEach { it.close() }
//            logger.debug("Cleared frames for $textureId")
//        }
//    }

    fun clear() {
        spriteContents.values.forEach { contents ->
            contents.forEach { it.close() }
        }

        spriteContents.clear()
        logger.info("Cleared all sprite data")
    }

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