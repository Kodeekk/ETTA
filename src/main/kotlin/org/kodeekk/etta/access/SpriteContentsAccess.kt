package org.kodeekk.etta.access

import net.minecraft.resources.ResourceLocation

interface SpriteContentsAccess {
    fun `etta$originalId`(): ResourceLocation?
    fun `etta$getMipLevel`(): Int
//    fun `etta$setOriginalId`(originalId: ResourceLocation?)
}