package org.kodeekk.etta.mixin

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import org.kodeekk.etta.debug.DebugOverlay
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Gui::class)
abstract class GuiMixin {

    @Inject(
        method = ["render"],
        at = [At("TAIL")]
    )
    private fun onRender(context: GuiGraphics, tickCounter: DeltaTracker, ci: CallbackInfo) {
        DebugOverlay.render(context)
    }
}