package ru.wh3d0.whCheckJar;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class WhCheckJarPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "whcheckjar.admin";
    private static final String PREFIX = ChatColor.DARK_GRAY + "["
            + ChatColor.AQUA + "whCheckJar" + ChatColor.DARK_GRAY + "] ";

    private ScanResult lastScan;
    private File lastReport;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginCommand command = getCommand("whcheckjar");
        if (command == null) {
            throw new IllegalStateException("Команда whcheckjar не описана в plugin.yml");
        }

        command.setExecutor(this);
        command.setTabCompleter(this);

        if (getConfig().getBoolean("scan-on-startup", true)) {
            long delay = Math.max(1L, getConfig().getLong("startup-delay-ticks", 20L));
            getServer().getScheduler().runTaskLater(this, () -> scan(null), delay);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            message(sender, ChatColor.RED + "Недостаточно прав.");
            return true;
        }

        String action = args.length == 0 ? "scan" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "scan":
                scan(sender);
                break;
            case "status":
                showLastScan(sender);
                break;
            case "reload":
                reloadConfig();
                message(sender, ChatColor.GREEN + "Конфигурация перезагружена.");
                break;
            default:
                message(sender, ChatColor.YELLOW
                        + "Использование: /" + label + " [scan|status|reload]");
        }
        return true;
    }

    private void scan(CommandSender sender) {
        try {
            File pluginsFolder = getDataFolder().getAbsoluteFile().getParentFile();
            if (pluginsFolder == null || !pluginsFolder.isDirectory()) {
                throw new IllegalStateException("Каталог plugins не найден");
            }

            PluginDirectoryScanner scanner = new PluginDirectoryScanner(
                    pluginsFolder,
                    getServer().getPluginManager(),
                    getConfig().getStringList("ignored-folders")
            );

            ScanResult result = scanner.scan();
            File report = new ScanReportWriter(getDataFolder()).write(result);
            lastScan = result;
            lastReport = report;

            if (sender == null) {
                getLogger().info(summary(lastScan) + " Отчёт: logs/" + lastReport.getName());
            } else {
                showResult(sender);
            }
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Не удалось выполнить проверку", exception);
            if (sender != null) {
                message(sender, ChatColor.RED + "Не удалось выполнить проверку. Подробности в консоли.");
            }
        }
    }

    private void showLastScan(CommandSender sender) {
        if (lastScan == null) {
            message(sender, ChatColor.YELLOW + "Проверка ещё не выполнялась.");
            return;
        }
        showResult(sender);
    }

    private void showResult(CommandSender sender) {
        message(sender, ChatColor.GREEN + "Проверка завершена.");
        sender.sendMessage(ChatColor.GRAY + summary(lastScan));

        if (!lastScan.getOrphanDirectories().isEmpty()) {
            List<String> names = new ArrayList<String>();
            for (File folder : lastScan.getOrphanDirectories()) {
                names.add(folder.getName());
            }
            sender.sendMessage(ChatColor.YELLOW + "Без JAR: " + String.join(", ", names));
        }

        sender.sendMessage(ChatColor.GRAY + "Отчёт: " + ChatColor.WHITE
                + "plugins/whCheckJar/logs/" + lastReport.getName());
    }

    private String summary(ScanResult result) {
        return "JAR: " + result.getJars().size()
                + ", папок: " + result.getDirectories().size()
                + ", без JAR: " + result.getOrphanDirectories().size()
                + ", проблемных JAR: " + result.getProblemJarCount() + ".";
    }

    private void message(CommandSender sender, String text) {
        sender.sendMessage(PREFIX + text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String option : Arrays.asList("scan", "status", "reload")) {
            if (option.startsWith(input)) {
                result.add(option);
            }
        }
        return result;
    }
}
