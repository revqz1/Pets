package me.revqz.pets;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
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
        player.sendMessage(ChatColor.GREEN + "Pets moving to mine " + block.getType().name() + "!");
    }

    public boolean isMining(Player player) { return activeTargets.containsKey(player.getUniqueId()); }
    public void stopMining(Player player) { activeTargets.remove(player.getUniqueId()); }

    public void tickMining(Player player, List<Pets.ActivePet> pets, double tick) {
        Block targetBlock = activeTargets.get(player.getUniqueId());

        // Validation check
        if (targetBlock == null || targetBlock.getType() == Material.AIR || targetBlock.getLocation().distance(player.getLocation()) > 15) {
            stopMining(player);
            return;
        }

        Location blockLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        double totalStrength = 0;

        for (int i = 0; i < pets.size(); i++) {
            Pets.ActivePet activePet = pets.get(i);
            ItemDisplay model = activePet.model;

            if (activePet.isAnimating) continue; // Don't interrupt level up animation

            // --- MOVEMENT LOGIC (Walking) ---
            double angle = (i * (Math.PI * 2 / pets.size()));
            Location targetPos = blockLoc.clone().add(Math.cos(angle), 0.5, Math.sin(angle));

            // Make pet face the block
            targetPos.setDirection(blockLoc.toVector().subtract(targetPos.toVector()));

            Location currentPos = model.getLocation();
            double distance = currentPos.distance(targetPos);

            if (distance > 1.0) {
                // If far away, "Walk" towards the target
                Vector dir = targetPos.toVector().subtract(currentPos.toVector()).normalize().multiply(0.4); // 0.4 speed
                Location nextStep = currentPos.clone().add(dir);

                // Add a little bobbing for walking effect
                nextStep.setY(targetPos.getY() + Math.sin(tick * 0.4) * 0.1);

                // Maintain look direction
                nextStep.setDirection(targetPos.toVector().subtract(nextStep.toVector()));

                activePet.teleport(nextStep);

                // Skip mining damage while walking
                continue;
            } else {
                // We are close enough: Snap to position and Mine
                activePet.teleport(targetPos);
            }

            // --- MINING ANIMATION & DAMAGE ---
            float attackPitch = (float) Math.sin(tick * 0.8) * 45;
            Transformation transform = model.getTransformation();
            transform.getLeftRotation().set(new AxisAngle4f((float)Math.toRadians(attackPitch), 1, 0, 0));
            model.setTransformation(transform);

            // Particles
            if (tick % 10 == 0) {
                model.getWorld().spawnParticle(Particle.CRIT, blockLoc, 3, 0.3, 0.3, 0.3, 0.1);
                model.getWorld().spawnParticle(Particle.BLOCK, blockLoc, 5, 0.2, 0.2, 0.2, targetBlock.getBlockData());
            }

            // Accumulate Strength
            totalStrength += calculateTotalDamage(model.getItemStack());
        }

        // Only damage block if pets are actually hitting it (strength > 0)
        if (totalStrength > 0) {
            double damagePerTick = totalStrength / 20.0;
            double currentDmg = blockDamage.getOrDefault(targetBlock, 0.0) + damagePerTick;
            blockDamage.put(targetBlock, currentDmg);

            // --- BLOCK BREAK ---
            if (currentDmg >= blockHealth.getOrDefault(targetBlock.getType(), 20)) {
                targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.STEP_SOUND, targetBlock.getType());
                targetBlock.breakNaturally();

                // Fire the Custom Event for every pet involved
                for (Pets.ActivePet p : pets) {
                    Bukkit.getPluginManager().callEvent(new PetEvents.PetBlockBreakEvent(player, p, targetBlock));
                }

                blockDamage.remove(targetBlock);
                stopMining(player);
            }
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

    public void playLevelUpAnimation(Pets.ActivePet pet) {
        pet.isAnimating = true;
        // Float up
        pet.model.teleport(pet.model.getLocation().add(0, 0.5, 0));

        new BukkitRunnable() {
            float angle = 0;
            @Override
            public void run() {
                angle += 25; // Spin speed
                Transformation t = pet.model.getTransformation();
                t.getLeftRotation().set(new AxisAngle4f((float)Math.toRadians(angle), 0, 1, 0));
                pet.model.setTransformation(t);

                pet.model.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, pet.model.getLocation().add(0, 0.5, 0), 1);

                if (angle >= 360 * 2) { // Spin twice
                    pet.isAnimating = false;
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}