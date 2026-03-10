package ru.lewis.libraryloader

import ru.lewis.libraryloader.model.PomResolver
import java.io.File
import java.util.logging.Logger

class LibraryLoader(
    private val libDir: File,
    private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
    private val logger: Logger? = null
) {
    fun load(): ClassLoader {
        val resource = classLoader.getResourceAsStream("libraries.toml")
            ?: throw IllegalStateException("libraries.toml not found")

        val config = TomlParser.parse(resource.bufferedReader().readText())

        if (config.libraries.isEmpty()) {
            logger?.info("[LibraryLoader] No libraries to load")
            return classLoader
        }

        libDir.mkdirs()

        val downloader   = Downloader(libDir)
        val pomResolver  = PomResolver(config.repositories)

        // Резолвим все зависимости включая транзитивные
        val allDependencies = config.libraries
            .flatMap { dep ->
                logger?.info("[LibraryLoader] Resolving tree for $dep...")
                listOf(dep) + pomResolver.resolve(dep)
            }
            .distinctBy { it.toString() }  // убираем дубли

        allDependencies.forEach { dep ->
            try {
                logger?.info("[LibraryLoader] Downloading $dep...")
                val file = downloader.download(dep, config.repositories)
                ClassLoaderInjector.inject(file, classLoader)
                logger?.info("[LibraryLoader] Loaded ${dep.toFileName()}")
            } catch (e: Exception) {
                logger?.warning("[LibraryLoader] Skipped $dep: ${e.message}")
            }
        }

        return ClassLoaderInjector.buildClassLoader(classLoader)
    }
}