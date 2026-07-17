package ru.wh3d0.whCheckJar;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ScanReportWriter {
    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter REPORT_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final File logsFolder;

    ScanReportWriter(File pluginFolder) {
        logsFolder = new File(pluginFolder, "logs");
    }

    File write(ScanResult result) throws IOException {
        Files.createDirectories(logsFolder.toPath());

        byte[] report = buildReport(result).getBytes(StandardCharsets.UTF_8);
        File historyFile = nextHistoryFile(result);
        File latestFile = new File(logsFolder, "latest.log");

        Files.write(historyFile.toPath(), report,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        Files.write(latestFile.toPath(), report,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return historyFile;
    }

    private File nextHistoryFile(ScanResult result) {
        String name = "scan-" + FILE_DATE.format(result.getScannedAt());
        File file = new File(logsFolder, name + ".log");
        int copy = 2;
        while (file.exists()) {
            file = new File(logsFolder, name + "-" + copy++ + ".log");
        }
        return file;
    }

    private String buildReport(ScanResult result) {
        StringBuilder out = new StringBuilder();
        line(out, "whCheckJar");
        line(out, "Дата: " + REPORT_DATE.format(result.getScannedAt()));
        line(out, "Папка: " + result.getPluginsDirectory().getAbsolutePath());
        line(out, "");
        line(out, "JAR-файлов: " + result.getJars().size());
        line(out, "Папок: " + result.getDirectories().size());
        line(out, "Папок без JAR: " + result.getOrphanDirectories().size());
        line(out, "Проблемных JAR: " + result.getProblemJarCount());
        line(out, "");

        line(out, "ПАПКИ");
        if (result.getDirectories().isEmpty()) {
            line(out, "- папок нет");
        }
        for (DirectoryInspection folder : result.getDirectories()) {
            line(out, formatFolder(folder));
        }

        line(out, "");
        line(out, "JAR-ФАЙЛЫ");
        if (result.getJars().isEmpty()) {
            line(out, "- JAR-файлов нет");
        }
        for (JarInspection jar : result.getJars()) {
            line(out, formatJar(jar));
        }

        if (!result.getDuplicatePluginNames().isEmpty()) {
            line(out, "");
            line(out, "ДУБЛИКАТЫ");
            for (Map.Entry<String, List<File>> duplicate
                    : result.getDuplicatePluginNames().entrySet()) {
                List<String> names = new ArrayList<String>();
                for (File file : duplicate.getValue()) {
                    names.add(file.getName());
                }
                line(out, "- " + duplicate.getKey() + ": " + String.join(", ", names));
            }
        }

        line(out, "");
        line(out, "Плагин ничего не удалял и не перемещал.");
        return out.toString();
    }

    private String formatFolder(DirectoryInspection folder) {
        String name = folder.getDirectory().getName();
        switch (folder.getStatus()) {
            case MATCHED_PLUGIN:
                return "- [OK] " + name + " -> " + folder.getPluginName();
            case IGNORED:
                return "- [IGNORE] " + name;
            default:
                return "- [NO JAR] " + name;
        }
    }

    private String formatJar(JarInspection jar) {
        String name = jar.getFile().getName();
        if (jar.getType() == JarInspection.Type.NOT_A_PLUGIN) {
            return "- [NOT PLUGIN] " + name + " — " + jar.getProblem();
        }
        if (jar.getType() == JarInspection.Type.UNREADABLE) {
            return "- [ERROR] " + name + " — " + jar.getProblem();
        }

        String state = "не загружен";
        if (jar.isEnabled()) {
            state = "включён";
        } else if (jar.isLoaded()) {
            state = "выключен";
        }

        return "- [OK] " + name + " -> " + jar.getPluginName()
                + " v" + jar.getPluginVersion() + " (" + state + ")";
    }

    private void line(StringBuilder out, String text) {
        out.append(text).append(System.lineSeparator());
    }
}
