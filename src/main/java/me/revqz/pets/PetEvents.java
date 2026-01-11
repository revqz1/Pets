package me.revqz.pets;

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

            givePetXP(player, activePet, petItem, 50);
        }

        private void givePetXP(Player player, Pets.ActivePet activePet, ItemStack item, int amount) {
            PetItemManager pm = plugin.getPetItemManager();
            double currentXP = pm.getXP(item);
            int level = pm.getLevel(item);
            double reqXp = pm.getRequiredXP(level);
            int evo = pm.getEvo(item);

            currentXP += amount;

            if (currentXP >= reqXp) {
                currentXP = currentXP - reqXp;
                level++;
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "LEVEL UP! " + ChatColor.WHITE + "Your pet is now Level " + level);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2);
                plugin.getMiningSystem().playLevelUpAnimation(activePet);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }

            if (item.getItemMeta() instanceof SkullMeta meta) {
                meta.getPersistentDataContainer().set(pm.xpKey, PersistentDataType.DOUBLE, currentXP);
                meta.getPersistentDataContainer().set(pm.levelKey, PersistentDataType.INTEGER, level);

                // FIXED: 7 Arguments
                pm.updatePetLore(meta, pm.getRarity(item), pm.getStrength(item), pm.getVariant(item), level, currentXP, evo);
                item.setItemMeta(meta);

                activePet.model.setItemStack(item);
                activePet.updateHologram(level, currentXP, pm.getRequiredXP(level), evo);
                updatePlayerInventory(player, item);
            }
        }

        private void updatePlayerInventory(Player player, ItemStack updatedItem) {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack is = contents[i];
                if (is != null && plugin.getPetItemManager().isPetItem(is)) {
                    if (is.getItemMeta().getDisplayName().equals(updatedItem.getItemMeta().getDisplayName())) {
                        player.getInventory().setItem(i, updatedItem);
                        break;
                    }
                }
            }
        }
    }
}