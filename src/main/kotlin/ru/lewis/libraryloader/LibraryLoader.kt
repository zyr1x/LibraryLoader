package ru.lewis.libraryloader

import ru.lewis.libraryloader.model.ChildFirstClassLoader
import ru.lewis.libraryloader.model.PomResolver
import java.io.File
import java.util.logging.Logger

class LibraryLoader(
    private val libDir: File,
    private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
    private val logger: Logger? = null,
    private val extraJars: List<File> = emptyList()
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

        val downloader  = Downloader(libDir)
        val pomResolver = PomResolver(config.repositories)

        val allDependencies = config.libraries
            .flatMap { dep ->
                logger?.info("[LibraryLoader] Resolving tree for $dep...")
                listOf(dep) + pomResolver.resolve(dep)
            }
            .distinctBy { it.toString() }

        // собираем URL'ы скачанных jar'ов
        val downloadedUrls = mutableListOf<java.net.URL>()

        allDependencies.forEach { dep ->
            try {
                logger?.info("[LibraryLoader] Downloading $dep...")
                val file = downloader.download(dep, config.repositories)
                downloadedUrls.add(file.toURI().toURL())  // ← собираем сюда
                logger?.info("[LibraryLoader] Loaded ${dep.toFileName()}")
            } catch (e: Exception) {
                logger?.warning("[LibraryLoader] Skipped $dep: ${e.message}")
            }
        }

        val appUrls = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { File(it).toURI().toURL() }

        val allUrls = (downloadedUrls + extraJars.map { it.toURI().toURL() }).toTypedArray()
        return ChildFirstClassLoader(allUrls, classLoader)
    }
}