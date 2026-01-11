package org.kodeekk.etta.animation

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import org.kodeekk.etta.core.*
import org.kodeekk.etta.events.EventSystem
import org.kodeekk.etta.texture.SpriteManager
import org.kodeekk.etta.texture.SpriteUploader
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object AnimationController {
    private val logger = LoggerFactory.getLogger("ETTA-Controller")

    private val animations = ConcurrentHashMap<ResourceLocation, AnimationMetadata>()
    private val states = ConcurrentHashMap<ResourceLocation, List<SegmentState>>()
    private val lastUploadedFrame = ConcurrentHashMap<ResourceLocation, Int>()
    private val failedUploads = ConcurrentHashMap<ResourceLocation, Int>()
    private val debugLogged = ConcurrentHashMap<ResourceLocation, Boolean>()

    fun registerAnimation(metadata: AnimationMetadata) {
        animations[metadata.textureId] = metadata

        val segmentStates = metadata.segments.map { segment ->
            SegmentState(
                segment = segment,
                currentFrame = when (segment) {
                    is AnimationSegment.FallbackSegment -> segment.frameIndex
                    is AnimationSegment.SingleFrameSegment -> segment.frameIndex
                    is AnimationSegment.SequenceSegment -> segment.firstFrameIndex
                    is AnimationSegment.OneShotSegment -> segment.firstFrameIndex
                },
                isActive = segment is AnimationSegment.FallbackSegment
            )
        }
        states[metadata.textureId] = segmentStates

        logger.info("Registered animation: ${metadata.textureId} (${metadata.source}, ${metadata.segments.size} segments)")
    }

    fun getAnimation(textureId: ResourceLocation): AnimationMetadata? {
        return animations[textureId]
    }

    fun isAnimated(textureId: ResourceLocation): Boolean {
        return animations.containsKey(textureId)
    }

    fun getCurrentFrame(textureId: ResourceLocation): Int {
        val segmentStates = states[textureId] ?: return 0
        val metadata = animations[textureId] ?: return 0

        if (metadata.source == AnimationSource.MCMETA) {
            return segmentStates.firstOrNull()?.currentFrame ?: 0
        }

        val activeSegment = segmentStates
            .filter { it.isActive }
            .maxByOrNull { it.segment.priority }

        return activeSegment?.currentFrame ?: 0
    }

    fun tick() {
        val player = Minecraft.getInstance().player

        val contextVars: MutableMap<String, Any> = mutableMapOf(
            "__health" to (player?.health?.toDouble() ?: 20.0),
            "__max_health" to (player?.maxHealth?.toDouble() ?: 20.0),
            "__hunger" to (player?.foodData?.foodLevel ?: 20),
            "__first_frame" to 0,
            "__last_frame" to 0
        )

        states.forEach { (textureId, segmentStates) ->
            val metadata = animations[textureId] ?: return@forEach

            contextVars["__last_frame"] = segmentStates.maxOfOrNull {
                when (val seg = it.segment) {
                    is AnimationSegment.SequenceSegment -> seg.lastFrameIndex
                    is AnimationSegment.OneShotSegment -> seg.lastFrameIndex
                    else -> 0
                }
            } ?: 0

            when (metadata.source) {
                AnimationSource.MCMETA -> tickMcmetaAnimation(segmentStates, metadata)
                AnimationSource.MCMETAX -> tickMcmetaxAnimation(textureId, segmentStates, metadata, contextVars)
            }

            if (metadata.source == AnimationSource.MCMETAX && SpriteManager.hasFrames(textureId)) {
                uploadFrameIfChanged(textureId)
            }
        }
    }

    private fun uploadFrameIfChanged(textureId: ResourceLocation) {
        try {
            val currentFrame = getCurrentFrame(textureId)
            val lastFrame = lastUploadedFrame[textureId]

            if (lastFrame != null && lastFrame == currentFrame) {
                return
            }

            val failCount = failedUploads.getOrDefault(textureId, 0)

            if (failCount >= 10) {
                if (!debugLogged.containsKey(textureId)) {
                    logger.warn("Gave up uploading frames for $textureId after 10 failures (texture may not be in atlas)")
                    debugLogged[textureId] = true
                }
                return
            }

            val success = SpriteUploader.uploadFrameFromMemory(textureId, currentFrame)

            if (success) {
                lastUploadedFrame[textureId] = currentFrame
                failedUploads.remove(textureId)

                if (!debugLogged.containsKey(textureId)) {
                    logger.info("Successfully started GPU uploads for $textureId")
                    debugLogged[textureId] = true
                }
            } else {
                val newFailCount = failCount + 1
                failedUploads[textureId] = newFailCount

                if (newFailCount % 5 == 0) {
                    logger.debug("Failed to upload frame for $textureId ($newFailCount times)")
                }
            }
        } catch (e: Exception) {
            val failCount = failedUploads.getOrDefault(textureId, 0) + 1
            failedUploads[textureId] = failCount

            if (failCount <= 2) {
                logger.error("Exception during frame upload for $textureId", e)
            }
        }
    }

    private fun tickMcmetaAnimation(segmentStates: List<SegmentState>, metadata: AnimationMetadata) {
        val state = segmentStates.firstOrNull() ?: return
        val segment = state.segment as? AnimationSegment.SequenceSegment ?: return

        val effectiveFrametime = segment.frametime ?: metadata.frametime

        state.tickCounter++
        if (state.tickCounter >= effectiveFrametime) {
            state.tickCounter = 0
            state.currentFrame++

            if (state.currentFrame > segment.lastFrameIndex) {
                if (segment.loop) {
                    state.currentFrame = segment.firstFrameIndex
                } else {
                    state.currentFrame = segment.lastFrameIndex
                }
            }
        }
    }

    private fun tickMcmetaxAnimation(
        textureId: ResourceLocation,
        segmentStates: List<SegmentState>,
        metadata: AnimationMetadata,
        contextVars: Map<String, Any>
    ) {
        segmentStates.forEach { state ->
            val segment = state.segment
            val effectiveFrametime = segment.frametime ?: metadata.frametime

            when (segment) {
                is AnimationSegment.FallbackSegment -> {
                    val hasHigherPriority = segmentStates.any {
                        it.segment.priority > segment.priority && it.isActive
                    }
                    state.isActive = !hasHigherPriority
                }

                is AnimationSegment.SingleFrameSegment -> {
                    val shouldBeActive = evaluateSegmentCondition(segment, contextVars)

                    if (shouldBeActive != state.isActive) {
                        state.isActive = shouldBeActive
                        if (shouldBeActive) {
                            state.currentFrame = segment.frameIndex
                            logger.debug("Activated SINGLE_FRAME '${segment.name}' for $textureId")
                        }
                    }
                }

                is AnimationSegment.SequenceSegment -> {
                    val shouldBeActive = evaluateSegmentCondition(segment, contextVars)

                    if (shouldBeActive && !state.isActive) {
                        state.isActive = true
                        state.currentFrame = segment.firstFrameIndex
                        state.tickCounter = 0
                        logger.debug("Activated SEQUENCE '${segment.name}' for $textureId")
                    } else if (!shouldBeActive && state.isActive) {
                        if (segment.pauseOnLastFrame) {
                            state.currentFrame = segment.lastFrameIndex
                        } else if (!segment.loop) {
                            state.isActive = false
                        }
                    }

                    if (state.isActive) {
                        state.tickCounter++
                        if (state.tickCounter >= effectiveFrametime) {
                            state.tickCounter = 0
                            state.currentFrame++

                            if (state.currentFrame > segment.lastFrameIndex) {
                                if (segment.loop && shouldBeActive) {
                                    state.currentFrame = segment.firstFrameIndex
                                } else {
                                    state.currentFrame = segment.lastFrameIndex
                                    if (!segment.pauseOnLastFrame) {
                                        state.isActive = false
                                    }
                                }
                            }
                        }
                    }
                }

                is AnimationSegment.OneShotSegment -> {
                    val shouldBeActive = evaluateSegmentCondition(segment, contextVars)

                    if (shouldBeActive && !state.isActive && !state.hasPlayed) {
                        state.isActive = true
                        state.currentFrame = segment.firstFrameIndex
                        state.tickCounter = 0
                        logger.debug("Activated ONESHOT '${segment.name}' for $textureId")
                    }

                    if (state.isActive) {
                        state.tickCounter++
                        if (state.tickCounter >= effectiveFrametime) {
                            state.tickCounter = 0
                            state.currentFrame++

                            if (state.currentFrame > segment.lastFrameIndex) {
                                state.currentFrame = segment.lastFrameIndex
                                state.isActive = false
                                state.hasPlayed = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun evaluateSegmentCondition(segment: AnimationSegment, contextVars: Map<String, Any>): Boolean {
        return when {
            segment is AnimationSegment.SingleFrameSegment -> {
                segment.expression?.evaluate(contextVars)
                    ?: segment.eventName?.let { EventSystem.isEventActive(it) }
                    ?: false
            }
            segment is AnimationSegment.SequenceSegment -> {
                segment.expression?.evaluate(contextVars)
                    ?: segment.eventName?.let { EventSystem.isEventActive(it) }
                    ?: true
            }
            segment is AnimationSegment.OneShotSegment -> {
                segment.expression?.evaluate(contextVars)
                    ?: segment.eventName?.let { EventSystem.isEventActive(it) }
                    ?: false
            }
            else -> false
        }
    }

    fun clear() {
        animations.clear()
        states.clear()
        lastUploadedFrame.clear()
        failedUploads.clear()
        debugLogged.clear()
        logger.info("Cleared all animations")
    }

    fun getAllAnimatedTextures(): Set<ResourceLocation> {
        return animations.keys
    }

    fun getDebugInfo(textureId: ResourceLocation): String {
        val metadata = animations[textureId] ?: return "No animation data"
        val segmentStates = states[textureId] ?: return "No segment states"

        return buildString {
            appendLine("Animation: $textureId (${metadata.source})")
            appendLine("Frametime: ${metadata.frametime}")
            appendLine("Current Frame: ${getCurrentFrame(textureId)}")
            appendLine("Segments:")
            segmentStates.forEach { state ->
                val segment = state.segment
                val status = if (state.isActive) "ACTIVE" else "inactive"
                appendLine("  - ${segment.name}: $status (frame ${state.currentFrame}, priority ${segment.priority})")
            }
        }
    }
}