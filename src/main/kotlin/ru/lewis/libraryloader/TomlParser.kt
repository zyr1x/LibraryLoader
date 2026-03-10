package ru.lewis.libraryloader

import ru.lewis.libraryloader.model.Dependency

object TomlParser {

    data class LibrariesConfig(
        val repositories: Map<String, String>,
        val libraries: List<Dependency>
    )

    fun parse(content: String): LibrariesConfig {
        val repositories = mutableMapOf<String, String>()
        val libraries = mutableListOf<Dependency>()

        var section = ""

        content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                when {
                    line == "[repositories]" -> section = "repositories"
                    line == "[libraries]"    -> section = "libraries"
                    line.contains("=") -> {
                        val (key, value) = line.split("=", limit = 2)
                            .map { it.trim().trim('"') }

                        when (section) {
                            "repositories" -> repositories[key] = value
                            "libraries"    -> libraries.add(Dependency.of(value))
                        }
                    }
                }
            }

        return LibrariesConfig(repositories, libraries)
    }
}