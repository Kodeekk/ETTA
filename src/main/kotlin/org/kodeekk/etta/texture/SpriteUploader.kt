package org.kodeekk.etta.texture

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.kodeekk.etta.mixin.accessor.TextureAtlasSpriteAccessor
import org.kodeekk.etta.mixin.accessor.TextureManagerAccessor
import org.kodeekk.etta.mixin.accessor.TextureAtlasAccessor
import org.slf4j.LoggerFactory

/**
 * Handles uploading sprite data to the GPU for dynamic texture updates.
 *
 * Uses mixin accessors instead of reflection for safer field access.
 * Provides simple API for uploading custom SpriteContents.
 */
object SpriteUploader {
    private val logger = LoggerFactory.getLogger("ETTA-SpriteUploader")

    /**
     * PUBLIC API: Upload custom SpriteContents to a texture.
     * This is the main function you requested.
     *
     * @param textureId The texture ResourceLocation (e.g., "minecraft:textures/item/totem_of_undying")
     * @param spriteContents The SpriteContents to upload
     * @return true if successful, false otherwise
     */
    fun uploadSpriteContents(textureId: ResourceLocation, spriteContents: SpriteContents): Boolean {
        val client = Minecraft.getInstance()
        val textureManager = client.textureManager

        // Find the sprite using mixin accessors
        val sprite = findSpriteInAtlas(textureId)

        if (sprite == null) {
            // Log all available sprites for debugging (only on first failure)
            if (logger.isDebugEnabled) {
                logger.debug("Could not find sprite for $textureId")
                logger.debug("Available sprites:")
                logAllSprites()
            }
            logger.error("Could not find sprite in any atlas: $textureId")
            return false
        }

        // Validate dimensions match
        val oldContents = sprite.contents()
        if (spriteContents.width() != oldContents.width() ||
            spriteContents.height() != oldContents.height()) {
            logger.error("Sprite dimensions mismatch: ${spriteContents.width()}x${spriteContents.height()} vs ${oldContents.width()}x${oldContents.height()}")
            return false
        }

        try {
            // Replace sprite contents using accessor
            (sprite as TextureAtlasSpriteAccessor).setContents(spriteContents)

            // Find the atlas this sprite belongs to
            val atlas = findAtlasForSprite(textureId)

            if (atlas != null) {
                // Upload using the atlas's texture
                sprite.uploadFirstFrame(atlas.getTexture())
            } else {
                logger.error("Could not find atlas for sprite: $textureId")
                return false
            }

            logger.debug("Successfully uploaded sprite contents for $textureId")
            return true
        } catch (e: Exception) {
            logger.error("Failed to upload sprite contents for $textureId", e)
            return false
        }
    }

    /**
     * Finds a sprite in any atlas, trying multiple possible ResourceLocation formats.
     * Uses mixin accessors instead of reflection.
     */
    private fun findSpriteInAtlas(textureId: ResourceLocation): TextureAtlasSprite? {
        val client = Minecraft.getInstance()
        val textureManager = client.textureManager

        // Generate possible sprite keys
        val possibleKeys = generatePossibleSpriteKeys(textureId)

        // Use TextureManagerAccessor to get the texture map
        val texturesMap = (textureManager as TextureManagerAccessor).getByPath()

        // Search all atlases
        for ((_, texture) in texturesMap) {
            if (texture !is TextureAtlas) continue

            // Use TextureAtlasAccessor to get sprites map
            val spritesMap = (texture as TextureAtlasAccessor).getTexturesByName()

            // Try each possible key
            for (key in possibleKeys) {
                val sprite = spritesMap[key]
                if (sprite != null) {
                    logger.debug("Found sprite with key: $key in atlas")
                    return sprite
                }
            }
        }

        return null
    }

    /**
     * Finds the atlas that contains a specific sprite.
     */
    private fun findAtlasForSprite(textureId: ResourceLocation): TextureAtlas? {
        val client = Minecraft.getInstance()
        val textureManager = client.textureManager
        val possibleKeys = generatePossibleSpriteKeys(textureId)

        val texturesMap = (textureManager as TextureManagerAccessor).getByPath()

        for ((_, texture) in texturesMap) {
            if (texture !is TextureAtlas) continue

            val spritesMap = (texture as TextureAtlasAccessor).getTexturesByName()

            for (key in possibleKeys) {
                if (spritesMap.containsKey(key)) {
                    return texture
                }
            }
        }

        return null
    }

    /**
     * Generates possible sprite ResourceLocation keys.
     *
     * Minecraft stores sprites with different key formats:
     * - minecraft:item/totem_of_undying
     * - minecraft:textures/item/totem_of_undying
     * - minecraft:totem_of_undying
     */
    private fun generatePossibleSpriteKeys(textureId: ResourceLocation): List<ResourceLocation> {
        val keys = mutableListOf<ResourceLocation>()
        val namespace = textureId.namespace
        val path = textureId.path

        // Original
        keys.add(textureId)

        // Remove "textures/" prefix if present
        if (path.startsWith("textures/")) {
            val withoutTextures = path.removePrefix("textures/")
            keys.add(ResourceLocation.fromNamespaceAndPath(namespace, withoutTextures))
        }

        // Just the filename
        val filename = path.substringAfterLast('/')
        keys.add(ResourceLocation.fromNamespaceAndPath(namespace, filename))

        // With item/ prefix
        if (!path.startsWith("item/")) {
            val filenameOnly = path.substringAfterLast('/')
            keys.add(ResourceLocation.fromNamespaceAndPath(namespace, "item/$filenameOnly"))
        }

        // With block/ prefix
        if (!path.startsWith("block/")) {
            val filenameOnly = path.substringAfterLast('/')
            keys.add(ResourceLocation.fromNamespaceAndPath(namespace, "block/$filenameOnly"))
        }

        return keys.distinct()
    }

    /**
     * Logs all available sprites for debugging.
     */
    private fun logAllSprites() {
        val client = Minecraft.getInstance()
        val textureManager = client.textureManager

        try {
            val texturesMap = (textureManager as TextureManagerAccessor).getByPath()

            for ((atlasId, texture) in texturesMap) {
                if (texture !is TextureAtlas) continue

                val spritesMap = (texture as TextureAtlasAccessor).getTexturesByName()

                logger.debug("Atlas $atlasId has ${spritesMap.size} sprites:")
                spritesMap.keys.take(10).forEach { key ->
                    logger.debug("  - $key")
                }
                if (spritesMap.size > 10) {
                    logger.debug("  ... and ${spritesMap.size - 10} more")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to log sprites", e)
        }
    }

    /**
     * Upload a frame from SpriteManager (for animation system).
     */
    fun uploadFrameFromMemory(textureId: ResourceLocation, frameIndex: Int): Boolean {
        val spriteContents = SpriteManager.getSpriteContents(textureId, frameIndex) ?: run {
            logger.error("No sprite contents found in memory for $textureId frame $frameIndex")
            return false
        }

        return uploadSpriteContents(textureId, spriteContents)
    }

    /**
     * Upload by item ID (converts to texture path automatically).
     *
     * Example: uploadByItemId(ResourceLocation.of("minecraft", "totem_of_undying"), spriteContents)
     */
    fun uploadByItemId(itemId: ResourceLocation, spriteContents: SpriteContents): Boolean {
        val textureId = ResourceLocation.fromNamespaceAndPath(itemId.namespace, "textures/item/${itemId.path}")
        return uploadSpriteContents(textureId, spriteContents)
    }
}