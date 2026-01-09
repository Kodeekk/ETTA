package org.kodeekk.etta.mixin

import net.minecraft.client.Minecraft
import org.kodeekk.etta.animation.AnimationController
import org.kodeekk.etta.events.EventSystem
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * MinecraftMixin - Handles game tick events.
 *
 * NOTE: Resource loading is now handled via ResourceManagerHelper in ETTA.kt
 * The commented-out resource reload mixins are NOT needed because we use
 * Fabric's proper ResourceManagerHelper.registerReloadListener() API instead.
 */
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

    // REMOVED: Resource reload mixins
    // We now use ResourceManagerHelper.registerReloadListener() in ETTA.kt
    // This is the proper Fabric API way to handle resource loading
    //
    // The old approach (commented out in your original code) was:
    // - Fragile (relies on specific method names that change between versions)
    // - Unreliable (might not trigger on initial load)
    // - Against Fabric best practices
    //
    // The new approach (in ETTA.kt):
    // - Uses official Fabric API
    // - Works on initial load AND resource reload (F3+T)
    // - Version-independent
    // - Follows Fabric conventions
}