package org.kodeekk.etta.mixin

import net.minecraft.client.Minecraft
import org.kodeekk.etta.animation.AnimationController
import org.kodeekk.etta.events.EventSystem
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
abstract class MinecraftMixin {

    @Inject(
        method = ["tick"],
        at = [At("HEAD")]
    )
    private fun onClientTick(ci: CallbackInfo) {
        EventSystem.tick()
        AnimationController.tick()
    }
}