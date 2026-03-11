package ru.lewis.libraryloader.model;

import java.net.URL;
import java.net.URLClassLoader;

public class ChildFirstClassLoader extends URLClassLoader {

    public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz != null) return clazz;
            try {
                clazz = findClass(name);
                if (resolve) resolveClass(clazz);
                return clazz;
            } catch (ClassNotFoundException ignored) {}
            return super.loadClass(name, resolve);
        }
    }
}