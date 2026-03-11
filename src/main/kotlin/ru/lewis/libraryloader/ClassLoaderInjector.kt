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
        return URLClassLoader(urls.toTypedArray(), parent)
    }

    private fun getAppClassPath(): List<URL> {
        return System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { File(it).toURI().toURL() }
    }
}