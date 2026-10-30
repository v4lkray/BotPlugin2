
package com.example.botplugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class BotNpc {

    public static class Settings {
        public boolean slowFalling = false;
        public boolean shielding = false;
        public boolean lookAt = false;
        public boolean fireRes = false;
        public final boolean[] blast = new boolean[4];
    }

    private final ServerPlayer npc;
    private final Player bukkit;

    private BotNpc(ServerPlayer npc) {
        this.npc = npc;
        this.bukkit = (Player) npc.getBukkitEntity();
    }

    public static BotNpc spawn(Location loc, String name, String textureValue, String textureSignature) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) loc.getWorld()).getHandle();

        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("bot:" + name).getBytes()),
                name
        );

        if (textureValue != null && !textureValue.isEmpty()) {
            profile.properties().put(
                    "textures",
                    new Property("textures", textureValue, textureSignature)
            );
        }

        ServerPlayer npc = new ServerPlayer(
                server,
                level,
                profile,
                ClientInformation.createDefault()
        );

        npc.setInvulnerable(true);

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);

        server.getPlayerList().placeNewPlayer(
                connection,
                npc,
                CommonListenerCookie.createInitial(profile, false)
        );

        BotNpc bot = new BotNpc(npc);
        bot.bukkit.teleport(loc);

        return bot;
    }

    public void despawn() {
        if (npc.connection != null) {
            npc.connection.disconnect(Component.literal("Bot despawned"));
        }
    }

    public void refreshEquipment(Settings s) {
        PlayerInventory inv = bukkit.getInventory();

        inv.setArmorContents(new ItemStack[]{
                armor(Material.NETHERITE_BOOTS, s.blast[3]),
                armor(Material.NETHERITE_LEGGINGS, s.blast[2]),
                armor(Material.NETHERITE_CHESTPLATE, s.blast[1]),
                armor(Material.NETHERITE_HELMET, s.blast[0])
        });

        inv.setItemInMainHand(new ItemStack(Material.TOTEM_OF_UNDYING));

        inv.setItemInOffHand(
                s.shielding
                        ? new ItemStack(Material.SHIELD)
                        : new ItemStack(Material.TOTEM_OF_UNDYING)
        );
    }

    public void tick(Settings s) {
        bukkit.setFoodLevel(20);
        bukkit.setSaturation(20f);

        var maxHealth = bukkit.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            bukkit.setHealth(maxHealth.getValue());
        }

        applyEffect(PotionEffectType.SLOW_FALLING, s.slowFalling);
        applyEffect(PotionEffectType.FIRE_RESISTANCE, s.fireRes);

        if (s.shielding) {
            if (!npc.isUsingItem()) {
                npc.startUsingItem(InteractionHand.OFF_HAND);
            }
        } else if (npc.isUsingItem()) {
            npc.stopUsingItem();
        }

        if (s.lookAt) {
            Player nearest = null;
            double best = 20 * 20;

            for (Player p : bukkit.getWorld().getPlayers()) {
                if (p.equals(bukkit)) continue;

                double d = p.getLocation().distanceSquared(bukkit.getLocation());

                if (d < best) {
                    best = d;
                    nearest = p;
                }
            }

            if (nearest != null) {
                Location eye = nearest.getEyeLocation();

                npc.lookAt(
                        EntityAnchorArgument.Anchor.EYES,
                        new Vec3(eye.getX(), eye.getY(), eye.getZ())
                );
            }
        }
    }

    private void applyEffect(PotionEffectType type, boolean on) {
        if (on) {
            bukkit.addPotionEffect(
                    new PotionEffect(
                            type,
                            PotionEffect.INFINITE_DURATION,
                            0,
                            false,
                            false
                    )
            );
        } else {
            bukkit.removePotionEffect(type);
        }
    }

    private static ItemStack armor(Material material, boolean blastProtection) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.addEnchant(
                blastProtection
                        ? Enchantment.BLAST_PROTECTION
                        : Enchantment.PROTECTION,
                4,
                true
        );

        item.setItemMeta(meta);
        return item;
    }
}

