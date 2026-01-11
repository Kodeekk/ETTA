package org.kodeekk.etta.parser

import com.google.gson.JsonParser
import org.kodeekk.etta.core.*
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object McmetaParser {
    private val logger = LoggerFactory.getLogger("ETTA-McmetaParser")

    fun parse(content: String, textureId: ResourceLocation): AnimationMetadata? {
        return try {
            val json = JsonParser.parseString(content).asJsonObject
            val animation = json.getAsJsonObject("animation") ?: return null

            val frametime = animation.get("frametime")?.asInt ?: 1
            val interpolate = animation.get("interpolate")?.asBoolean ?: false

            val frames = if (animation.has("frames")) {
                animation.getAsJsonArray("frames")
            } else null

            val frameCount = frames?.size() ?: 0

            val segment = AnimationSegment.SequenceSegment(
                name = "default",
                firstFrameIndex = 0,
                lastFrameIndex = if (frameCount > 0) frameCount - 1 else 0,
                loop = true,
                pauseOnLastFrame = false,
                eventName = null,
                expression = null,
                priority = 0,
                frametime = null
            )

            AnimationMetadata(
                textureId = textureId,
                frametime = frametime,
                interpolate = interpolate,
                segments = listOf(segment),
                source = AnimationSource.MCMETA
            )
        } catch (e: Exception) {
            logger.error("Failed to parse mcmeta for $textureId", e)
            null
        }
    }
}