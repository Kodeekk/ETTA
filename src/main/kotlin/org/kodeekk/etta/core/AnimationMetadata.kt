package org.kodeekk.etta.core

import net.minecraft.resources.ResourceLocation

data class AnimationMetadata(
    val textureId: ResourceLocation,
    val frametime: Int = 1,
    val interpolate: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val segments: List<AnimationSegment>,
    val source: AnimationSource = AnimationSource.MCMETAX
)