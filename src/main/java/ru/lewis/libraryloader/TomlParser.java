package ru.lewis.libraryloader;

import ru.lewis.libraryloader.model.Dependency;

import java.util.*;

public class TomlParser {

    public record LibrariesConfig(Map<String, String> repositories, List<Dependency> libraries) {
    }

    public static LibrariesConfig parse(String content) {
        Map<String, String> repositories = new LinkedHashMap<>();
        List<Dependency>    libraries    = new ArrayList<>();
        String section = "";

        for (String raw : content.split("\n")) {
            String line = raw.trim();
            if (line.isBlank() || line.startsWith("#")) continue;

            if (line.equals("[repositories]")) { section = "repositories"; continue; }
            if (line.equals("[libraries]"))    { section = "libraries";    continue; }

            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                String key   = parts[0].trim().replace("\"", "");
                String value = parts[1].trim().replace("\"", "");

                switch (section) {
                    case "repositories" -> repositories.put(key, value);
                    case "libraries"    -> libraries.add(Dependency.of(value));
                }
            }
        }

        return new LibrariesConfig(repositories, libraries);
    }
}