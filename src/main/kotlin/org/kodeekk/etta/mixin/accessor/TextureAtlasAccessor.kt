package org.kodeekk.etta.mixin.accessor

import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(TextureAtlas::class)
interface TextureAtlasAccessor {
    @Accessor("sprites")
    fun getSprites(): List<SpriteContents>

    @Accessor("texturesByName")
    fun getTexturesByName(): Map<ResourceLocation, TextureAtlasSprite>
}