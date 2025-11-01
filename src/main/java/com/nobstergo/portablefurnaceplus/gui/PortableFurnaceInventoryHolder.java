package com.nobstergo.portablefurnaceplus.gui;

import com.nobstergo.portablefurnaceplus.item.PortableFurnaceItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PortableFurnaceInventoryHolder implements InventoryHolder, Listener {

    private final ItemStack linkedItem;

    public PortableFurnaceInventoryHolder(ItemStack item) {
        this.linkedItem = item;
    }

    @Override
    public Inventory getInventory() {
        return null; 
    }

    // Prevent players from doing any funky actions
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        if (!(e.getInventory().getHolder() instanceof PortableFurnaceInventoryHolder)) return;
        int slot = e.getRawSlot();
        if (slot == 1 && e.getCursor() != null) {
            ItemStack cursor = e.getCursor();
            if (cursor.getAmount() + (e.getCurrentItem() != null ? e.getCurrentItem().getAmount() : 0) > PortableFurnaceItem.MAX_INPUT_STACK) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // Prevent player from shift-clicking items incorrectly
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PortableFurnaceInventoryHolder) {
            for (int slot : e.getRawSlots()) {
                if (slot == 1) { 
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}
