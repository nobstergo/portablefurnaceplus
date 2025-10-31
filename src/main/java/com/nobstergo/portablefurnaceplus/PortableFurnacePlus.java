package com.nobstergo.portablefurnaceplus;

import com.nobstergo.portablefurnaceplus.commands.GiveCommand;
import com.nobstergo.portablefurnaceplus.item.PortableFurnaceItem;
import com.nobstergo.portablefurnaceplus.util.MessageFile;
import com.nobstergo.portablefurnaceplus.util.yamlUpdate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class PortableFurnacePlus extends JavaPlugin {

    public static PortableFurnacePlus INSTANCE;
    public NamespacedKey keyBase;

    private BukkitTask tickingTask;

    // Store each player's linked furnace location
    private final HashMap<UUID, Location> playerLinkedFurnaces = new HashMap<>();

    @Override
    public void onEnable() {
        INSTANCE = this;
        saveDefaultConfig();
        saveResource("messages.yml", false);
        yamlUpdate.updateYaml(new File(getDataFolder(), "messages.yml"), this, "messages.yml");
        yamlUpdate.updateYaml(new File(getDataFolder(), "config.yml"), this, "config.yml");

        MessageFile.setup(this);
        keyBase = new NamespacedKey(this, "portable_furnace_plus");

        // Register events
        getServer().getPluginManager().registerEvents(new PortableFurnaceItem(), this);

        // Register command
        this.getCommand("portablefurnaceplus").setExecutor(new GiveCommand());

        // Start ticking task (runs once per second)
        tickingTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            PortableFurnaceItem.processAllPlayers();
        }, 20L, 20L);

        getLogger().info("PortableFurnacePlus enabled.");

    }

    public boolean allowSmoker() {
        return getConfig().getBoolean("allow-smoker", false);
    }

    public boolean allowBlastFurnace() {
        return getConfig().getBoolean("allow-blast-furnace", false);
    }


    @Override
    public void onDisable() {
        for (Location loc : playerLinkedFurnaces.values()) {
            if (loc != null && loc.getWorld() != null) {
                loc.getChunk().setForceLoaded(false);
            }
        }
        playerLinkedFurnaces.clear();
    }

    public HashMap<UUID, Location> getPlayerLinkedFurnaces() {
        return playerLinkedFurnaces;
    }
}
