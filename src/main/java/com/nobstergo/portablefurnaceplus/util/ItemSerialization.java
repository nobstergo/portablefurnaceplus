package com.nobstergo.portablefurnaceplus.util;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.io.*;
import java.util.Base64;

public class ItemSerialization {

    public static void saveItemStackArray(PersistentDataContainer pdc, org.bukkit.NamespacedKey key, ItemStack[] stacks) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
        boos.writeInt(stacks.length);
        for (ItemStack is : stacks) {
            boos.writeObject(is);
        }
        boos.flush();
        boos.close();
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        pdc.set(key, PersistentDataType.STRING, base64);
    }

    public static ItemStack[] loadItemStackArray(PersistentDataContainer pdc, org.bukkit.NamespacedKey key) throws IOException, ClassNotFoundException {
        if (!pdc.has(key, PersistentDataType.STRING)) return null;
        String base64 = pdc.get(key, PersistentDataType.STRING);
        if (base64 == null) return null;
        byte[] data = Base64.getDecoder().decode(base64);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
        int len = bois.readInt();
        ItemStack[] arr = new ItemStack[len];
        for (int i = 0; i < len; i++) {
            Object o = bois.readObject();
            if (o instanceof ItemStack) {
                arr[i] = (ItemStack) o;
            } else {
                arr[i] = null;
            }
        }
        bois.close();
        return arr;
    }
}
