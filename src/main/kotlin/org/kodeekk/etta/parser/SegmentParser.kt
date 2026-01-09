package org.kodeekk.etta.parser

import org.kodeekk.etta.core.AnimationSegment
import org.kodeekk.etta.expression.ExpressionEvaluator
import org.slf4j.LoggerFactory

/**
 * Parses animation segments from mcmetax content.
 */
object SegmentParser {
    private val logger = LoggerFactory.getLogger("ETTA-SegmentParser")

    /**
     * Parses all segments from a segments block.
     */
    fun parseSegments(content: String): List<AnimationSegment> {
        val segments = mutableListOf<AnimationSegment>()
        var counter = 0
        var pos = 0

        logger.debug("Starting segment parsing, content length: ${content.length}")

        while (pos < content.length) {
            val segmentStart = content.indexOf("segment!", pos)
            if (segmentStart == -1) break

            logger.debug("Found segment at position $segmentStart")

            try {
                val (segment, endPos) = parseSegmentAt(content, segmentStart, counter++)
                if (segment != null) {
                    logger.debug("Successfully parsed segment: ${segment.name}")
                    segments.add(segment)
                    pos = endPos
                } else {
                    logger.warn("Failed to parse segment at position $segmentStart")
                    pos = segmentStart + 8
                }
            } catch (e: Exception) {
                logger.error("Failed to parse segment at position $segmentStart: ${e.message}", e)
                pos = segmentStart + 8
            }
        }

        logger.debug("Finished parsing, found ${segments.size} segments")
        return segments
    }

    /**
     * Parses a single segment starting at the given position.
     */
    private fun parseSegmentAt(content: String, startPos: Int, nameIndex: Int): Pair<AnimationSegment?, Int> {
        var pos = startPos + 8 // Skip "segment!"

        // Parse segment type
        val typeEnd = findNextSpecialChar(content, pos, setOf('@', '{', ' ', '\n', '\r', '\t'))
        val segmentTypeStr = content.substring(pos, typeEnd).trim()
        logger.debug("Parsing segment type: '$segmentTypeStr'")

        val segmentType = SegmentType.fromString(segmentTypeStr)
        if (segmentType == null) {
            logger.warn("Unknown segment type: $segmentTypeStr")
            return null to (pos + 1)
        }

        pos = typeEnd

        // Skip whitespace
        while (pos < content.length && content[pos].isWhitespace()) pos++

        // Parse corrector (@event or @expression)
        var correctorType: String? = null
        var correctorArg: String? = null

        if (pos < content.length && content[pos] == '@') {
            pos++ // Skip '@'

            val correctorTypeEnd = content.indexOf('(', pos)
            if (correctorTypeEnd != -1) {
                correctorType = content.substring(pos, correctorTypeEnd).trim()
                logger.debug("Found corrector type: $correctorType")
                pos = correctorTypeEnd + 1 // Skip '('

                // Extract corrector argument
                val (arg, argEnd) = extractBalancedParens(content, pos)
                correctorArg = arg
                logger.debug("Corrector argument length: ${arg.length}")
                pos = argEnd

                // Skip closing paren
                if (pos < content.length && content[pos] == ')') pos++
            }
        }

        // Skip whitespace
        while (pos < content.length && content[pos].isWhitespace()) pos++

        // Parse properties block
        if (pos >= content.length || content[pos] != '{') {
            logger.warn("Expected '{' for properties block at position $pos")
            return null to pos
        }

        pos++ // Skip opening '{'
        val (propertiesBlock, propertiesEnd) = extractBalancedBraces(content, pos)
        pos = propertiesEnd

        // Skip closing '}'
        if (pos < content.length && content[pos] == '}') pos++

        // Parse properties
        val properties = parseProperties(propertiesBlock)
        logger.debug("Parsed properties: $properties")

        // Create segment
        val segment = createSegment(
            segmentType = segmentType,
            properties = properties,
            correctorType = correctorType,
            correctorArg = correctorArg,
            defaultName = "segment_$nameIndex"
        )

        return segment to pos
    }

