package org.kodeekk.etta.core

import org.kodeekk.etta.expression.ExpressionEvaluator


sealed class AnimationSegment {
    abstract val name: String
    abstract val priority: Int
    abstract val frametime: Int?

    data class FallbackSegment(
        override val name: String = "fallback",
        val frameIndex: Int,
        override val priority: Int = -1000,
        override val frametime: Int? = null
    ) : AnimationSegment()

    data class SingleFrameSegment(
        override val name: String,
        val frameIndex: Int,
        val eventName: String? = null,
        val expression: ExpressionEvaluator? = null,
        override val priority: Int = 10,
        override val frametime: Int? = null
    ) : AnimationSegment()

    data class SequenceSegment(
        override val name: String,
        val firstFrameIndex: Int,
        val lastFrameIndex: Int,
        val loop: Boolean = true,
        val pauseOnLastFrame: Boolean = false, // Deprecated
        val eventName: String? = null,
        val expression: ExpressionEvaluator? = null,
        override val priority: Int = 10,
        override val frametime: Int? = null
    ) : AnimationSegment()

    data class OneShotSegment(
        override val name: String,
        val firstFrameIndex: Int,
        val lastFrameIndex: Int,
        val eventName: String? = null,
        val expression: ExpressionEvaluator? = null,
        override val priority: Int = 10,
        override val frametime: Int? = null
    ) : AnimationSegment()

    fun shouldBeActive(variables: Map<String, Any>): Boolean {
        return when (this) {
            is FallbackSegment -> true
            is SingleFrameSegment -> {
                expression?.evaluate(variables) ?: (eventName == null)
            }
            is SequenceSegment -> {
                expression?.evaluate(variables) ?: (eventName == null)
            }
            is OneShotSegment -> {
                expression?.evaluate(variables) ?: (eventName == null)
            }
        }
    }

    fun getFrameRange(): IntRange {
        return when (this) {
            is FallbackSegment -> frameIndex..frameIndex
            is SingleFrameSegment -> frameIndex..frameIndex
            is SequenceSegment -> firstFrameIndex..lastFrameIndex
            is OneShotSegment -> firstFrameIndex..lastFrameIndex
        }
    }
}