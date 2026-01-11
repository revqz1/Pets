package me.revqz.pets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class PetEvents {

    // --- CUSTOM EVENT ---
    public static class PetBlockBreakEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Pets.ActivePet pet;
        private final Block block;

        public PetBlockBreakEvent(Player player, Pets.ActivePet pet, Block block) {
            this.player = player;
            this.pet = pet;
            this.block = block;
        }

        public Player getPlayer() { return player; }
        public Pets.ActivePet getPet() { return pet; }
        public Block getBlock() { return block; }

        public static HandlerList getHandlerList() { return handlers; }
        @Override
        public HandlerList getHandlers() { return handlers; }
    }

    // --- LISTENER ---
    public static class PetExperienceListener implements Listener {
        private final Pets plugin;

        public PetExperienceListener(Pets plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onPetBreakBlock(PetBlockBreakEvent event) {
            Player player = event.getPlayer();
            Pets.ActivePet activePet = event.getPet();
            ItemStack petItem = activePet.model.getItemStack();

            // Give XP Reward (e.g., 50 XP per block break)
            int xpReward = 50;

            givePetXP(player, activePet, petItem, xpReward);
        }

        private void givePetXP(Player player, Pets.ActivePet activePet, ItemStack item, int amount) {
            PetItemManager pm = plugin.getPetItemManager();
            double currentXP = pm.getXP(item);
            int level = pm.getLevel(item);
            double reqXp = pm.getRequiredXP(level);

            // Add XP
            currentXP += amount;

            // Level Up Logic
            if (currentXP >= reqXp) {
                currentXP = currentXP - reqXp; // Rollover XP
                level++;
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "LEVEL UP! " + ChatColor.WHITE + "Your pet is now Level " + level);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2);

                // Trigger the Twist Animation
                plugin.getMiningSystem().playLevelUpAnimation(activePet);
            } else {
                // Small sound for XP gain
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }

            // Update NBT Data on the ItemStack
            if (item.getItemMeta() instanceof SkullMeta meta) {
                meta.getPersistentDataContainer().set(pm.xpKey, PersistentDataType.DOUBLE, currentXP);
                meta.getPersistentDataContainer().set(pm.levelKey, PersistentDataType.INTEGER, level);

                // Update Lore
                pm.updatePetLore(meta, PetItemManager.Rarity.COMMON, pm.getStrength(item), pm.getVariant(item), level, currentXP);
                item.setItemMeta(meta);

                // 1. Update the Floating Head
                activePet.model.setItemStack(item);

                // 2. Update the Hologram text
                activePet.updateHologram(level, currentXP, pm.getRequiredXP(level));

                // 3. IMPORTANT: Update the Player's Inventory
                // We must find the matching item in the inventory and replace it
                updatePlayerInventory(player, item);
            }
        }

        private void updatePlayerInventory(Player player, ItemStack updatedItem) {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack is = contents[i];
                if (is != null && plugin.getPetItemManager().isPetItem(is)) {
                    // Simple check: if names match, we assume it's the same pet being leveled.
                    // (For a more advanced system, you would add a unique UUID to every pet item)
                    if (is.getItemMeta().getDisplayName().equals(updatedItem.getItemMeta().getDisplayName())) {
                        player.getInventory().setItem(i, updatedItem);
                        break;
                    }
                }
            }
        }
    }
}