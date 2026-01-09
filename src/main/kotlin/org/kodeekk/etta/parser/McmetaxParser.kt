package org.kodeekk.etta.parser

import net.minecraft.resources.ResourceLocation
import org.kodeekk.etta.core.AnimationMetadata
import org.kodeekk.etta.core.AnimationSource
import org.slf4j.LoggerFactory

/**
 * Parses .mcmetax files (ETTA animation format).
 */
object McmetaxParser {
    private val logger = LoggerFactory.getLogger("ETTA-McmetaxParser")

    /**
     * Parses mcmetax content into AnimationMetadata.
     */
    fun parse(content: String, textureId: ResourceLocation): AnimationMetadata? {
        return try {
            logger.debug("Parsing mcmetax for: $textureId")

            // Find animation block
            val animationBlock = extractAnimationBlock(content)
            if (animationBlock == null) {
                logger.warn("No animation block found in mcmetax for $textureId")
                return null
            }

            // Parse frametime
            val frametime = extractFrametime(animationBlock)
            logger.debug("Frametime: $frametime")

            // Parse segments
            val segmentsBlock = extractSegmentsBlock(animationBlock)
            if (segmentsBlock == null) {
                logger.error("No segments block found in mcmetax for $textureId")
                return null
            }

            val segments = SegmentParser.parseSegments(segmentsBlock)
            if (segments.isEmpty()) {
                logger.error("No valid segments parsed from mcmetax for $textureId")
                return null
            }

            // Validate: only one FALLBACK allowed
            val fallbackCount = segments.count {
                it is org.kodeekk.etta.core.AnimationSegment.FallbackSegment
            }
            if (fallbackCount > 1) {
                logger.error("Multiple FALLBACK_FRAME segments defined in $textureId (only one allowed)")
                return null
            }

            logger.info("Successfully parsed ${segments.size} segments for $textureId")

            AnimationMetadata(
                textureId = textureId,
                frametime = frametime                                  ,
                interpolate = false,
                width = null,
                height = null,
                segments = segments,
                source = AnimationSource.MCMETAX
            )
        } catch (e: Exception) {
            logger.error("Failed to parse mcmetax for $textureId", e)
            null
        }
    }

    /**
     * Extracts the animation block from the content.
     */
    private fun extractAnimationBlock(content: String): String? {
        val animationStart = content.indexOf("\"animation\"")
        if (animationStart == -1) return null

        // Find the opening brace
        var pos = animationStart
        while (pos < content.length && content[pos] != '{') pos++
        if (pos >= content.length) return null

        // Extract balanced content
        val (block, _) = extractBalancedBraces(content, pos + 1)
        return block
    }

    /**
     * Extracts frametime value from animation block.
     */
    private fun extractFrametime(block: String): Int {
        val frametimeRegex = Regex("""frametime\s*=\s*(\d+)""")
        val match = frametimeRegex.find(block)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    /**
     * Extracts the segments block from animation block.
     */
    private fun extractSegmentsBlock(animationBlock: String): String? {
        val segmentsStart = animationBlock.indexOf("segments")
        if (segmentsStart == -1) return null

        // Skip to '='
        var pos = segmentsStart + 8
        while (pos < animationBlock.length && animationBlock[pos].isWhitespace()) pos++
        if (pos >= animationBlock.length || animationBlock[pos] != '=') return null
        pos++

        // Skip to '{'
        while (pos < animationBlock.length && animationBlock[pos].isWhitespace()) pos++
        if (pos >= animationBlock.length || animationBlock[pos] != '{') return null

        // Extract balanced content
        val (block, _) = extractBalancedBraces(animationBlock, pos + 1)
        return block
    }

    /**
     * Extracts content between balanced braces.
     * Returns (content, endPosition).
     */
    private fun extractBalancedBraces(content: String, startPos: Int): Pair<String, Int> {
        val builder = StringBuilder()
        var depth = 1
        var pos = startPos
        var inString = false
        var escapeNext = false

        while (pos < content.length && depth > 0) {
            val char = content[pos]

            when {
                escapeNext -> {
                    builder.append(char)
                    escapeNext = false
                }
                char == '\\' && inString -> {
                    builder.append(char)
                    escapeNext = true
                }
                char == '"' -> {
                    builder.append(char)
                    inString = !inString
                }
                !inString && char == '{' -> {
                    depth++
                    builder.append(char)
                }
                !inString && char == '}' -> {
                    depth--
                    if (depth > 0) {
                        builder.append(char)
                    }
                }
                else -> builder.append(char)
            }

            pos++
        }

        return Pair(builder.toString(), pos)
    }
}