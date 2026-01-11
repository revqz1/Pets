package me.revqz.pets;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager implements Listener {

    private final Pets plugin;
    private final ItemStack filler;

    public InventoryManager(Pets plugin) {
        this.plugin = plugin;
        this.filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
    }

    public void openStorage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Pet Storage");

        int[] border = {0,1,2,3,4,5,6,7,8, 45,46,47,48,49,50,51,52,53};
        for (int i : border) inv.setItem(i, filler);

        // FIXED: Using loadPetData
        List<ItemStack> pets = plugin.getDataManager().loadPetData(player);
        int slot = 9;
        for (ItemStack pet : pets) {
            if (slot >= 45) break;
            inv.setItem(slot++, pet);
        }

        ItemStack fuseBtn = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = fuseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Fusion Bench (Click)");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Combine 3 identical pets");
        lore.add(ChatColor.GRAY + "to upgrade Evolution!");
        meta.setLore(lore);
        fuseBtn.setItemMeta(meta);
        inv.setItem(49, fuseBtn);

        player.openInventory(inv);
    }

    public void attemptFusion(Player player) {
        // FIXED: Using loadPetData
        List<ItemStack> storage = plugin.getDataManager().loadPetData(player);
        PetItemManager pm = plugin.getPetItemManager();

        for (int i = 0; i < storage.size(); i++) {
            ItemStack p1 = storage.get(i);
            List<Integer> matches = new ArrayList<>();
            matches.add(i);

            for (int j = i + 1; j < storage.size(); j++) {
                ItemStack p2 = storage.get(j);
                if (isSimilar(pm, p1, p2)) {
                    matches.add(j);
                    if (matches.size() == 3) break;
                }
            }

            if (matches.size() == 3) {
                matches.sort((a, b) -> b - a);
                for(int index : matches) plugin.getDataManager().removePetFromStorage(player, index);

                int newEvo = pm.getEvo(p1) + 1;
                String skin = "MHF_" + pm.getRarity(p1).name();
                if(p1.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta sm && sm.hasOwner()) {
                    if (sm.getOwningPlayer() != null && sm.getOwningPlayer().getName() != null) {
                        skin = sm.getOwningPlayer().getName();
                    }
                }

                // FIXED: 7 Arguments
                ItemStack newPet = pm.createPetItem(
                        skin,
                        pm.getStrength(p1) + 5,
                        pm.getRarity(p1),
                        pm.getVariant(p1),
                        1, 0, newEvo
                );

                plugin.getDataManager().addPetToStorage(player, newPet);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                player.sendMessage(ChatColor.GREEN + "Fusion Successful! Created Evolution " + newEvo + " pet!");
                openStorage(player);
                return;
            }
        }
        player.sendMessage(ChatColor.RED + "You need 3 identical pets to fuse!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private boolean isSimilar(PetItemManager pm, ItemStack i1, ItemStack i2) {
        return pm.getRarity(i1) == pm.getRarity(i2) &&
                pm.getVariant(i1) == pm.getVariant(i2) &&
                pm.getEvo(i1) == pm.getEvo(i2);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.contains("Pet Storage")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.isSimilar(filler)) return;

            if (clicked.getType() == Material.ENCHANTING_TABLE) {
                attemptFusion(player);
                return;
            }

            if (plugin.getPetItemManager().isPetItem(clicked)) {
                int slot = event.getSlot();
                int index = slot - 9;

                plugin.getDataManager().removePetFromStorage(player, index);
                plugin.equipPet(player, clicked, false);
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Pet Equipped from Storage!");
            }
        }
    }
}