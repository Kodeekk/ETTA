package org.kodeekk.etta.expression

import org.slf4j.LoggerFactory
import kotlin.math.pow
import kotlin.text.iterator

/**
 * Evaluates mcmetax expression blocks using recursive descent parsing.
 *
 * Supports:
 * - Variables (__health, __max_health, etc.)
 * - Constants (const/mut declarations)
 * - Conditionals (if/when)
 * - Operators with proper precedence
 * - Evaluation with eval()
 */
class ExpressionEvaluator(private val expressionCode: String) {
    private val logger = LoggerFactory.getLogger("ETTA-Expression")
    private val variables = mutableMapOf<String, Any>()

    /**
     * Evaluates the expression with given context variables.
     * Returns true/false for boolean results, or false on error.
     */
    fun evaluate(contextVars: Map<String, Any>): Boolean {
        return try {
            variables.clear()
            variables.putAll(contextVars)

            logger.debug("Evaluating expression with context: $contextVars")
            val result = evaluateBlock(expressionCode)

            // Convert result to boolean
            val boolResult = when (result) {
                is Boolean -> result
                is Number -> result.toDouble() != 0.0
                null -> false
                else -> true
            }

            logger.debug("Expression result: $boolResult (raw: $result)")
            boolResult
        } catch (e: Exception) {
            logger.error("Expression evaluation failed: ${e.message}", e)
            false
        }
    }

    private fun evaluateBlock(code: String): Any? {
        val trimmed = code.trim().removeSurrounding("{", "}")
        val statements = splitStatements(trimmed)

        var lastResult: Any? = null

        for (statement in statements) {
            val stmt = statement.trim()
            if (stmt.isEmpty()) continue

            lastResult = when {
                stmt.startsWith("eval(") -> {
                    val expr = extractFunctionArg(stmt, "eval")
                    return evaluateExpression(expr) // Return immediately
                }
                stmt.startsWith("const ") -> {
                    handleVarDeclaration(stmt.removePrefix("const "))
                    null
                }
                stmt.startsWith("mut ") -> {
                    handleVarDeclaration(stmt.removePrefix("mut "))
                    null
                }
                stmt.startsWith("if ") -> handleIf(stmt)
                stmt.startsWith("when ") -> handleWhen(stmt)
                else -> evaluateExpression(stmt)
            }
        }

        return lastResult ?: false
    }