    /**
     * Creates an AnimationSegment from parsed data.
     */
    private fun createSegment(
        segmentType: SegmentType,
        properties: Map<String, String>,
        correctorType: String?,
        correctorArg: String?,
        defaultName: String
    ): AnimationSegment? {
        val name = properties["name"] ?: defaultName
        val priority = properties["priority"]?.toIntOrNull() ?: 10
        val frametime = properties["frametime"]?.toIntOrNull()

        // Parse corrector
        val eventName: String? = if (correctorType == "event") {
            correctorArg?.trim('"', '\'', ' ')
        } else null

        val expression: ExpressionEvaluator? = if (correctorType == "expression") {
            correctorArg?.let { ExpressionEvaluator(it) }
        } else null

        return when (segmentType) {
            SegmentType.FALLBACK_FRAME -> {
                val frameIndex = properties["frame_index"]?.toIntOrNull() ?: 0
                AnimationSegment.FallbackSegment(
                    name = name,
                    frameIndex = frameIndex,
                    priority = -1000,
                    frametime = frametime
                )
            }

            SegmentType.SINGLE_FRAME -> {
                val frameIndex = properties["frame_index"]?.toIntOrNull() ?: 0
                AnimationSegment.SingleFrameSegment(
                    name = name,
                    frameIndex = frameIndex,
                    eventName = eventName,
                    expression = expression,
                    priority = priority,
                    frametime = frametime
                )
            }

            SegmentType.SEQUENCE -> {
                val firstFrame = properties["first_frame_index"]?.toIntOrNull() ?: 0
                val lastFrame = properties["last_frame_index"]?.toIntOrNull() ?: 0
                val loop = properties["loop"]?.toBooleanStrictOrNull() ?: true
                val pauseOnLastFrame = properties["pause_on_last_frame"]?.toBooleanStrictOrNull() ?: false

                AnimationSegment.SequenceSegment(
                    name = name,
                    firstFrameIndex = firstFrame,
                    lastFrameIndex = lastFrame,
                    loop = loop,
                    pauseOnLastFrame = pauseOnLastFrame,
                    eventName = eventName,
                    expression = expression,
                    priority = priority,
                    frametime = frametime
                )
            }

            SegmentType.ONESHOT -> {
                val firstFrame = properties["first_frame_index"]?.toIntOrNull() ?: 0
                val lastFrame = properties["last_frame_index"]?.toIntOrNull() ?: 0

                AnimationSegment.OneShotSegment(
                    name = name,
                    firstFrameIndex = firstFrame,
                    lastFrameIndex = lastFrame,
                    eventName = eventName,
                    expression = expression,
                    priority = priority,
                    frametime = frametime
                )
            }
        }
    }

    /**
     * Extracts content between balanced braces.
     */
    private fun extractBalancedBraces(content: String, startPos: Int): Pair<String, Int> {
        return extractBalanced(content, startPos, '{', '}')
    }

    /**
     * Extracts content between balanced parentheses.
     */
    private fun extractBalancedParens(content: String, startPos: Int): Pair<String, Int> {
        return extractBalanced(content, startPos, '(', ')')
    }

    /**
     * Generic balanced delimiter extraction.
     */
    private fun extractBalanced(content: String, startPos: Int, open: Char, close: Char): Pair<String, Int> {
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
                !inString && char == open -> {
                    depth++
                    builder.append(char)
                }
                !inString && char == close -> {
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

    /**
     * Finds the next occurrence of any special character.
     */
    private fun findNextSpecialChar(content: String, startPos: Int, chars: Set<Char>): Int {
        var pos = startPos
        while (pos < content.length && content[pos] !in chars) {
            pos++
        }
        return pos
    }

    /**
     * Parses properties from a properties block.
     */
    private fun parseProperties(block: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()

        // Split by semicolons or commas
        val lines = block.split(Regex("[;,]"))

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val parts = trimmed.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                properties[key] = value
            }
        }

        return properties
    }
}