package ru.wh3d0.whCheckJar;

import java.io.File;

final class JarInspection {
    enum Type {
        PLUGIN,
        NOT_A_PLUGIN,
        UNREADABLE
    }

    private final File file;
    private final Type type;
    private final String pluginName;
    private final String pluginVersion;
    private final String problem;
    private final boolean loaded;
    private final boolean enabled;

    private JarInspection(File file, Type type, String pluginName, String pluginVersion,
                          String problem, boolean loaded, boolean enabled) {
        this.file = file;
        this.type = type;
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        this.problem = problem;
        this.loaded = loaded;
        this.enabled = enabled;
    }

    static JarInspection plugin(File file, String pluginName, String pluginVersion,
                                boolean loaded, boolean enabled) {
        return new JarInspection(file, Type.PLUGIN, pluginName, pluginVersion, null, loaded, enabled);
    }

    static JarInspection notAPlugin(File file, String problem) {
        return new JarInspection(file, Type.NOT_A_PLUGIN, null, null, problem, false, false);
    }

    static JarInspection unreadable(File file, String problem) {
        return new JarInspection(file, Type.UNREADABLE, null, null, problem, false, false);
    }

    File getFile() {
        return file;
    }

    Type getType() {
        return type;
    }

    String getPluginName() {
        return pluginName;
    }

    String getPluginVersion() {
        return pluginVersion;
    }

    String getProblem() {
        return problem;
    }

    boolean isLoaded() {
        return loaded;
    }

    boolean isEnabled() {
        return enabled;
    }
}