    private fun splitStatements(code: String): List<String> {
        val statements = mutableListOf<String>()
        var current = StringBuilder()
        var braceDepth = 0
        var inString = false
        var escapeNext = false

        for (char in code) {
            when {
                escapeNext -> {
                    current.append(char)
                    escapeNext = false
                }
                char == '\\' && inString -> {
                    current.append(char)
                    escapeNext = true
                }
                char == '"' && !escapeNext -> inString = !inString
                char == '{' && !inString -> {
                    braceDepth++
                    current.append(char)
                }
                char == '}' && !inString -> {
                    braceDepth--
                    current.append(char)
                }
                char == ';' && braceDepth == 0 && !inString -> {
                    if (current.isNotBlank()) {
                        statements.add(current.toString())
                    }
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }

        if (current.isNotBlank()) {
            statements.add(current.toString())
        }

        return statements
    }

    private fun handleVarDeclaration(stmt: String) {
        val parts = stmt.split("=", limit = 2)
        if (parts.size == 2) {
            val name = parts[0].trim()
            val value = evaluateExpression(parts[1].trim()) ?: return
            variables[name] = value
        }
    }

    private fun handleIf(stmt: String): Any? {
        // if condition { block } else { block }
        val conditionStart = stmt.indexOf("if") + 2
        val conditionEnd = findMatchingBrace(stmt, conditionStart)

        if (conditionEnd == -1) return null

        val condition = stmt.substring(conditionStart, conditionEnd).trim()
        val rest = stmt.substring(conditionEnd)

        val blocks = extractBlocks(rest)
        if (blocks.isEmpty()) return null

        val conditionResult = evaluateExpression(condition) as? Boolean ?: false

        return if (conditionResult) {
            evaluateBlock(blocks[0])
        } else if (blocks.size > 1) {
            evaluateBlock(blocks[1])
        } else {
            null
        }
    }

    private fun handleWhen(stmt: String): Any? {
        val parts = stmt.removePrefix("when ").split("{", limit = 2)
        if (parts.size < 2) return null

        val varName = parts[0].trim()
        val varValue = variables[varName] ?: return null

        val casesBlock = parts[1].trim().removeSuffix("}")
        val cases = splitCases(casesBlock)

        for (case in cases) {
            if (!case.contains("=>")) continue

            val (pattern, action) = case.split("=>", limit = 2)
            val patternTrimmed = pattern.trim()

            if (patternTrimmed == "rest__") {
                return evaluateBlock(action.trim())
            }

            val matches = when (varValue) {
                is Number -> patternTrimmed.toIntOrNull() == varValue.toInt()
                is String -> patternTrimmed.trim('"', '\'') == varValue
                else -> false
            }

            if (matches) {
                return evaluateBlock(action.trim())
            }
        }

        return null
    }

    private fun splitCases(casesBlock: String): List<String> {
        val cases = mutableListOf<String>()
        var current = StringBuilder()
        var braceDepth = 0

        for (char in casesBlock) {
            when (char) {
                '{' -> {
                    braceDepth++
                    current.append(char)
                }
                '}' -> {
                    braceDepth--
                    current.append(char)
                }
                ',' -> {
                    if (braceDepth == 0) {
                        cases.add(current.toString())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }

        if (current.isNotBlank()) {
            cases.add(current.toString())
        }

        return cases
    }

    private fun findMatchingBrace(code: String, startPos: Int): Int {
        var pos = startPos
        while (pos < code.length && code[pos].isWhitespace()) pos++
        if (pos >= code.length || code[pos] != '{') return pos

        var depth = 0
        while (pos < code.length) {
            when (code[pos]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return pos
                }
            }
            pos++
        }
        return pos
    }

    private fun extractBlocks(code: String): List<String> {
        val blocks = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        var started = false

        for (char in code) {
            when (char) {
                '{' -> {
                    if (depth == 0) started = true
                    depth++
                    current.append(char)
                }
                '}' -> {
                    depth--
                    current.append(char)
                    if (depth == 0 && started) {
                        blocks.add(current.toString())
                        current = StringBuilder()
                        started = false
                    }
                }
                else -> if (started) current.append(char)
            }
        }

        return blocks
    }

    private fun extractFunctionArg(stmt: String, funcName: String): String {
        val start = stmt.indexOf("$funcName(") + funcName.length + 1
        val end = stmt.lastIndexOf(')')
        return if (end > start) stmt.substring(start, end) else ""
    }

    /**
     * Evaluates a single expression with proper operator precedence.
     * Uses recursive descent parsing.
     */
    private fun evaluateExpression(expr: String): Any? {
        val trimmed = expr.trim()

        // Handle constants
        when (trimmed) {
            "true" -> return true
            "false" -> return false
            "first_frame__" -> return variables["__first_frame"] ?: 0
            "last_frame__" -> return variables["__last_frame"] ?: 0
        }

        // Handle variables
        if (variables.containsKey(trimmed)) {
            return variables[trimmed]
        }

        // Handle string literals
        if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Handle numbers
        trimmed.toIntOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // Parse with operator precedence: OR -> AND -> Comparison -> Arithmetic
        return parseLogicalOr(trimmed)
    }

    // Operator precedence levels (lowest to highest):
    // 1. Logical OR (||)
    // 2. Logical AND (&&)
    // 3. Comparison (==, !=, <, >, <=, >=)
    // 4. Addition/Subtraction (+, -)
    // 5. Multiplication/Division (*, /)
    // 6. Power (^)
    // 7. Unary (-, !)
    // 8. Primary (numbers, variables, parentheses)

    private fun parseLogicalOr(expr: String): Any? {
        val parts = splitByOperator(expr, "||")
        if (parts.size > 1) {
            val left = parseLogicalAnd(parts[0]) as? Boolean ?: false
            val right = parseLogicalAnd(parts.drop(1).joinToString("||")) as? Boolean ?: false
            return left || right
        }
        return parseLogicalAnd(expr)
    }

    private fun parseLogicalAnd(expr: String): Any? {
        val parts = splitByOperator(expr, "&&")
        if (parts.size > 1) {
            val left = parseComparison(parts[0]) as? Boolean ?: false
            val right = parseComparison(parts.drop(1).joinToString("&&")) as? Boolean ?: false
            return left && right
        }
        return parseComparison(expr)
    }

    private fun parseComparison(expr: String): Any? {
        // Try each comparison operator
        for (op in listOf("==", "!=", "<=", ">=", "<", ">")) {
            val parts = splitByOperator(expr, op)
            if (parts.size == 2) {
                val left = parseAdditive(parts[0])?.toDouble() ?: continue
                val right = parseAdditive(parts[1])?.toDouble() ?: continue

                return when (op) {
                    "==" -> left == right
                    "!=" -> left != right
                    "<" -> left < right
                    ">" -> left > right
                    "<=" -> left <= right
                    ">=" -> left >= right
                    else -> null
                }
            }
        }
        return parseAdditive(expr)
    }

    private fun parseAdditive(expr: String): Any? {
        for (op in listOf("+", "-")) {
            val parts = splitByOperator(expr, op)
            if (parts.size > 1) {
                var result = parseMultiplicative(parts[0])?.toDouble() ?: continue
                for (i in 1 until parts.size) {
                    val operand = parseMultiplicative(parts[i])?.toDouble() ?: break
                    result = if (op == "+") result + operand else result - operand
                }
                return result
            }
        }
        return parseMultiplicative(expr)
    }

    private fun parseMultiplicative(expr: String): Any? {
        for (op in listOf("*", "/")) {
            val parts = splitByOperator(expr, op)
            if (parts.size > 1) {
                var result = parsePower(parts[0])?.toDouble() ?: continue
                for (i in 1 until parts.size) {
                    val operand = parsePower(parts[i])?.toDouble() ?: break
                    result = if (op == "*") result * operand else result / operand
                }
                return result
            }
        }
        return parsePower(expr)
    }

    private fun parsePower(expr: String): Any? {
        val parts = splitByOperator(expr, "^")
        if (parts.size == 2) {
            val base = parseUnary(parts[0])?.toDouble() ?: return null
            val exp = parseUnary(parts[1])?.toDouble() ?: return null
            return base.pow(exp)
        }
        return parseUnary(expr)
    }

    private fun parseUnary(expr: String): Any? {
        val trimmed = expr.trim()

        if (trimmed.startsWith("-")) {
            val operand = parsePrimary(trimmed.substring(1))?.toDouble() ?: return null
            return -operand
        }

        if (trimmed.startsWith("!")) {
            val operand = parsePrimary(trimmed.substring(1)) as? Boolean ?: return null
            return !operand
        }

        return parsePrimary(trimmed)
    }

    private fun parsePrimary(expr: String): Any? {
        val trimmed = expr.trim()

        // Parentheses
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return evaluateExpression(trimmed.substring(1, trimmed.length - 1))
        }

        // Try to evaluate as base expression
        return evaluateExpression(trimmed)
    }

    private fun splitByOperator(expr: String, op: String): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var i = 0

        while (i < expr.length) {
            val char = expr[i]

            when {
                char == '(' -> {
                    parenDepth++
                    current.append(char)
                }
                char == ')' -> {
                    parenDepth--
                    current.append(char)
                }
                parenDepth == 0 && expr.substring(i).startsWith(op) -> {
                    parts.add(current.toString())
                    current = StringBuilder()
                    i += op.length - 1
                }
                else -> current.append(char)
            }

            i++
        }

        parts.add(current.toString())
        return parts.filter { it.isNotBlank() }
    }

    private fun Any.toDouble(): Double? {
        return when (this) {
            is Number -> this.toDouble()
            is String -> this.toDoubleOrNull()
            else -> null
        }
    }
}