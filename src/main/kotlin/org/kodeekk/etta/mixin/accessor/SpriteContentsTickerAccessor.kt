package org.kodeekk.etta.mixin.accessor

import net.minecraft.client.renderer.texture.SpriteContents
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(targets = ["net.minecraft.client.texture.SpriteContents\$Ticker"])
interface SpriteContentsTickerAccessor {
    @Accessor
    fun getContents(): SpriteContents
}