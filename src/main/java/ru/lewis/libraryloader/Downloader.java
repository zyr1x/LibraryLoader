package ru.lewis.libraryloader;

import ru.lewis.libraryloader.model.Dependency;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

public class Downloader {

    private final File cacheDir;

    public Downloader(File cacheDir) { this.cacheDir = cacheDir; }

    public File download(Dependency dependency, Map<String, String> repositories) throws Exception {
        File cached = new File(cacheDir, dependency.toFileName());
        if (cached.exists()) return cached;

        cacheDir.mkdirs();

        for (Map.Entry<String, String> entry : repositories.entrySet()) {
            String url = entry.getValue().replaceAll("/$", "") + "/" + dependency.toPath();
            try {
                downloadFile(url, cached);
                return cached;
            } catch (Exception e) {
                cached.delete();
            }
        }

        throw new IllegalStateException("Could not download " + dependency + " from any repository");
    }

    private void downloadFile(String url, File target) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200)
            throw new RuntimeException("HTTP " + conn.getResponseCode() + " for " + url);

        try (var in = conn.getInputStream(); var out = Files.newOutputStream(target.toPath())) {
            in.transferTo(out);
        }
    }
}