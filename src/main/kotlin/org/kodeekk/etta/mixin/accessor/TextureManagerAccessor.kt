package org.kodeekk.etta.mixin.accessor

import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(TextureManager::class)
interface TextureManagerAccessor {
    @Accessor("byPath")
    fun getByPath(): Map<ResourceLocation, AbstractTexture>
}