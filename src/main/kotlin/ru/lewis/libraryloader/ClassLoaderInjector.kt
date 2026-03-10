package ru.lewis.libraryloader

import java.io.File
import java.net.URL
import java.net.URLClassLoader

object ClassLoaderInjector {

    private val urls = mutableListOf<URL>()

    fun inject(file: File, classLoader: ClassLoader) {
        urls.add(file.toURI().toURL())
    }

    fun buildClassLoader(parent: ClassLoader): URLClassLoader {
        // добавляем classpath самого приложения
        val appUrls = (parent as? URLClassLoader)?.urLs?.toList()
            ?: getAppClassPath()

        val allUrls = (appUrls + urls).toTypedArray()
        // parent = null чтобы не делегировать системному classloader
        return URLClassLoader(allUrls, null)
    }

    private fun getAppClassPath(): List<URL> {
        return System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { File(it).toURI().toURL() }
    }
}