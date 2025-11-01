package com.nobstergo.portablefurnaceplus.item;

import com.nobstergo.portablefurnaceplus.PortableFurnacePlus;
import com.nobstergo.portablefurnaceplus.util.ItemSerialization;
import com.nobstergo.portablefurnaceplus.util.MessageFile;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.block.Furnace;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import java.util.*;

public class PortableFurnaceItem implements Listener {

    public static final NamespacedKey KEY_LINKED_LOC = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_linked_loc");
    public static final NamespacedKey KEY_INV = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_inv");
    public static final NamespacedKey KEY_OWNER = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_owner");
    public static final NamespacedKey KEY_BURN = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_burn");
    public static final NamespacedKey KEY_BURN_TOTAL = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_burn_total");
    public static final NamespacedKey KEY_COOK = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_cook");
    public static final NamespacedKey KEY_COOK_TOTAL = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_cook_total");
    public static final NamespacedKey KEY_VER = new NamespacedKey(PortableFurnacePlus.INSTANCE, "pfp_ver");

    public static final int MAX_INPUT_STACK = 16;

    // Map open GUI 
    private static final Map<Inventory, ItemStack> openInventoryToItem = new WeakHashMap<>();
    // Store location of linked Furnace
    private static final Set<Location> forcedChunks = new HashSet<>();
    // Store linked furnaces
    private static final Map<UUID, Location> playerLinkedFurnaces = new HashMap<>();

    private static void addForcedChunk(Location loc) {
        if (loc == null) return;
        Chunk chunk = loc.getChunk();
        chunk.setForceLoaded(true);
        forcedChunks.add(loc);
    }

    private static void removeForcedChunk(Location loc) {
        if (loc == null) return;
        Chunk chunk = loc.getChunk();
        chunk.setForceLoaded(false);
        forcedChunks.remove(loc);
    }

    public static void clearAllForcedChunks() {
        for (Location loc : forcedChunks) {
            Chunk chunk = loc.getChunk();
            chunk.setForceLoaded(false);
        }
        forcedChunks.clear();
    }

    // Minimal fuel in ticks
    private static final Map<Material, Integer> FUEL_TIMES = new HashMap<>();
    static {
        FUEL_TIMES.put(Material.COAL, 1600);
        FUEL_TIMES.put(Material.CHARCOAL, 1600);
        FUEL_TIMES.put(Material.BLAZE_ROD, 2400);
        FUEL_TIMES.put(Material.LAVA_BUCKET, 20000);
        FUEL_TIMES.put(Material.COAL_BLOCK, 16000);
    }

