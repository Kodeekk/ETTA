package org.kodeekk.etta.hotreload

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Watches resource pack directories for file changes.
 * Based on ScaldingHot's file watching system but self-contained in ETTA.
 */
object FileWatcher {
    private val logger = LoggerFactory.getLogger("ETTA-FileWatcher")

    private val watchServices = mutableListOf<WatchService>()
    private val watchedPaths = ConcurrentHashMap<Path, WatchKey>()
    private val pathToResourcePack = ConcurrentHashMap<Path, String>()

    // Debouncing
    private val pendingChanges = ConcurrentHashMap<Path, ChangeType>()
    private var debounceTimer: Thread? = null
    private val debounceDelayMs = 500L

    private var watchThread: Thread? = null
    private var running = false

    enum class ChangeType {
        CREATED, MODIFIED, DELETED
    }

    /**
     * Starts watching resource pack directories.
     */
    fun start() {
        if (running) {
            logger.warn("FileWatcher already running")
            return
        }

        running = true

        watchThread = thread(name = "ETTA-FileWatcher", isDaemon = true) {
            logger.info("FileWatcher started")

            while (running) {
                try {
                    processWatchEvents()
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    logger.error("Error in watch thread", e)
                }
            }

            logger.info("FileWatcher stopped")
        }
    }

    /**
     * Stops the file watcher.
     */
    fun stop() {
        running = false
        watchThread?.interrupt()

        watchServices.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                logger.error("Error closing watch service", e)
            }
        }

        watchServices.clear()
        watchedPaths.clear()
        pathToResourcePack.clear()
    }

    /**
     * Registers a resource pack directory for watching.
     *
     * @param packPath The root path of the resource pack
     * @param packName The name of the resource pack (for logging)
     */
    fun watchResourcePack(packPath: Path, packName: String) {
        if (!Files.exists(packPath)) {
            logger.warn("Resource pack path does not exist: $packPath")
            return
        }

        if (!Files.isDirectory(packPath)) {
            logger.warn("Resource pack path is not a directory: $packPath")
            return
        }

        try {
            val watchService = FileSystems.getDefault().newWatchService()
            watchServices.add(watchService)

            // Register the root directory
            registerDirectory(packPath, watchService, packName)

            // Recursively register subdirectories
            Files.walk(packPath).forEach { path ->
                if (Files.isDirectory(path)) {
                    registerDirectory(path, watchService, packName)
                }
            }

            logger.info("Watching resource pack: $packName at $packPath")
        } catch (e: Exception) {
            logger.error("Failed to watch resource pack: $packPath", e)
        }
    }

    private fun registerDirectory(path: Path, watchService: WatchService, packName: String) {
        try {
            val watchKey = path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )

            watchedPaths[path] = watchKey
            pathToResourcePack[path] = packName

            logger.debug("Registered watch on: $path")
        } catch (e: Exception) {
            logger.debug("Could not register watch on: $path (${e.message})")
        }
    }

    private fun processWatchEvents() {
        for (watchService in watchServices) {
            val watchKey = watchService.poll() ?: continue

            val directory = watchedPaths.entries.find { it.value == watchKey }?.key ?: continue

            for (event in watchKey.pollEvents()) {
                val kind = event.kind()

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                val ev = event as WatchEvent<Path>
                val filename = ev.context()
                val changedPath = directory.resolve(filename)

                val changeType = when (kind) {
                    StandardWatchEventKinds.ENTRY_CREATE -> ChangeType.CREATED
                    StandardWatchEventKinds.ENTRY_MODIFY -> ChangeType.MODIFIED
                    StandardWatchEventKinds.ENTRY_DELETE -> ChangeType.DELETED
                    else -> continue
                }

                // Only watch ETTA-related files
                if (isEttaFile(changedPath)) {
                    logger.debug("Detected $changeType: $changedPath")
                    pendingChanges[changedPath] = changeType
                    scheduleDebounce()
                }
            }

            watchKey.reset()
        }
    }

    private fun isEttaFile(path: Path): Boolean {
        val pathStr = path.toString()
        val separator = File.separator
        return pathStr.endsWith(".mcmetax") ||
                (pathStr.contains(".etta${separator}frames$separator") &&
                        pathStr.endsWith(".png"))
    }

    private fun scheduleDebounce() {
        // Cancel existing timer
        debounceTimer?.interrupt()

        // Start new timer
        debounceTimer = thread(name = "ETTA-Debounce", isDaemon = true) {
            try {
                Thread.sleep(debounceDelayMs)
                processPendingChanges()
            } catch (e: InterruptedException) {
                // Timer cancelled
            }
        }
    }

    private fun processPendingChanges() {
        if (pendingChanges.isEmpty()) return

        val changes = pendingChanges.toMap()
        pendingChanges.clear()

        logger.info("Processing ${changes.size} file changes")

        // Notify hot reload handler
        HotReloadHandler.handleChanges(changes)
    }
}