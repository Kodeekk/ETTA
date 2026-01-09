package org.kodeekk.etta.debug

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.kodeekk.etta.animation.AnimationController
import org.kodeekk.etta.events.EventSystem
/**
 * Debug overlay showing active events and animated textures.
 * Toggle with F8 key.
 */
object DebugOverlay {
    var enabled = false
    private var lastEventState = mutableSetOf<String>()

    fun toggle() {
        enabled = !enabled
        Minecraft.getInstance().player?.displayClientMessage(
            Component.literal("ETTA Debug: ${if (enabled) "§aON" else "§cOFF"}"),
            false
        )
    }

    fun render(context: GuiGraphics) {
        if (!enabled) return

        val client = Minecraft.getInstance()
        val textRenderer = client.debugRenderer

        var y = 10
        val x = 10
        val lineHeight = 12
        val backgroundColor = 0x40000000.toInt() // Semi-transparent black
        val padding = 2

        // Helper function to render text with background
        fun renderLine(text: String, color: Int = 0xFFFFFFFF.toInt()) {
            val text = Component.literal(text)
            val width = Minecraft.getInstance().window.guiScaledWidth
            val height = Minecraft.getInstance().window.guiScaledHeight
            context.fill(x - padding, y - padding, x + text.toString().length*4 + padding, y + lineHeight, backgroundColor)

            context.drawStringWithBackdrop(Minecraft.getInstance().gui.font, text, x, y, width, color)
            y += lineHeight
        }

        // Title
        renderLine("ETTA Debug", 0xFFFFD700.toInt())
        y += 3

        // Active Events Section
        val activeEvents = EventSystem.getActiveEvents()
        renderLine("Active Events: ${activeEvents.size}", 0xFFFFA500.toInt())

        if (activeEvents.isEmpty()) {
            renderLine("  (none)", 0xFFCCCCCC.toInt())
        } else {
            activeEvents.sorted().take(10).forEach { event ->
                val isNew = event !in lastEventState
                val color = if (isNew) 0xFF00FF00.toInt() else 0xFFFFFFFF.toInt()
                renderLine("  • $event", color)
            }
            if (activeEvents.size > 10) {
                renderLine("  ... ${activeEvents.size - 10} more", 0xFF888888.toInt())
            }
        }

        y += 3

        // Animated Textures Section
        val animatedTextures = AnimationController.getAllAnimatedTextures()
        renderLine("Animated Textures: ${animatedTextures.size}", 0xFFFFA500.toInt())

        if (animatedTextures.isNotEmpty()) {
            animatedTextures.take(5).forEach { textureId ->
                val animation = AnimationController.getAnimation(textureId)
                val frame = AnimationController.getCurrentFrame(textureId)
                val source = animation?.source?.name ?: "?"

                val fileName = textureId.path.split("/").last()
                renderLine("  $fileName [$source] → $frame", 0xFFDDDDDD.toInt())
            }

            if (animatedTextures.size > 5) {
                renderLine("  ... ${animatedTextures.size - 5} more", 0xFF888888.toInt())
            }
        }

        // Update last state
        lastEventState = activeEvents.toMutableSet()
    }
}