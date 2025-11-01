package com.nobstergo.portablefurnaceplus.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class yamlUpdate {
    public static void updateYaml(File file, JavaPlugin plugin, String resourceName) {
        try {
            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

            InputStream defaultStream = plugin.getResource(resourceName);
            if (defaultStream == null) return;
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream)
            );

            // Copy missing defaults into config
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!userConfig.contains(key)) {
                    userConfig.set(key, defaultConfig.get(key));
                    changed = true;
                }
            }

            if (changed) {
                userConfig.save(file);
                plugin.getLogger().info("Updated " + resourceName + " with new keys.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update " + resourceName + ": " + e.getMessage());
        }
    }
}