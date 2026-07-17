package ru.wh3d0.whCheckJar;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class PluginDirectoryScanner {
    private final File pluginsDirectory;
    private final PluginManager pluginManager;
    private final Set<String> ignoredFolderNames;

    PluginDirectoryScanner(File pluginsDirectory, PluginManager pluginManager,
                           Collection<String> ignoredFolderNames) {
        this.pluginsDirectory = pluginsDirectory;
        this.pluginManager = pluginManager;
        this.ignoredFolderNames = new LinkedHashSet<String>();
        for (String name : ignoredFolderNames) {
            if (name != null && !name.trim().isEmpty()) {
                this.ignoredFolderNames.add(normalize(name.trim()));
            }
        }
    }

    ScanResult scan() {
        File[] contents = pluginsDirectory.listFiles();
        if (contents == null) {
            contents = new File[0];
        }
        Arrays.sort(contents, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        Map<String, Plugin> loadedPlugins = new HashMap<String, Plugin>();
        Map<String, String> knownDataFolders = new LinkedHashMap<String, String>();
        collectLoadedPlugins(loadedPlugins, knownDataFolders);

        List<JarInspection> jars = new ArrayList<JarInspection>();
        Map<String, List<File>> jarsByPluginName = new LinkedHashMap<String, List<File>>();

        for (File file : contents) {
            if (!isJar(file)) {
                continue;
            }

            JarInspection inspection = inspectJar(file, loadedPlugins);
            jars.add(inspection);
            if (inspection.getType() == JarInspection.Type.PLUGIN) {
                knownDataFolders.put(inspection.getPluginName(), inspection.getPluginName());
                jarsByPluginName
                        .computeIfAbsent(normalize(inspection.getPluginName()),
                                key -> new ArrayList<File>())
                        .add(file);
            }
        }

        List<DirectoryInspection> directories = new ArrayList<DirectoryInspection>();
        List<File> orphanDirectories = new ArrayList<File>();
        for (File file : contents) {
            if (!file.isDirectory()) {
                continue;
            }

            String normalizedName = normalize(file.getName());
            if (ignoredFolderNames.contains(normalizedName)) {
                directories.add(new DirectoryInspection(
                        file, DirectoryInspection.Status.IGNORED, null));
            } else if (!knownDataFolders.containsKey(file.getName())) {
                orphanDirectories.add(file);
                directories.add(new DirectoryInspection(
                        file, DirectoryInspection.Status.WITHOUT_JAR, null));
            } else {
                directories.add(new DirectoryInspection(
                        file,
                        DirectoryInspection.Status.MATCHED_PLUGIN,
                        knownDataFolders.get(file.getName())
                ));
            }
        }

        Map<String, List<File>> duplicates = new LinkedHashMap<String, List<File>>();
        for (Map.Entry<String, List<File>> entry : jarsByPluginName.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
        }

        return new ScanResult(LocalDateTime.now(), pluginsDirectory, jars, directories,
                orphanDirectories, duplicates);
    }

    private JarInspection inspectJar(File file, Map<String, Plugin> loadedPlugins) {
        try (JarFile jar = new JarFile(file)) {
            JarEntry pluginYml = jar.getJarEntry("plugin.yml");
            if (pluginYml == null) {
                return JarInspection.notAPlugin(file, "в корне JAR отсутствует plugin.yml");
            }
            try (InputStream input = jar.getInputStream(pluginYml)) {
                PluginDescriptionFile description = new PluginDescriptionFile(input);
                Plugin loaded = loadedPlugins.get(normalize(description.getName()));
                return JarInspection.plugin(file, description.getName(), description.getVersion(),
                        loaded != null, loaded != null && loaded.isEnabled());
            } catch (InvalidDescriptionException exception) {
                return JarInspection.unreadable(file,
                        "некорректный plugin.yml: " + safeMessage(exception));
            }
        } catch (IOException | SecurityException exception) {
            return JarInspection.unreadable(file, safeMessage(exception));
        }
    }

    private void collectLoadedPlugins(Map<String, Plugin> pluginsByName,
                                      Map<String, String> dataFolders) {
        for (Plugin plugin : pluginManager.getPlugins()) {
            pluginsByName.put(normalize(plugin.getName()), plugin);
            File dataFolder = plugin.getDataFolder();
            File parent = dataFolder.getAbsoluteFile().getParentFile();
            if (parent != null && sameFile(parent, pluginsDirectory)) {
                dataFolders.put(dataFolder.getName(), plugin.getName());
            }
        }
    }

    private boolean isJar(File file) {
        return file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    private boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException ignored) {
            return first.getAbsoluteFile().equals(second.getAbsoluteFile());
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty()
                ? exception.getClass().getSimpleName()
                : message.replace('\r', ' ').replace('\n', ' ');
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
