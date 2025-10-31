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
        return null; // not used
    }

    // Prevent players from moving the output into the input beyond the limit, or any funky actions
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        if (!(e.getInventory().getHolder() instanceof PortableFurnaceInventoryHolder)) return;
        // We allowed only clicks, but we want to restrict direct pickup of result into input beyond rules
        int slot = e.getRawSlot();
        // Input visually at slot 1, fuel at 4, output at 7. Prevent placing more than MAX_INPUT_STACK into slot 1
        if (slot == 1 && e.getCursor() != null) {
            ItemStack cursor = e.getCursor();
            if (cursor.getAmount() + (e.getCurrentItem() != null ? e.getCurrentItem().getAmount() : 0) > PortableFurnaceItem.MAX_INPUT_STACK) {
                e.setCancelled(true);
                return;
            }
        }
        // allow other normal operations within the small GUI
    }

    // Prevent player from shift-clicking items into other inventories incorrectly
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PortableFurnaceInventoryHolder) {
            for (int slot : e.getRawSlots()) {
                if (slot == 1) { // input slot
                    // check amount won't exceed
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    // Save on close is handled in the PortableFurnaceItem class InventoryCloseEvent
}
