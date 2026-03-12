package ru.lewis.testplugin.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewis.libraryloader.LibraryInjector;
import ru.lewis.libraryloader.LibraryLoaderInjector;

import java.util.Collections;

public class TestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            LibraryInjector injector = new LibraryLoaderInjector(
                    this.getDataFolder().toPath().resolve("libraries").toFile(),
                    this.getClassLoader(),
                    this.getLogger(),
                    Collections.emptyList()
            );

            Object app = injector.prepare("ru.lewis.testplugin.App", Object.class)
                    .withTypes(Plugin.class)
                    .with((Plugin) this)
                    .build();

            app.getClass().getMethod("enable").invoke(app);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
