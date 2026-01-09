package org.kodeekk.etta.mixin

import net.minecraft.client.renderer.texture.SpriteContents
import org.kodeekk.etta.animation.AnimationController
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.slf4j.LoggerFactory

@Mixin(targets = ["net.minecraft.client.texture.SpriteContents\$Ticker"])
abstract class SpriteContentsTickerMixin {
    private val logger = LoggerFactory.getLogger("ETTA-SpriteTickerMixin")
    private var ettaTextureId: net.minecraft.resources.ResourceLocation? = null

    // Use raw type for frames to avoid referencing private AnimationFrame class
    @Inject(
        method = ["<init>"],
        at = [At("TAIL")]
    )
    private fun onInit(
        contents: SpriteContents,
        frames: List<*>,  // Use raw type
        frameCount: Int,
        defaultFrameTime: Int,
        interpolate: Boolean,
        ci: CallbackInfo
    ) {
        ettaTextureId = contents.name
        logger.info("SpriteTickerMixin initialized for texture: ${ettaTextureId}")
    }

    @Inject(
        method = ["getFrame"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun onGetFrame(cir: CallbackInfoReturnable<Int>) {
        val textureId = ettaTextureId
        if (textureId != null && AnimationController.isAnimated(textureId)) {
            val customFrame = AnimationController.getCurrentFrame(textureId)
            logger.debug("Overriding frame for $textureId → $customFrame")
            cir.returnValue = customFrame
        }
    }
}