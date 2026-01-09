package org.kodeekk.etta.parser

enum class CorrectorType {
    EVENT,      // @event("name")
    EXPRESSION, // @expression({ ... })
    NONE        // No corrector (always active)
}