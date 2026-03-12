package ru.lewis.libraryloader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class LibraryLoaderInjector implements LibraryInjector {

    private final ClassLoader isolatedClassLoader;

    public LibraryLoaderInjector(File libDir) throws Exception {
        this(libDir, ClassLoader.getSystemClassLoader(), null, Collections.emptyList());
    }

    public LibraryLoaderInjector(File libDir, ClassLoader parent, Logger logger, List<File> extraJars) throws Exception {
        LibraryLoader loader = new LibraryLoader(libDir, parent, logger, extraJars);
        this.isolatedClassLoader = loader.load();
    }

    @Override
    public <T> InjectionBuilder<T> prepare(Class<T> clazz) {
        return new InjectionBuilderImpl<>(clazz.getName(), clazz);
    }

    @Override
    public <T> InjectionBuilder<T> prepare(String className, Class<T> expectedType) {
        return new InjectionBuilderImpl<>(className, expectedType);
    }

    @Override
    public ClassLoader getClassLoader() {
        return isolatedClassLoader;
    }

    private class InjectionBuilderImpl<T> implements InjectionBuilder<T> {
        private final String className;
        private final Class<T> expectedType;
        private final List<Object> args = new ArrayList<>();
        private Class<?>[] paramTypes;

        InjectionBuilderImpl(String className, Class<T> expectedType) {
            this.className = className;
            this.expectedType = expectedType;
        }

        @Override
        public InjectionBuilder<T> with(Object arg) {
            args.add(arg);
            return this;
        }

        @Override
        public InjectionBuilder<T> withTypes(Class<?>... types) {
            this.paramTypes = types;
            return this;
        }

        @Override
        public T build() throws Exception {
            Class<?> loadedClass = isolatedClassLoader.loadClass(className);

            if (args.isEmpty()) {
                return expectedType.cast(loadedClass.getDeclaredConstructor().newInstance());
            }

            if (paramTypes == null) {
                paramTypes = args.stream()
                        .map(Object::getClass)
                        .toArray(Class<?>[]::new);
            }

            Constructor<?> constructor = loadedClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return expectedType.cast(constructor.newInstance(args.toArray()));
        }
    }
}
