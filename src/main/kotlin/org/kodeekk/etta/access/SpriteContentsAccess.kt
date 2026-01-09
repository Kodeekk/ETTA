package org.kodeekk.etta.access

import net.minecraft.resources.ResourceLocation

interface SpriteContentsAccess {
    fun `scaldinghot$originalId`(): ResourceLocation?

    fun `scaldinghot$setOriginalId`(originalId: ResourceLocation?)

    fun `scaldinghot$getMipLevel`(): Int
}