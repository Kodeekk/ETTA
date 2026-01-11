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

object HotReloadHandler {
    private val logger = LoggerFactory.getLogger("ETTA-HotReload")

    private val pathToIdentifier = mutableMapOf<String, ResourceLocation>()

    private val reloadTexture = ReloadTexture()

    fun registerResourcePack(basePath: Path, namespace: String) {
        pathToIdentifier[basePath.toString()] = ResourceLocation.fromNamespaceAndPath(namespace, "")
        logger.debug("Registered resource pack mapping: $basePath -> $namespace")
    }

    fun handleChanges(changes: Map<Path, FileWatcher.ChangeType>) {
        val affectedTextures = mutableMapOf<ResourceLocation, MutableList<Path>>()

        for ((path, changeType) in changes) {
            val textureId = extractTextureId(path) ?: continue

            affectedTextures.getOrPut(textureId) { mutableListOf() }.add(path)

            logger.debug("Change $changeType: $path -> $textureId")
        }

        for ((textureId, changedFiles) in affectedTextures) {
            try {
                reloadTexture(textureId, changedFiles)
            } catch (e: Exception) {
                logger.error("Failed to hot reload texture: $textureId", e)
            }
        }

        logger.info("Hot reloaded ${affectedTextures.size} textures")
    }

    private fun reloadTexture(textureId: ResourceLocation, changedFiles: List<Path>) {
        logger.info("Hot reloading texture: $textureId")

        val mcmetaxChanged = changedFiles.any { it.toString().endsWith(".mcmetax") }

        if (mcmetaxChanged) {
            reloadAnimationMetadata(textureId, changedFiles)
        }

        val frameChanges = changedFiles.filter {
            it.toString().contains(".etta") && it.toString().contains("frames")
        }

        if (frameChanges.isNotEmpty()) {
            reloadFramesWithReloadTexture(textureId, frameChanges)
        }
    }

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

    private fun reloadFramesWithReloadTexture(textureId: ResourceLocation, frameFiles: List<Path>) {
        logger.info("Reloading ${frameFiles.size} frames for $textureId using ReloadTexture")

        for (framePath in frameFiles) {
            val frameIndex = extractFrameIndex(framePath) ?: continue

            try {
                val pathString = framePath.toString()

                logger.info("Hot reloading frame $frameIndex from: $pathString")

                reloadTexture.onHotReload(pathString, textureId)

                logger.info("✓ Hot reloaded frame $frameIndex for $textureId")
            } catch (e: Exception) {
                logger.error("Failed to reload frame $frameIndex: $framePath", e)
            }
        }
    }

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

    private fun reloadSingleFrame(textureId: ResourceLocation, framePath: Path, frameIndex: Int) {
        if (!Files.exists(framePath)) {
            logger.warn("Frame file not found: $framePath")
            return
        }

        Files.newInputStream(framePath).use { stream ->
            val image = NativeImage.read(stream)

            val spriteContents = SpriteContents(
                ResourceLocation.fromNamespaceAndPath(textureId.namespace, "${textureId.path}_frame_$frameIndex"),
                FrameSize(image.width, image.height),
                image,
                ResourceMetadata.EMPTY
            )

            val success = SpriteUploader.uploadSpriteContents(textureId, spriteContents)

            if (success) {
                logger.info("Hot reloaded frame $frameIndex for $textureId")
            } else {
                spriteContents.close()
                logger.error("Failed to upload frame $frameIndex for $textureId")
            }
        }
    }

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

    fun uploadCustomSpriteContents(textureId: ResourceLocation, spriteContents: SpriteContents): Boolean {
        return try {
            SpriteUploader.uploadSpriteContents(textureId, spriteContents)
        } catch (e: Exception) {
            logger.error("Failed to upload custom sprite contents", e)
            false
        }
    }

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

    private fun extractTextureId(path: Path): ResourceLocation? {
        val pathStr = path.toString().replace('\\', '/')

        val assetsIndex = pathStr.indexOf("/assets/")
        if (assetsIndex == -1) return null

        val afterAssets = pathStr.substring(assetsIndex + 8)
        val parts = afterAssets.split("/")

        if (parts.size < 3) return null

        val namespace = parts[0]
        val remainingPath = parts.drop(1).joinToString("/")

        val texturePath = if (remainingPath.contains(".etta/")) {
            remainingPath.substringBefore(".etta/")
        } else {
            remainingPath
        }

        return ResourceLocation.fromNamespaceAndPath(namespace, texturePath)
    }

    private fun extractFrameIndex(path: Path): Int? {
        val filename = path.fileName.toString()
        return filename.removeSuffix(".png").toIntOrNull()
    }
}