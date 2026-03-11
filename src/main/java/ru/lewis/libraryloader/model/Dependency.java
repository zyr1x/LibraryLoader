package ru.lewis.libraryloader.model;

public class Dependency {

    private final String group;
    private final String artifact;
    private final String version;

    public Dependency(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    public String getGroup()    { return group; }
    public String getArtifact() { return artifact; }
    public String getVersion()  { return version; }

    public String toPath() {
        return group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

    public String toFileName() { return artifact + "-" + version + ".jar"; }

    public String toPomPath() {
        return group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".pom";
    }

    @Override public String toString() { return group + ":" + artifact + ":" + version; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Dependency d)) return false;
        return toString().equals(d.toString());
    }

    @Override public int hashCode() { return toString().hashCode(); }

    public static Dependency of(String notation) {
        String[] parts = notation.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid dependency: " + notation);
        return new Dependency(parts[0], parts[1], parts[2]);
    }
}