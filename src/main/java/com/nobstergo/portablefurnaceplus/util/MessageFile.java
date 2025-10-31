package com.nobstergo.portablefurnaceplus.util;

import com.nobstergo.portablefurnaceplus.PortableFurnacePlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageFile {
    private static FileConfiguration messages;

    public static void setup(PortableFurnacePlus plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public static String get(String key) {
        if (messages == null) return ChatColor.RED + "Messages not loaded.";
        String msg = messages.getString(key, "&cMissing message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}

