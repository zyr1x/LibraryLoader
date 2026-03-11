package ru.lewis.libraryloader.model

import java.net.URLClassLoader

class ChildFirstClassLoader(
    urls: Array<java.net.URL>,
    parent: ClassLoader
) : URLClassLoader(urls, parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            // уже загружен — возвращаем
            var clazz = findLoadedClass(name)
            if (clazz != null) return clazz

            // сначала ищем в наших url'ах (скачанные либы + классы плагина)
            try {
                clazz = findClass(name)
                if (resolve) resolveClass(clazz)
                return clazz
            } catch (e: ClassNotFoundException) {
                // не нашли — идём к родителю (PluginClassLoader → Paper API, Bukkit)
            }

            return super.loadClass(name, resolve)
        }
    }
}