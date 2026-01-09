package org.kodeekk.etta.mixin.accessor

import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Mutable
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(TextureAtlasSprite::class)
interface TextureAtlasSpriteAccessor {
    @Mutable
    @Accessor("contents")
    fun setContents(contents: SpriteContents)

    @Accessor("contents")
    fun getContents(): SpriteContents
}
