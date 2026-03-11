package ru.lewis.testplugin.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewis.libraryloader.LibraryLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class TestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        ClassLoader classLoader = null;
        try {
            classLoader = new LibraryLoader(
                    this.getDataFolder().toPath().resolve("libraries").toFile(),
                    this.getClassLoader(),
                    this.getLogger(),
                    List.of()
            ).load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Class<?> appClass = classLoader.loadClass("ru.lewis.testplugin.App");
            Object app = appClass.getDeclaredConstructor(Plugin.class).newInstance(this);
            Method method = appClass.getMethod("enable");
            method.invoke(app);

            Class<?> gsonClass = Class.forName("com.google.gson.Gson", true, classLoader);
            getLogger().info("Gson class loaded: " + gsonClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
