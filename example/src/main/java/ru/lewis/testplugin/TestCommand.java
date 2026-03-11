package ru.lewis.testplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TestCommand implements CommandExecutor {
    private Logger logger;

    public TestCommand(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        testGson();
        return false;
    }

    public void testGson() {

        try {

            Gson gson = new Gson();

            Map<String, Object> data = new HashMap<>();
            data.put("name", "TestPlugin");
            data.put("version", 1);
            data.put("working", true);

            String json = gson.toJson(data);

            logger.info("Gson JSON: " + json);
            logger.info("Gson ClassLoader: " + Gson.class.getClassLoader());

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }
}
