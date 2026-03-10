package ru.lewis.libraryloader

import ru.lewis.libraryloader.model.Dependency
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Downloader(private val cacheDir: File) {

    fun download(dependency: Dependency, repositories: Map<String, String>): File {
        val cachedFile = File(cacheDir, dependency.toFileName())

        if (cachedFile.exists()) {
            return cachedFile
        }

        cacheDir.mkdirs()

        for ((name, url) in repositories) {
            val fullUrl = "${url.trimEnd('/')}/${dependency.toPath()}"
            try {
                downloadFile(fullUrl, cachedFile)
                return cachedFile
            } catch (e: Exception) {
                cachedFile.delete()
                continue
            }
        }

        throw IllegalStateException("Could not download $dependency from any repository")
    }

    private fun downloadFile(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout    = 30_000
        connection.requestMethod  = "GET"

        if (connection.responseCode != 200) {
            throw RuntimeException("HTTP ${connection.responseCode} for $url")
        }

        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}