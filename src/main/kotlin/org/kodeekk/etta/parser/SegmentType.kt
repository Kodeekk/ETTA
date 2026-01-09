package org.kodeekk.etta.parser

enum class SegmentType {
    FALLBACK_FRAME,  // Single frame fallback
    SINGLE_FRAME,    // Single frame segment
    SEQUENCE,        // Multiple frames with loop
    ONESHOT;         // Multiple frames, no loop (deprecated)

    companion object {
        fun fromString(str: String): SegmentType? {
            return when (str.uppercase()) {
                "FALLBACK_FRAME" -> FALLBACK_FRAME
                "SINGLE_FRAME" -> SINGLE_FRAME
                "SEQUENCE" -> SEQUENCE
                "ONESHOT" -> ONESHOT
                else -> null
            }
        }
    }
}



