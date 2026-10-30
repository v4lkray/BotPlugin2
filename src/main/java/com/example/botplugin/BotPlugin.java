package com.example.botplugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BotPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private final BotNpc.Settings settings = new BotNpc.Settings();
    private BotNpc bot;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("bot").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (bot != null) bot.tick(settings);
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (bot != null) bot.despawn();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        BotMenuHolder holder = new BotMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Bot Menu"));
        holder.inventory = inv;
        render(inv);
        player.openInventory(inv);
    }

    private void render(Inventory inv) {
        inv.clear();
        inv.setItem(10, item(bot != null ? Material.RED_WOOL : Material.LIME_WOOL,
                bot != null ? "Despawn Bot" : "Spawn Bot"));
        inv.setItem(11, toggle(Material.FEATHER, "Slow Falling", settings.slowFalling));
        inv.setItem(12, toggle(Material.SHIELD, "Shielding", settings.shielding));
        inv.setItem(13, toggle(Material.ENDER_EYE, "Look at You", settings.lookAt));
        inv.setItem(14, toggle(Material.BLAZE_POWDER, "Fire Resistance", settings.fireRes));

        inv.setItem(19, armorItem(Material.NETHERITE_HELMET, "Helmet", 0));
        inv.setItem(20, armorItem(Material.NETHERITE_CHESTPLATE, "Chestplate", 1));
        inv.setItem(21, armorItem(Material.NETHERITE_LEGGINGS, "Leggings", 2));
        inv.setItem(22, armorItem(Material.NETHERITE_BOOTS, "Boots", 3));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BotMenuHolder holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getSlot();
        switch (slot) {
            case 10 -> {
                if (bot == null) {
                    String name = getConfig().getString("bot.name", "Bot");
                    String value = getConfig().getString("skin.value", "");
                    String sig = getConfig().getString("skin.signature", "");
                    bot = BotNpc.spawn(player.getLocation(), name, value, sig);
                    bot.refreshEquipment(settings);
                } else {
                    bot.despawn();
                    bot = null;
                }
            }
            case 11 -> settings.slowFalling = !settings.slowFalling;
            case 12 -> {
                settings.shielding = !settings.shielding;
                if (bot != null) bot.refreshEquipment(settings);
            }
            case 13 -> settings.lookAt = !settings.lookAt;
            case 14 -> settings.fireRes = !settings.fireRes;
            case 19, 20, 21, 22 -> {
                int armorIndex = slot - 19; // helmet=0, chest=1, legs=2, boots=3
                settings.blast[armorIndex] = !settings.blast[armorIndex];
                if (bot != null) bot.refreshEquipment(settings);
            }
            default -> { return; }
        }
        render(holder.inventory);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BotMenuHolder) event.setCancelled(true);
    }

    private static ItemStack item(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack toggle(Material material, String name, boolean on) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name + ": " + (on ? "ON" : "OFF")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack armorItem(Material material, String piece, int index) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        boolean blast = settings.blast[index];
        meta.displayName(Component.text(piece + ": " + (blast ? "Blast Protection 4" : "Protection 4")));
        meta.lore(List.of(Component.text("Click to switch")));
        stack.setItemMeta(meta);
        return stack;
    }

    private static class BotMenuHolder implements InventoryHolder {
        Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
