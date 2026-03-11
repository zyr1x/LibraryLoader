package ru.lewis.libraryloader.model;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class PomResolver {

    private final Map<String, String> repositories;
    private final MavenXpp3Reader reader = new MavenXpp3Reader();

    public PomResolver(Map<String, String> repositories) {
        this.repositories = repositories;
    }

    public List<Dependency> resolve(Dependency dependency) {
        return resolve(dependency, new HashSet<>());
    }

    private List<Dependency> resolve(Dependency dependency, Set<String> visited) {
        String key = dependency.toString();
        if (visited.contains(key)) return Collections.emptyList();
        visited.add(key);

        Model model = fetchPom(dependency);
        if (model == null) return Collections.emptyList();

        resolveModel(model);

        List<Dependency> result = new ArrayList<>();
        for (org.apache.maven.model.Dependency dep : model.getDependencies()) {
            String scope   = dep.getScope() == null ? "compile" : dep.getScope();
            boolean optional = dep.isOptional();

            if (scope.equals("test") || scope.equals("provided") || scope.equals("system")) continue;
            if (optional) continue;

            String version = dep.getVersion();
            if (version == null) continue;

            Dependency resolved = new Dependency(dep.getGroupId(), dep.getArtifactId(), version);
            result.add(resolved);
            result.addAll(resolve(resolved, visited));
        }
        return result;
    }

    private Model fetchPom(Dependency dependency) {
        String pomPath = dependency.toPomPath();
        for (Map.Entry<String, String> entry : repositories.entrySet()) {
            String fullUrl = entry.getValue().replaceAll("/$", "") + "/" + pomPath;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(30_000);
                if (conn.getResponseCode() != 200) continue;
                try (var is = conn.getInputStream()) {
                    return reader.read(is);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void resolveModel(Model model) {
        Map<String, String> props = new LinkedHashMap<>();

        if (model.getParent() != null && model.getParent().getVersion() != null) {
            props.put("project.parent.version", model.getParent().getVersion());
            props.put("parent.version",         model.getParent().getVersion());
        }

        String projectVersion = model.getVersion() != null ? model.getVersion()
                : (model.getParent() != null ? model.getParent().getVersion() : null);
        if (projectVersion != null) props.put("project.version", projectVersion);

        if (model.getProperties() != null) {
            model.getProperties().forEach((k, v) -> props.put(k.toString(), v.toString()));
        }

        Map<String, String> managed = new HashMap<>();
        if (model.getDependencyManagement() != null) {
            for (org.apache.maven.model.Dependency dep : model.getDependencyManagement().getDependencies()) {
                String version = resolve(dep.getVersion(), props);
                if (version != null) managed.put(dep.getGroupId() + ":" + dep.getArtifactId(), version);
            }
        }

        for (org.apache.maven.model.Dependency dep : model.getDependencies()) {
            dep.setGroupId(resolve(dep.getGroupId(), props));
            dep.setArtifactId(resolve(dep.getArtifactId(), props));
            String version = resolve(dep.getVersion(), props);
            if (version == null) version = managed.get(dep.getGroupId() + ":" + dep.getArtifactId());
            dep.setVersion(version);
        }
    }

    private String resolve(String value, Map<String, String> props) {
        if (value == null || !value.contains("${")) return value;
        for (Map.Entry<String, String> e : props.entrySet()) {
            value = value.replace("${" + e.getKey() + "}", e.getValue());
        }
        return value;
    }
}