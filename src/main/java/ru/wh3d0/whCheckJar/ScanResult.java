package ru.wh3d0.whCheckJar;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ScanResult {
    private final LocalDateTime scannedAt;
    private final File pluginsDirectory;
    private final List<JarInspection> jars;
    private final List<DirectoryInspection> directories;
    private final List<File> orphanDirectories;
    private final Map<String, List<File>> duplicatePluginNames;

    ScanResult(LocalDateTime scannedAt, File pluginsDirectory, List<JarInspection> jars,
               List<DirectoryInspection> directories,
               List<File> orphanDirectories,
               Map<String, List<File>> duplicatePluginNames) {
        this.scannedAt = scannedAt;
        this.pluginsDirectory = pluginsDirectory;
        this.jars = Collections.unmodifiableList(new ArrayList<JarInspection>(jars));
        this.directories = Collections.unmodifiableList(
                new ArrayList<DirectoryInspection>(directories));
        this.orphanDirectories = Collections.unmodifiableList(new ArrayList<File>(orphanDirectories));
        this.duplicatePluginNames = Collections.unmodifiableMap(duplicatePluginNames);
    }

    LocalDateTime getScannedAt() {
        return scannedAt;
    }

    File getPluginsDirectory() {
        return pluginsDirectory;
    }

    List<JarInspection> getJars() {
        return jars;
    }

    List<DirectoryInspection> getDirectories() {
        return directories;
    }

    List<File> getOrphanDirectories() {
        return orphanDirectories;
    }

    Map<String, List<File>> getDuplicatePluginNames() {
        return duplicatePluginNames;
    }

    int getPluginJarCount() {
        int count = 0;
        for (JarInspection jar : jars) {
            if (jar.getType() == JarInspection.Type.PLUGIN) {
                count++;
            }
        }
        return count;
    }

    int getProblemJarCount() {
        int count = 0;
        for (JarInspection jar : jars) {
            if (jar.getType() != JarInspection.Type.PLUGIN) {
                count++;
            }
        }
        return count;
    }
}
