package me.revqz.pets;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Random;

public class EggSystem {

    private final Pets plugin;
    private final Random random = new Random();

    public EggSystem(Pets plugin) {
        this.plugin = plugin;
    }

    public void placeEggStation(Player player) {
        Location center = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        Interaction interaction = player.getWorld().spawn(center, Interaction.class);
        interaction.setInteractionHeight(1.5f);
        interaction.setInteractionWidth(1.0f);
        interaction.getPersistentDataContainer().set(plugin.getPetItemManager().eggKey, PersistentDataType.BYTE, (byte) 1);

        ItemDisplay itemDisplay = player.getWorld().spawn(center.clone().add(0, 0.5, 0), ItemDisplay.class);
        itemDisplay.setItemStack(plugin.getPetItemManager().createMysteryEgg(null));
        itemDisplay.setBillboard(Display.Billboard.FIXED);
        itemDisplay.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.2f, 1.2f, 1.2f), new AxisAngle4f()));

        TextDisplay text = player.getWorld().spawn(center.clone().add(0, 1.8, 0), TextDisplay.class);
        text.setText(ChatColor.YELLOW + "" + ChatColor.BOLD + "MYSTERY EGG\n" + ChatColor.GRAY + "Right-Click to Open!");
        text.setBillboard(Display.Billboard.CENTER);
        player.sendMessage(ChatColor.GREEN + "Egg Station placed!");
    }

    public void playHatchAnimation(Player player, Location stationLoc) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);

        Location animLoc = stationLoc.clone().add(0, 1.2, 0);
        ItemDisplay animEgg = player.getWorld().spawn(animLoc, ItemDisplay.class);
        animEgg.setItemStack(plugin.getPetItemManager().createMysteryEgg(null));
        animEgg.setBillboard(Display.Billboard.FIXED);

        PetItemManager.Rarity rarity = plugin.getPetItemManager().rollRarity();

        new BukkitRunnable() {
            int tick = 0;
            float rotationY = 0;
            float speed = 0.1f;

            @Override
            public void run() {
                tick++;
                if (tick < 40) {
                    speed *= 1.1f;
                    rotationY += speed;
                    Transformation t = animEgg.getTransformation();
                    t.getLeftRotation().set(new AxisAngle4f(rotationY, 0, 1, 0));
                    animEgg.setTransformation(t);
                    if (tick % 4 == 0) player.playSound(animLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.5f + (tick / 40f) * 1.5f);
                } else {
                    this.cancel();
                    animEgg.remove();
                    spawnReward(player, animLoc, rarity);
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

        player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.0f);

        ItemDisplay rewardDisplay = player.getWorld().spawn(loc, ItemDisplay.class);
        rewardDisplay.setItemStack(petItem);
        rewardDisplay.setBillboard(Display.Billboard.CENTER);

        TextDisplay title = player.getWorld().spawn(loc.clone().add(0, 0.7, 0), TextDisplay.class);
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