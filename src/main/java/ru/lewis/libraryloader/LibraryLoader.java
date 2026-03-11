package ru.lewis.libraryloader;

import ru.lewis.libraryloader.model.ChildFirstClassLoader;
import ru.lewis.libraryloader.model.Dependency;
import ru.lewis.libraryloader.model.PomResolver;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class LibraryLoader {

    private final File libDir;
    private final ClassLoader classLoader;
    private final Logger logger;
    private final List<File> extraJars;

    public LibraryLoader(File libDir) {
        this(libDir, ClassLoader.getSystemClassLoader(), null, Collections.emptyList());
    }

    public LibraryLoader(File libDir, ClassLoader classLoader, Logger logger, List<File> extraJars) {
        this.libDir      = libDir;
        this.classLoader = classLoader;
        this.logger      = logger;
        this.extraJars   = extraJars;
    }

    public ClassLoader load() throws Exception {
        var resource = classLoader.getResourceAsStream("libraries.toml");
        if (resource == null) throw new IllegalStateException("libraries.toml not found");

        var config = TomlParser.parse(new String(resource.readAllBytes()));

        if (config.libraries().isEmpty()) {
            log("[LibraryLoader] No libraries to load");
            return classLoader;
        }

        libDir.mkdirs();

        var downloader  = new Downloader(libDir);
        var pomResolver = new PomResolver(config.repositories());

        // resolve full dependency trees, preserving insertion order, deduplicating
        Map<String, Dependency> allDeps = new LinkedHashMap<>();
        for (Dependency dep : config.libraries()) {
            log("[LibraryLoader] Resolving tree for " + dep + "...");
            allDeps.put(dep.toString(), dep);
            for (Dependency transitive : pomResolver.resolve(dep)) {
                allDeps.putIfAbsent(transitive.toString(), transitive);
            }
        }

        List<URL> urls = new ArrayList<>();
        for (Dependency dep : allDeps.values()) {
            try {
                log("[LibraryLoader] Downloading " + dep + "...");
                File file = downloader.download(dep, config.repositories());
                urls.add(file.toURI().toURL());
                log("[LibraryLoader] Loaded " + dep.toFileName());
            } catch (Exception e) {
                warn("[LibraryLoader] Skipped " + dep + ": " + e.getMessage());
            }
        }

        for (File jar : extraJars) {
            urls.add(jar.toURI().toURL());
        }

        return new ChildFirstClassLoader(urls.toArray(new URL[0]), classLoader);
    }

    private void log(String msg)  { if (logger != null) logger.info(msg); }
    private void warn(String msg) { if (logger != null) logger.warning(msg); }
}
