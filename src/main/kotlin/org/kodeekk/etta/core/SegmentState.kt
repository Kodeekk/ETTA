package org.kodeekk.etta.core

data class SegmentState(
    val segment: AnimationSegment,
    var currentFrame: Int,
    var isActive: Boolean = false,
    var tickCounter: Int = 0,
    var hasPlayed: Boolean = false // For ONESHOT segments
)