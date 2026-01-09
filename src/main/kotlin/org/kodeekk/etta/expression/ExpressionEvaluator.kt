package org.kodeekk.etta.expression

import net.minecraft.client.Minecraft
import net.minecraft.world.level.LightLayer
import org.kodeekk.etta.events.EventSystem
import org.kodeekk.etta.parser.McmetaxParser
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

/**
 * Enhanced expression evaluator with advanced functions.
 *
 * New functions:
 * - random() - Random value 0.0-1.0
 * - between(val, min, max)
 * - abs(val), min(a,b), max(a,b)
 * - event_start(name), event_end(name)
 * - holding_item(name)
 * - in_biome(name)
 * - has_effect(name)
 */
class ExpressionEvaluator(
    private val expression: String,
    private val parseContext: Any? = null
) {
    private val logger = LoggerFactory.getLogger("ETTA-Expression")

    // State tracking for event transitions
    private val lastEventStates = mutableMapOf<String, Boolean>()
    private var ticksInState = 0
    private var lastEvalResult = false

    fun evaluate(contextVars: Map<String, Any>): Boolean {
        return try {
            val result = evaluateExpression(expression, contextVars)
            val boolResult = toBoolean(result)

            // Track state changes
            if (boolResult != lastEvalResult) {
                ticksInState = 0
                lastEvalResult = boolResult
            } else {
                ticksInState++
            }

            boolResult
        } catch (e: Exception) {
            logger.error("Expression failed: ${e.message}")
            false
        }
    }

    private fun evaluateExpression(expr: String, context: Map<String, Any>): Any {
        val trimmed = expr.trim()

        // Logical OR
        val orParts = splitByOperator(trimmed, "||")
        if (orParts.size > 1) {
            return orParts.any { toBoolean(evaluateExpression(it, context)) }
        }

        // Logical AND
        val andParts = splitByOperator(trimmed, "&&")
        if (andParts.size > 1) {
            return andParts.all { toBoolean(evaluateExpression(it, context)) }
        }

        // Comparisons
        for (op in listOf("<=", ">=", "==", "!=", "<", ">")) {
            val parts = splitByOperator(trimmed, op, maxParts = 2)
            if (parts.size == 2) {
                val left = toNumber(evaluateExpression(parts[0], context))
                val right = toNumber(evaluateExpression(parts[1], context))
                return when (op) {
                    "<" -> left < right
                    ">" -> left > right
                    "<=" -> left <= right
                    ">=" -> left >= right
                    "==" -> left == right
                    "!=" -> left != right
                    else -> false
                }
            }
        }

        // Arithmetic
        for (op in listOf("+", "-", "*", "/")) {
            val parts = splitByOperator(trimmed, op, maxParts = 2)
            if (parts.size == 2) {
                val left = toNumber(evaluateExpression(parts[0], context))
                val right = toNumber(evaluateExpression(parts[1], context))
                return when (op) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> if (right != 0.0) left / right else 0.0
                    else -> 0.0
                }
            }
        }

        // Negation
        if (trimmed.startsWith("!")) {
            return !toBoolean(evaluateExpression(trimmed.substring(1), context))
        }

        // Parentheses
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return evaluateExpression(trimmed.substring(1, trimmed.length - 1), context)
        }

        // Functions
        if (trimmed.contains("(") && trimmed.endsWith(")")) {
            return evaluateFunction(trimmed, context)
        }

        // Variables
        return resolveVariable(trimmed, context)
    }

    private fun evaluateFunction(funcCall: String, context: Map<String, Any>): Any {
        val funcName = funcCall.substringBefore("(").trim()
        val argsStr = funcCall.substringAfter("(").substringBeforeLast(")").trim()
        val args = if (argsStr.isEmpty()) emptyList() else splitArguments(argsStr)

        return when (funcName) {
            // Event functions
            "event" -> {
                if (args.isEmpty()) return false
                val eventName = args[0].trim()
                EventSystem.isEventActive(eventName)
            }

            "event_start" -> {
                if (args.isEmpty()) return false
                val eventName = args[0].trim()
                val currentState = EventSystem.isEventActive(eventName)
                val lastState = lastEventStates[eventName] ?: false
                lastEventStates[eventName] = currentState
                currentState && !lastState // True only on transition false->true
            }

            "event_end" -> {
                if (args.isEmpty()) return false
                val eventName = args[0].trim()
                val currentState = EventSystem.isEventActive(eventName)
                val lastState = lastEventStates[eventName] ?: false
                lastEventStates[eventName] = currentState
                !currentState && lastState // True only on transition true->false
            }

            // Math functions
            "random" -> {
                Random.nextDouble()
            }

            "abs" -> {
                if (args.isEmpty()) return 0.0
                abs(toNumber(evaluateExpression(args[0], context)))
            }

            "min" -> {
                if (args.size < 2) return 0.0
                val a = toNumber(evaluateExpression(args[0], context))
                val b = toNumber(evaluateExpression(args[1], context))
                min(a, b)
            }

            "max" -> {
                if (args.size < 2) return 0.0
                val a = toNumber(evaluateExpression(args[0], context))
                val b = toNumber(evaluateExpression(args[1], context))
                max(a, b)
            }

            "between" -> {
                if (args.size < 3) return false
                val value = toNumber(evaluateExpression(args[0], context))
                val minVal = toNumber(evaluateExpression(args[1], context))
                val maxVal = toNumber(evaluateExpression(args[2], context))
                value >= minVal && value <= maxVal
            }

            // State functions
            "time_in_state" -> {
                ticksInState
            }

            "frame_index" -> {
                context["__current_frame"] ?: 0
            }

            "cycle_count" -> {
                context["__cycle_count"] ?: 0
            }

            // Game state functions
            "holding_item" -> {
                if (args.isEmpty()) return false
                val itemName = args[0].trim()
                val player = Minecraft.getInstance().player ?: return false
                val heldItem = player.mainHandItem
                heldItem.item.toString().lowercase().contains(itemName.lowercase())
            }

            "has_effect" -> {
                if (args.isEmpty()) return false
                val effectName = args[0].trim()
                val player = Minecraft.getInstance().player ?: return false
                player.activeEffects.any {
                    it.effect.toString().lowercase().contains(effectName.lowercase())
                }
            }

            "in_biome" -> {
                if (args.isEmpty()) return false
                val biomeName = args[0].trim()
                val player = Minecraft.getInstance().player ?: return false
                val biome = player.level().getBiome(player.blockPosition())
                biome.toString().lowercase().contains(biomeName.lowercase())
            }

            "armor_value" -> {
                val player = Minecraft.getInstance().player ?: return 0
                player.armorValue
            }

            "light_level" -> {
                val player = Minecraft.getInstance().player ?: return 0
                val sky_light_level = player.level().getBrightness(LightLayer.SKY, player.blockPosition())
                val block_light_level = player.level().getBrightness(LightLayer.BLOCK, player.blockPosition())

                if (sky_light_level <= 0) {
                    block_light_level
                } else if (block_light_level <= 0) {
                    sky_light_level
                }
                else { 0 }
            }

            else -> {
                logger.warn("Unknown function: $funcName")
                false
            }
        }
    }

    private fun splitArguments(argsStr: String): List<String> {
        val args = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0

        for (char in argsStr) {
            when {
                char == '(' -> {
                    parenDepth++
                    current.append(char)
                }
                char == ')' -> {
                    parenDepth--
                    current.append(char)
                }
                char == ',' && parenDepth == 0 -> {
                    args.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            args.add(current.toString().trim())
        }

        return args
    }

    private fun resolveVariable(name: String, context: Map<String, Any>): Any {
        // Strip quotes if present
        val cleanName = name.trim('"', '\'')

        // Check context variables
        when (cleanName) {
            "health" -> return context["__health"] ?: 20.0
            "max_health" -> return context["__max_health"] ?: 20.0
            "hunger" -> return context["__hunger"] ?: 20
            "first_frame" -> return context["__first_frame"] ?: 0
            "last_frame" -> return context["__last_frame"] ?: 0
            "true" -> return true
            "false" -> return false
        }

        // Try parse as number
        cleanName.toDoubleOrNull()?.let { return it }
        cleanName.toIntOrNull()?.let { return it }

        logger.warn("Unknown variable: $cleanName")
        return 0
    }

    private fun splitByOperator(
        expr: String,
        op: String,
        maxParts: Int = Int.MAX_VALUE
    ): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var i = 0

        while (i < expr.length && parts.size < maxParts - 1) {
            val char = expr[i]

            when {
                char == '(' -> {
                    parenDepth++
                    current.append(char)
                    i++
                }
                char == ')' -> {
                    parenDepth--
                    current.append(char)
                    i++
                }
                parenDepth == 0 && expr.substring(i).startsWith(op) -> {
                    parts.add(current.toString().trim())
                    current = StringBuilder()
                    i += op.length
                }
                else -> {
                    current.append(char)
                    i++
                }
            }
        }

        while (i < expr.length) {
            current.append(expr[i])
            i++
        }

        val last = current.toString().trim()
        if (last.isNotEmpty()) {
            parts.add(last)
        }

        return if (parts.isEmpty()) listOf(expr.trim()) else parts
    }

    private fun toBoolean(value: Any): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            else -> false
        }
    }

    private fun toNumber(value: Any): Double {
        return when (value) {
            is Number -> value.toDouble()
            is Boolean -> if (value) 1.0 else 0.0
            else -> 0.0
        }
    }
}