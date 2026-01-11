package me.revqz.pets;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Random;

public class EggSystem implements Listener {

    private final Pets plugin;
    private final Random random = new Random();

    public EggSystem(Pets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlaceEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getPetItemManager().isEggItem(item)) return;

        event.setCancelled(true);
        Location loc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

        item.setAmount(item.getAmount() - 1);

        ItemDisplay display = player.getWorld().spawn(loc.clone().add(0, 0.5, 0), ItemDisplay.class);
        display.setItemStack(plugin.getPetItemManager().createMysteryEgg(null));
        display.setBillboard(Display.Billboard.FIXED);
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));

        Interaction interaction = player.getWorld().spawn(loc, Interaction.class);
        interaction.setInteractionHeight(1.0f);
        interaction.setInteractionWidth(1.0f);
        interaction.getPersistentDataContainer().set(plugin.getPetItemManager().eggKey, PersistentDataType.BYTE, (byte) 1);

        player.playSound(loc, Sound.ENTITY_ITEM_FRAME_PLACE, 1f, 1f);
    }

    public void startCracking(Player player, Interaction interaction, ItemDisplay display) {
        interaction.remove();
        Location loc = display.getLocation();
        PetItemManager.Rarity rarity = plugin.getPetItemManager().rollRarity();

        new BukkitRunnable() {
            int tick = 0;
            float rotation = 0;
            float height = 0;

            @Override
            public void run() {
                tick++;

                height += 0.02; // Float up
                rotation += (tick * 0.02); // Accelerate rotation

                display.teleport(loc.clone().add(0, height, 0));

                Transformation t = display.getTransformation();
                t.getLeftRotation().set(new AxisAngle4f(rotation, 0, 1, 0));

                // Beat/Pulse effect
                if (tick % 10 == 0) {
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.5f + (tick / 50f));
                    player.getWorld().spawnParticle(Particle.CLOUD, display.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                }

                display.setTransformation(t);

                if (tick >= 50) {
                    this.cancel();
                    display.remove();
                    spawnReward(player, display.getLocation(), rarity);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnReward(Player player, Location loc, PetItemManager.Rarity rarity) {
        int strength = plugin.getStrengthForRarity(rarity);

        PetItemManager.Variant variant = PetItemManager.Variant.NORMAL;
        double roll = random.nextDouble();
        if (roll < 0.01) variant = PetItemManager.Variant.RAINBOW;
        else if (roll < 0.05) variant = PetItemManager.Variant.DIAMOND;
        else if (roll < 0.15) variant = PetItemManager.Variant.GOLD;

        ItemStack petItem = plugin.getPetItemManager().createPetItem("MHF_" + rarity.name(), strength, rarity, variant, 1, 0);

        // --- FIXED LINES BELOW ---
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 0.5, 0), 1);
        player.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
        // -------------------------

        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.0f);
        player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        ItemDisplay rewardDisplay = player.getWorld().spawn(loc.clone().add(0, 0.5, 0), ItemDisplay.class);
        rewardDisplay.setItemStack(petItem);
        rewardDisplay.setBillboard(Display.Billboard.CENTER);

        TextDisplay title = player.getWorld().spawn(loc.clone().add(0, 1.2, 0), TextDisplay.class);
        title.setText(rarity.getColor() + "" + ChatColor.BOLD + rarity.name());
        title.setBillboard(Display.Billboard.CENTER);

        player.getInventory().addItem(petItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                rewardDisplay.remove();
                title.remove();
            }
        }.runTaskLater(plugin, 60L);
    }
}