package ru.wh3d0.whCheckJar;

import java.io.File;

final class DirectoryInspection {
    enum Status {
        MATCHED_PLUGIN,
        IGNORED,
        WITHOUT_JAR
    }

    private final File directory;
    private final Status status;
    private final String pluginName;

    DirectoryInspection(File directory, Status status, String pluginName) {
        this.directory = directory;
        this.status = status;
        this.pluginName = pluginName;
    }

    File getDirectory() {
        return directory;
    }

    Status getStatus() {
        return status;
    }

    String getPluginName() {
        return pluginName;
    }
}