    // Create item
    public static ItemStack createPortableFurnaceFor(Player owner) {
        ItemStack item = new ItemStack(Material.FURNACE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Portable Furnace");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_VER, PersistentDataType.INTEGER, 1);

        // only set owner if player exists
        if (owner != null) {
            pdc.set(KEY_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        }

        // initialize empty inventory 
        try {
            ItemSerialization.saveItemStackArray(pdc, KEY_INV, new ItemStack[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        pdc.set(KEY_BURN, PersistentDataType.INTEGER, 0);
        pdc.set(KEY_BURN_TOTAL, PersistentDataType.INTEGER, 0);
        pdc.set(KEY_COOK, PersistentDataType.INTEGER, 0);
        pdc.set(KEY_COOK_TOTAL, PersistentDataType.INTEGER, 10); 

        item.setItemMeta(meta);
        return item;
    }

    private static boolean isPortable(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_VER, PersistentDataType.INTEGER);
    }

    private static void addForcedChunkIfOnline(Player player, Location loc) {
        if (loc == null) return;
        if (!player.isOnline()) return;
        Chunk chunk = loc.getChunk();
        if (!chunk.isForceLoaded()) {
            chunk.setForceLoaded(true);
        }
    }

    private static void unloadIfOffline(Player player) {
        UUID id = player.getUniqueId();
        Location loc = playerLinkedFurnaces.get(id);
        if (loc == null) return;
        Chunk chunk = loc.getChunk();
        if (chunk.isForceLoaded()) {
            chunk.setForceLoaded(false);
        }
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Location loc = playerLinkedFurnaces.get(p.getUniqueId());
        if (loc != null) {
            addForcedChunkIfOnline(p, loc);
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        Player p = e.getPlayer();
        unloadIfOffline(p);
    }
    
    public static boolean isPortableFurnace(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY_VER, PersistentDataType.INTEGER);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (PortableFurnaceItem.isPortableFurnace(item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !isPortable(item)) return;

        Player player = e.getPlayer();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Link on shift-click
        if (e.getClickedBlock() != null && player.isSneaking()) {
            Block clicked = e.getClickedBlock();
            Material type = clicked.getType();

            boolean allowed =
                    type == Material.FURNACE ||
                    (type == Material.SMOKER && PortableFurnacePlus.INSTANCE.allowSmoker()) ||
                    (type == Material.BLAST_FURNACE && PortableFurnacePlus.INSTANCE.allowBlastFurnace());

            if (!allowed) return;

            Location loc = clicked.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            // store data
            pdc.set(KEY_LINKED_LOC, PersistentDataType.STRING, locStr);
            PortableFurnacePlus.INSTANCE.getPlayerLinkedFurnaces().put(player.getUniqueId(), loc);
            addForcedChunkIfOnline(player, loc);

            // Check owner is set if not present
            if (!pdc.has(KEY_OWNER, PersistentDataType.STRING)) {
                pdc.set(KEY_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());
            }

            item.setItemMeta(meta);
            player.sendMessage(MessageFile.get("furnace-linked")
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()))
                    .replace("%world%", loc.getWorld().getName()));
            e.setCancelled(true);
        }

        // Ownership check 
        if (pdc.has(KEY_OWNER, PersistentDataType.STRING)) {
            String ownerUuid = pdc.get(KEY_OWNER, PersistentDataType.STRING);
            if (!player.getUniqueId().toString().equals(ownerUuid)) {
                player.sendMessage(MessageFile.get("not-owner"));
                e.setCancelled(true);
                return;
            }
        } 
        // Open real furnace at that location
        if (pdc.has(KEY_LINKED_LOC, PersistentDataType.STRING)) {
            String locStr = pdc.get(KEY_LINKED_LOC, PersistentDataType.STRING);
            String[] parts = locStr.split(",");
            if (parts.length == 4) {
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.FURNACE || type == Material.SMOKER || type == Material.BLAST_FURNACE) {
                        Furnace furnace = (Furnace) block.getState();
                        addForcedChunkIfOnline(player, loc);
                        player.openInventory(furnace.getInventory());
                        e.setCancelled(true);
                        return;
                    } else {
                        player.sendMessage(MessageFile.get("furnace-unlinked"));
                        pdc.remove(KEY_LINKED_LOC);
                        removeForcedChunk(loc);
                        playerLinkedFurnaces.remove(player.getUniqueId());
                        item.setItemMeta(meta);
                        return;
                    }
                } else {
                    player.sendMessage(MessageFile.get("world-missing"));
                    return;
                }
            } else {
                player.sendMessage(MessageFile.get("invalid-data"));
                return;
            }
        }

        // If not linked
        if (!pdc.has(KEY_LINKED_LOC, PersistentDataType.STRING)) {
            player.sendMessage(MessageFile.get("furnace-unlinked"));
            e.setCancelled(true);
            return;
        }
    }

    // Save GUI back into the item when closed
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (!openInventoryToItem.containsKey(inv)) return;

        ItemStack item = openInventoryToItem.remove(inv);
        if (!isPortable(item)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        try {
            ItemStack[] arr = new ItemStack[3];
            if (inv instanceof FurnaceInventory) {
                FurnaceInventory finv = (FurnaceInventory) inv;
                arr[0] = limitInputStack(finv.getSmelting());
                arr[1] = finv.getFuel();
                arr[2] = finv.getResult();
            } else {
                arr[0] = limitInputStack(inv.getItem(0));
                arr[1] = inv.getItem(1);
                arr[2] = inv.getItem(2);
            }

            ItemSerialization.saveItemStackArray(pdc, KEY_INV, arr);

            item.setItemMeta(meta);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static ItemStack safeLimitedClone(ItemStack s) {
        if (s == null) return null;
        ItemStack c = s.clone();
        if (c.getAmount() > MAX_INPUT_STACK) c.setAmount(MAX_INPUT_STACK);
        return c;
    }

    private static ItemStack limitInputStack(ItemStack in) {
        if (in == null) return null;
        if (in.getAmount() > MAX_INPUT_STACK) in.setAmount(MAX_INPUT_STACK);
        return in;
    }

    public static void processAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory pInv = player.getInventory();
            for (int i = 0; i < pInv.getSize(); i++) {
                ItemStack it = pInv.getItem(i);
                if (isPortable(it)) {
                    processSingleFurnaceForPlayer(pInv, i, it);
                }
            }
            ItemStack off = pInv.getItemInOffHand();
            if (isPortable(off)) {
                processSingleFurnaceForPlayer(pInv, -1, off);
            }
        }
    }

    private static void processSingleFurnaceForPlayer(Inventory playerInv, int index, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        try {
            ItemStack[] arr = ItemSerialization.loadItemStackArray(pdc, KEY_INV);
            if (arr == null) arr = new ItemStack[3];

            int burn = pdc.getOrDefault(KEY_BURN, PersistentDataType.INTEGER, 0);
            int burnTotal = pdc.getOrDefault(KEY_BURN_TOTAL, PersistentDataType.INTEGER, 0);
            int cook = pdc.getOrDefault(KEY_COOK, PersistentDataType.INTEGER, 0);
            int cookTotal = pdc.getOrDefault(KEY_COOK_TOTAL, PersistentDataType.INTEGER, 10);

            ItemStack input = arr[0];
            ItemStack fuel = arr[1];
            ItemStack output = arr[2];

            // find recipe result
            ItemStack result = findSmeltResult(input);
            if (result == null) {
                cook = 0;
                pdc.set(KEY_COOK, PersistentDataType.INTEGER, cook);
                item.setItemMeta(meta);
                return;
            }

            if (burn <= 0) {
                if (fuel != null && FUEL_TIMES.containsKey(fuel.getType())) {
                    int fuelTicks = FUEL_TIMES.get(fuel.getType());
                    burnTotal = fuelTicks;
                    burn = burnTotal;
                    fuel.setAmount(fuel.getAmount() - 1);
                    if (fuel.getAmount() <= 0) fuel = null;
                } else {
                    pdc.set(KEY_BURN, PersistentDataType.INTEGER, 0);
                    pdc.set(KEY_BURN_TOTAL, PersistentDataType.INTEGER, 0);
                    arr[0] = limitInputStack(input);
                    arr[1] = fuel;
                    arr[2] = output;
                    ItemSerialization.saveItemStackArray(pdc, KEY_INV, arr);
                    item.setItemMeta(meta);
                    return;
                }
            }

            if (input != null && burn > 0) {
                if (cookTotal <= 0) cookTotal = 10;
                cook++;
                burn--;

                if (cook >= cookTotal) {
                    if (output == null) {
                        output = result.clone();
                    } else if (output.isSimilar(result)) {
                        int space = output.getMaxStackSize() - output.getAmount();
                        int toAdd = Math.min(space, result.getAmount());
                        output.setAmount(output.getAmount() + toAdd);
                    } else {
                        cook = cookTotal;
                    }

                    int consumed = result.getAmount();
                    int newAmt = input.getAmount() - consumed;
                    if (newAmt <= 0) {
                        input = null;
                    } else {
                        input.setAmount(newAmt);
                        if (input.getAmount() > MAX_INPUT_STACK) input.setAmount(MAX_INPUT_STACK);
                    }
                    cook = 0;
                }
            }

            pdc.set(KEY_BURN, PersistentDataType.INTEGER, Math.max(burn, 0));
            pdc.set(KEY_BURN_TOTAL, PersistentDataType.INTEGER, Math.max(burnTotal, 0));
            pdc.set(KEY_COOK, PersistentDataType.INTEGER, Math.max(cook, 0));
            pdc.set(KEY_COOK_TOTAL, PersistentDataType.INTEGER, Math.max(cookTotal, 10));

            arr[0] = limitInputStack(input);
            arr[1] = fuel;
            arr[2] = output;
            ItemSerialization.saveItemStackArray(pdc, KEY_INV, arr);
            item.setItemMeta(meta);

            // if GUI open for this item, update it
            for (Map.Entry<Inventory, ItemStack> e : new HashMap<>(openInventoryToItem).entrySet()) {
                if (e.getValue() == item) {
                    Inventory gui = e.getKey();
                    if (gui instanceof FurnaceInventory) {
                        FurnaceInventory finv = (FurnaceInventory) gui;
                        finv.setSmelting(arr[0]);
                        finv.setFuel(arr[1]);
                        finv.setResult(arr[2]);
                    } else {
                        gui.setItem(0, arr[0]);
                        gui.setItem(1, arr[1]);
                        gui.setItem(2, arr[2]);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // find furnace smelt result using server recipes
    private static ItemStack findSmeltResult(ItemStack input) {
        if (input == null) return null;
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof FurnaceRecipe) {
                FurnaceRecipe fr = (FurnaceRecipe) r;
                RecipeChoice choice = fr.getInputChoice();
                if (choice instanceof RecipeChoice.MaterialChoice) {
                    for (Material m : ((RecipeChoice.MaterialChoice) choice).getChoices()) {
                        if (m == input.getType()) return fr.getResult().clone();
                    }
                } else {
                    if (fr.getInput().getType() == input.getType()) return fr.getResult().clone();
                }
            }
        }
        return null;
    }
}
