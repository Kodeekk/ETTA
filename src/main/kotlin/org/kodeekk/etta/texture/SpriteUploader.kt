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

object SpriteUploader {
    private val logger = LoggerFactory.getLogger("ETTA-SpriteUploader")

    fun uploadSpriteContents(textureId: ResourceLocation, spriteContents: SpriteContents): Boolean {
        val client = Minecraft.getInstance()
        val textureManager = client.textureManager

        val sprite = findSpriteInAtlas(textureId)

        if (sprite == null) {
            if (logger.isDebugEnabled) {
                logger.debug("Could not find sprite for $textureId")
                logger.debug("Available sprites:")
                logAllSprites()
            }
            logger.error("Could not find sprite in any atlas: $textureId")
            return false
        }

        val oldContents = sprite.contents()
        if (spriteContents.width() != oldContents.width() ||
            spriteContents.height() != oldContents.height()) {
            logger.error("Sprite dimensions mismatch: ${spriteContents.width()}x${spriteContents.height()} vs ${oldContents.width()}x${oldContents.height()}")
            return false
        }

        try {
            (sprite as TextureAtlasSpriteAccessor).setContents(spriteContents)

            val atlas = findAtlasForSprite(textureId)

            if (atlas != null) {
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

    private fun findSpriteInAtlas(textureId: ResourceLocation): TextureAtlasSprite? {
        val client = Minecraft.getInstance()
        val textureManager = client.textureManager

        val possibleKeys = generatePossibleSpriteKeys(textureId)

        val texturesMap = (textureManager as TextureManagerAccessor).getByPath()

        for ((_, texture) in texturesMap) {
            if (texture !is TextureAtlas) continue

            val spritesMap = (texture as TextureAtlasAccessor).getTexturesByName()

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

    private fun generatePossibleSpriteKeys(textureId: ResourceLocation): List<ResourceLocation> {
        val keys = mutableListOf<ResourceLocation>()
        val namespace = textureId.namespace
        val path = textureId.path

        keys.add(textureId)

        if (path.startsWith("textures/")) {
            val withoutTextures = path.removePrefix("textures/")
            keys.add(ResourceLocation.fromNamespaceAndPath(namespace, withoutTextures))
        }

        val filename = path.substringAfterLast('/')
        keys.add(ResourceLocation.fromNamespaceAndPath(namespace, filename))

        if (!path.startsWith("item/")) {
            val filenameOnly = path.substringAfterLast('/')
            keys.add(ResourceLocation.fromNamespaceAndPath(namespace, "item/$filenameOnly"))
        }

        if (!path.startsWith("block/")) {
            val filenameOnly = path.substringAfterLast('/')
            keys.add(ResourceLocation.fromNamespaceAndPath(namespace, "block/$filenameOnly"))
        }

        return keys.distinct()
    }

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

    fun uploadFrameFromMemory(textureId: ResourceLocation, frameIndex: Int): Boolean {
        val spriteContents = SpriteManager.getSpriteContents(textureId, frameIndex) ?: run {
            logger.error("No sprite contents found in memory for $textureId frame $frameIndex")
            return false
        }

        return uploadSpriteContents(textureId, spriteContents)
    }

//    fun uploadByItemId(itemId: ResourceLocation, spriteContents: SpriteContents): Boolean {
//        val textureId = ResourceLocation.fromNamespaceAndPath(itemId.namespace, "textures/item/${itemId.path}")
//        return uploadSpriteContents(textureId, spriteContents)
//    }
}