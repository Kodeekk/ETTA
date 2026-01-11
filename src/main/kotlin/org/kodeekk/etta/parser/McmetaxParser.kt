package org.kodeekk.etta.parser

import net.minecraft.resources.ResourceLocation
import org.kodeekk.etta.core.AnimationMetadata
import org.kodeekk.etta.core.AnimationSource
import org.kodeekk.etta.core.AnimationSegment
import org.kodeekk.etta.expression.ExpressionEvaluator
import org.slf4j.LoggerFactory

object McmetaxParser {
    private val logger = LoggerFactory.getLogger("ETTA-McmetaxParser")

    fun parse(content: String, textureId: ResourceLocation): AnimationMetadata? {
        return try {
            val context = ParseContext()
            val lines = preprocessLines(content)

            var currentSection: Section? = null
            var currentProperties = mutableMapOf<String, String>()
            var lineNum = 0

            for (line in lines) {
                lineNum++
                try {
                    when {
                        line.startsWith("[") && line.endsWith("]") -> {
                            currentSection?.let {
                                processSection(it, currentProperties, context, textureId)
                            }

                            currentSection = parseSection(line)
                            currentProperties.clear()
                        }
                        line.contains(":") -> {
                            val (key, value) = line.split(":", limit = 2)
                            val trimmedKey = key.trim()
                            val trimmedValue = value.trim()

                            if (currentProperties.containsKey(trimmedKey)) {
                                currentProperties[trimmedKey] =
                                    currentProperties[trimmedKey]!! + " " + trimmedValue
                            } else {
                                currentProperties[trimmedKey] = trimmedValue
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error on line $lineNum: $line", e)
                }
            }

            currentSection?.let {
                processSection(it, currentProperties, context, textureId)
            }

            if (context.segments.isEmpty()) {
                logger.error("No segments found for $textureId")
                return null
            }

            val fallbackCount = context.segments.count {
                it is AnimationSegment.FallbackSegment
            }
            if (fallbackCount > 1) {
                logger.error("Multiple fallbacks in $textureId")
                return null
            }

            logger.info("Parsed ${context.segments.size} segments for $textureId")

            AnimationMetadata(
                textureId = textureId,
                frametime = context.frametime,
                interpolate = context.interpolate,
                width = null,
                height = null,
                segments = context.segments,
                source = AnimationSource.MCMETAX
            )
        } catch (e: Exception) {
            logger.error("Failed to parse mcmetax for $textureId", e)
            null
        }
    }

    private fun preprocessLines(content: String): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (rawLine in content.lines()) {
            val line = rawLine.split("#")[0].trimEnd()

            if (line.isEmpty()) continue

            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (!line.trim().startsWith("[")) {
                    currentLine.append(" ").append(line.trim())
                    continue
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }

            currentLine = StringBuilder(line.trim())
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }

    private fun parseSection(line: String): Section {
        val content = line.removeSurrounding("[", "]").trim()

        return when {
            content == "animation" -> Section.Animation
            content == "variables" -> Section.Variables
            content == "conditions" -> Section.Conditions
            content == "fallback" -> Section.Fallback
            content.startsWith("segment:") -> {
                val name = content.removePrefix("segment:").trim()
                Section.Segment(name)
            }
            else -> Section.Unknown
        }
    }

    private fun processSection(
        section: Section,
        properties: Map<String, String>,
        context: ParseContext,
        textureId: ResourceLocation
    ) {
        when (section) {
            is Section.Animation -> {
                context.frametime = properties["frametime"]?.toIntOrNull() ?: 1
                context.interpolate = properties["interpolate"]?.toBooleanStrictOrNull() ?: false
                properties["max_fps"]?.toIntOrNull()?.let { context.maxFps = it }
            }

            is Section.Variables -> {
                properties.forEach { (key, value) ->
                    context.variables[key] = value
                }
            }

            is Section.Conditions -> {
                properties.forEach { (key, value) ->
                    context.namedConditions[key] = value
                }
            }

            is Section.Fallback -> {
                val frame = parseFrameSpec(
                    properties["frame"] ?: "0",
                    context
                ).firstOrNull() ?: 0

                context.segments.add(
                    AnimationSegment.FallbackSegment(
                        name = "fallback",
                        frameIndex = frame,
                        priority = -1000,
                        frametime = properties["frametime"]?.toIntOrNull()
                    )
                )
            }

            is Section.Segment -> {
                parseSegment(section.name, properties, context)?.let {
                    context.segments.add(it)
                }
            }

            Section.Unknown -> {}
        }
    }

    private fun parseSegment(
        name: String,
        props: Map<String, String>,
        context: ParseContext
    ): AnimationSegment? {
        val type = props["type"]?.lowercase() ?: "sequence"
        val priority = props["priority"]?.toIntOrNull() ?: 10
        val frametime = props["frametime"]?.toIntOrNull()
        val whenExpr = expandVariables(props["when"] ?: "", context)

        val evaluator = if (whenExpr.isNotEmpty()) {
            try {
                ExpressionEvaluator(whenExpr, context)
            } catch (e: Exception) {
                logger.error("Failed to parse expression: $whenExpr", e)
                null
            }
        } else null

        return when (type) {
            "single" -> {
                val frame = parseFrameSpec(
                    props["frame"] ?: "0",
                    context
                ).firstOrNull() ?: 0

                AnimationSegment.SingleFrameSegment(
                    name = name,
                    frameIndex = frame,
                    eventName = null,
                    expression = evaluator,
                    priority = priority,
                    frametime = frametime
                )
            }

            "sequence" -> {
                val frames = parseFrameSpec(props["frames"] ?: "0-0", context)
                val loop = props["loop"]?.toBooleanStrictOrNull() ?: true
                val pause = props["pause_on_last"]?.toBooleanStrictOrNull() ?: false

                AnimationSegment.SequenceSegment(
                    name = name,
                    firstFrameIndex = frames.firstOrNull() ?: 0,
                    lastFrameIndex = frames.lastOrNull() ?: 0,
                    loop = loop,
                    pauseOnLastFrame = pause,
                    eventName = null,
                    expression = evaluator,
                    priority = priority,
                    frametime = frametime
                )
            }

            "weighted" -> {
                val frames = parseFrameSpec(props["frames"] ?: "[0]", context)
                // TODO: Implement proper weighted segment type
                AnimationSegment.SequenceSegment(
                    name = name,
                    firstFrameIndex = frames.firstOrNull() ?: 0,
                    lastFrameIndex = frames.lastOrNull() ?: 0,
                    loop = true,
                    pauseOnLastFrame = false,
                    eventName = null,
                    expression = evaluator,
                    priority = priority,
                    frametime = frametime
                )
            }

            "conditional" -> {
                val frames = parseFrameSpec(props["frames"] ?: "0-0", context)

                AnimationSegment.SequenceSegment(
                    name = name,
                    firstFrameIndex = frames.firstOrNull() ?: 0,
                    lastFrameIndex = frames.lastOrNull() ?: 0,
                    loop = true,
                    pauseOnLastFrame = false,
                    eventName = null,
                    expression = evaluator,
                    priority = priority,
                    frametime = frametime
                )
            }

            "transition" -> {
                val frames = parseFrameSpec(props["frames"] ?: "0-0", context)

                AnimationSegment.OneShotSegment(
                    name = name,
                    firstFrameIndex = frames.firstOrNull() ?: 0,
                    lastFrameIndex = frames.lastOrNull() ?: 0,
                    eventName = null,
                    expression = evaluator,
                    priority = priority,
                    frametime = frametime
                )
            }

            "oneshot" -> {
                val frames = parseFrameSpec(props["frames"] ?: "0-0", context)

                AnimationSegment.OneShotSegment(
                    name = name,
                    firstFrameIndex = frames.firstOrNull() ?: 0,
                    lastFrameIndex = frames.lastOrNull() ?: 0,
                    eventName = null,
                    expression = evaluator,
                    priority = priority,
                    frametime = frametime
                )
            }

            else -> {
                logger.error("Unknown segment type: $type")
                null
            }
        }
    }

    private fun parseFrameSpec(spec: String, context: ParseContext): List<Int> {
        val expanded = expandVariables(spec, context)
        val frames = mutableListOf<Int>()

        if (expanded.startsWith("[") && expanded.endsWith("]")) {
            val content = expanded.substring(1, expanded.length - 1)
            val parts = content.split(",")

            for (part in parts) {
                frames.addAll(parseFramePart(part.trim(), context))
            }
        } else {
            frames.addAll(parseFramePart(expanded, context))
        }

        return frames.distinct().sorted()
    }

    private fun parseFramePart(part: String, context: ParseContext): List<Int> {
        if (part.contains(":")) {
            val (range, stepStr) = part.split(":", limit = 2)
            val step = stepStr.toIntOrNull() ?: 1
            return parseFramePart(range, context).filterIndexed { i, _ -> i % step == 0 }
        }

        if (part.contains("-")) {
            val parts = part.split("-", limit = 2)
            val start = parts[0].trim().toIntOrNull() ?: 0
            val end = parts[1].trim().toIntOrNull() ?: 0
            return (start..end).toList()
        }

        val num = part.toIntOrNull()
        return if (num != null) listOf(num) else emptyList()
    }

    private fun expandVariables(text: String, context: ParseContext): String {
        var result = text

        for ((key, value) in context.variables) {
            result = result.replace("$$key", value)
        }

        for ((key, value) in context.namedConditions) {
            result = result.replace("$$key", "($value)")
        }

        return result
    }

    private sealed class Section {
        object Animation : Section()
        object Variables : Section()
        object Conditions : Section()
        object Fallback : Section()
        data class Segment(val name: String) : Section()
        object Unknown : Section()
    }

    private class ParseContext {
        var frametime = 1
        var interpolate = false
        var maxFps = 60
        val variables = mutableMapOf<String, String>()
        val namedConditions = mutableMapOf<String, String>()
        val segments = mutableListOf<AnimationSegment>()
    }
}