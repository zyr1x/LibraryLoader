package ru.lewis.libraryloader;

public interface LibraryInjector {

    <T> InjectionBuilder<T> prepare(Class<T> clazz);

    <T> InjectionBuilder<T> prepare(String className, Class<T> expectedType);

    ClassLoader getClassLoader();

    interface InjectionBuilder<T> {
        InjectionBuilder<T> with(Object arg);
        InjectionBuilder<T> withTypes(Class<?>... types);
        T build() throws Exception;
    }
}
