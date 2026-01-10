package me.revqz.pets;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MiningSystem {

    private final Pets plugin;
    private final Map<UUID, Block> activeTargets = new HashMap<>();
    private final Map<Block, Double> blockDamage = new HashMap<>();
    private final Map<Material, Integer> blockHealth = new HashMap<>();

    public MiningSystem(Pets plugin) {
        this.plugin = plugin;
        setupBlockHealth();
    }

    private void setupBlockHealth() {
        blockHealth.put(Material.GRASS_BLOCK, 10);
        blockHealth.put(Material.DIRT, 10);
        blockHealth.put(Material.STONE, 30);
        blockHealth.put(Material.OBSIDIAN, 200);
        blockHealth.put(Material.BEDROCK, 999999);
    }

    public void startMining(Player player, Block block) {
        if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) return;
        activeTargets.put(player.getUniqueId(), block);
        blockDamage.putIfAbsent(block, 0.0);
        player.sendMessage(ChatColor.GREEN + "Pets attacking " + block.getType().name() + "!");
    }

    public boolean isMining(Player player) { return activeTargets.containsKey(player.getUniqueId()); }
    public void stopMining(Player player) { activeTargets.remove(player.getUniqueId()); }

    public void tickMining(Player player, List<Pets.ActivePet> pets, double tick) {
        Block targetBlock = activeTargets.get(player.getUniqueId());
        if (targetBlock == null || targetBlock.getType() == Material.AIR || targetBlock.getLocation().distance(player.getLocation()) > 10) {
            stopMining(player);
            return;
        }

        Location blockLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        double totalStrength = 0;

        for (int i = 0; i < pets.size(); i++) {
            Pets.ActivePet activePet = pets.get(i);
            ItemDisplay model = activePet.model;

            double angle = (i * (Math.PI * 2 / pets.size()));
            Location petLoc = blockLoc.clone().add(Math.cos(angle), 0.5, Math.sin(angle));
            petLoc.setDirection(blockLoc.toVector().subtract(petLoc.toVector()));
            petLoc.setYaw(petLoc.getYaw() + 180);
            petLoc.setPitch(0);

            float attackPitch = (float) Math.sin(tick * 0.8) * 45;
            activePet.teleport(petLoc);

            Transformation transform = model.getTransformation();
            transform.getLeftRotation().set(new AxisAngle4f((float)Math.toRadians(attackPitch), 1, 0, 0));
            model.setTransformation(transform);

            ItemStack stack = model.getItemStack();
            totalStrength += calculateTotalDamage(stack);

            if (tick % 20 == 0) givePetXP(player, activePet, stack);
            if (tick % 5 == 0) model.getWorld().spawnParticle(Particle.BLOCK, blockLoc, 3, 0.2, 0.2, 0.2, targetBlock.getBlockData());
        }

        double damagePerTick = totalStrength / 20.0;
        double currentDmg = blockDamage.getOrDefault(targetBlock, 0.0) + damagePerTick;
        blockDamage.put(targetBlock, currentDmg);

        if (currentDmg >= blockHealth.getOrDefault(targetBlock.getType(), 20)) {
            targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.STEP_SOUND, targetBlock.getType());
            targetBlock.breakNaturally();
            blockDamage.remove(targetBlock);
            stopMining(player);
        }
    }

    public int calculateTotalDamage(ItemStack item) {
        if (item == null) return 0;
        PetItemManager pm = plugin.getPetItemManager();
        int base = pm.getStrength(item);
        int level = pm.getLevel(item);
        PetItemManager.Variant variant = pm.getVariant(item);
        return (int) ((base + (level * 2)) * variant.multiplier);
    }

    private void givePetXP(Player player, Pets.ActivePet activePet, ItemStack item) {
        PetItemManager pm = plugin.getPetItemManager();
        double currentXP = pm.getXP(item);
        int level = pm.getLevel(item);

        currentXP += 10;
        double reqXp = pm.getRequiredXP(level);

        if (currentXP >= reqXp) {
            currentXP = 0;
            level++;
            player.sendMessage(ChatColor.GREEN + "Level Up! Your pet is now Level " + level);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2);
        }

        activePet.updateHologram(level, currentXP, reqXp);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.getPersistentDataContainer().set(pm.xpKey, PersistentDataType.DOUBLE, currentXP);
        meta.getPersistentDataContainer().set(pm.levelKey, PersistentDataType.INTEGER, level);

        pm.updatePetLore(meta, PetItemManager.Rarity.COMMON, pm.getStrength(item), pm.getVariant(item), level, currentXP);
        item.setItemMeta(meta);
        activePet.model.setItemStack(item);
    }
}