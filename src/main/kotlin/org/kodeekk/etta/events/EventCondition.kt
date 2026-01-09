package org.kodeekk.etta.events

fun interface EventCondition {
    fun check(): Boolean
}