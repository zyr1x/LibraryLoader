package ru.lewis.testplugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

public class App {
    private Plugin plugin;

    public App(Plugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand("test");
        pluginCommand.setExecutor(new TestCommand(plugin.getLogger()));
    }
}
