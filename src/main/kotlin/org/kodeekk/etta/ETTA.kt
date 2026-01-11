package org.kodeekk.etta

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.KeyMapping
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import org.kodeekk.etta.debug.DebugOverlay
import org.kodeekk.etta.hotreload.FileWatcher
import org.kodeekk.etta.hotreload.ResourcePackScanner
import org.kodeekk.etta.resources.ResourceLoader
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ETTA : ClientModInitializer {
    private var instance: ETTA = this
    public var logger: Logger = LoggerFactory.getLogger("ETTA")
    @JvmStatic
    public fun getInstance(): ETTA = instance
    @JvmStatic
    public fun getLOGGER(): Logger = logger
    const val MOD_ID = "etta"

    private lateinit var debugToggleKey: KeyMapping
    private var hotReloadEnabled = false
    private var resourcesLoaded = false

    override fun onInitializeClient() {
        logger.info("Initializing ETTA - Event-Triggered Texture Animation System")

        debugToggleKey = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.etta.toggle_debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.etta"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (debugToggleKey.consumeClick()) {
                DebugOverlay.toggle()
            }
        }

        registerResourceReloadListener()

        initializeHotReload()

        logger.info("ETTA initialized successfully")
        logger.info("Supports: .mcmetax (ETTA format) and .mcmeta (Minecraft format)")
        logger.info("Press F8 to toggle debug overlay")
    }

    private fun registerResourceReloadListener() {
        try {
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                object : SimpleSynchronousResourceReloadListener {
                    override fun getFabricId(): ResourceLocation {
                        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "animation_loader")
                    }

                    override fun onResourceManagerReload(resourceManager: ResourceManager) {
                        logger.info("=== ETTA Resource Reload Triggered ===")

                        ResourceLoader.loadAnimations(resourceManager)

                        resourcesLoaded = true
                        logger.info("=== ETTA Resource Reload Complete ===")
                    }
                }
            )

            logger.info("Resource reload listener registered successfully")
        } catch (e: Exception) {
            logger.error("Failed to register resource reload listener", e)
        }
    }
    private fun initializeHotReload() {
        try {
            FileWatcher.start()

            ClientTickEvents.END_CLIENT_TICK.register { client ->
                if (client.resourceManager != null && resourcesLoaded && !hotReloadEnabled) {
                    hotReloadEnabled = true

                    client.execute {
                        try {
                            ResourcePackScanner.scanAndWatch()
                            logger.info("Hot reload enabled - edit .mcmetax and frame files while game is running")
                        } catch (e: Exception) {
                            logger.error("Failed to scan resource packs for hot reload", e)
                        }
                    }
                }
            }

            logger.info("Hot reload system initialized")
        } catch (e: Exception) {
            logger.error("Failed to initialize hot reload system", e)
        }
    }
}