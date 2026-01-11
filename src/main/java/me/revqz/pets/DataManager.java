package me.revqz.pets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;

public class DataManager {

    private final Pets plugin;
    private final Gson gson;
    private final File file;
    private Map<UUID, List<SavedPet>> storageMap;

    public static class SavedPet {
        String ownerName;
        int strength;
        String rarity;
        String variant;
        int level;
        double xp;
        int evo;

        public SavedPet(String ownerName, int strength, String rarity, String variant, int level, double xp, int evo) {
            this.ownerName = ownerName;
            this.strength = strength;
            this.rarity = rarity;
            this.variant = variant;
            this.level = level;
            this.xp = xp;
            this.evo = evo;
        }
    }

    public DataManager(Pets plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.file = new File(plugin.getDataFolder(), "pets_storage.json");
        this.storageMap = new HashMap<>();
        load();
    }

    public void addPetToStorage(Player player, ItemStack item) {
        List<SavedPet> list = storageMap.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        PetItemManager pm = plugin.getPetItemManager();

        String skin = "MHF_Question";
        if(item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta sm) {
            if(sm.hasOwner()) {
                if (sm.getOwningPlayer() != null && sm.getOwningPlayer().getName() != null) {
                    skin = sm.getOwningPlayer().getName();
                }
            }
        }

        list.add(new SavedPet(
                skin,
                pm.getStrength(item),
                pm.getRarity(item).name(),
                pm.getVariant(item).name(),
                pm.getLevel(item),
                pm.getXP(item),
                pm.getEvo(item)
        ));
        save();
    }

    public void removePetFromStorage(Player player, int index) {
        if (!storageMap.containsKey(player.getUniqueId())) return;
        List<SavedPet> list = storageMap.get(player.getUniqueId());
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            save();
        }
    }

    // FIXED: Renamed to match Pets.java usage
    public List<ItemStack> loadPetData(Player player) {
        List<ItemStack> items = new ArrayList<>();
        if (!storageMap.containsKey(player.getUniqueId())) return items;

        for (SavedPet s : storageMap.get(player.getUniqueId())) {
            try {
                items.add(plugin.getPetItemManager().createPetItem(
                        s.ownerName, s.strength,
                        PetItemManager.Rarity.valueOf(s.rarity),
                        PetItemManager.Variant.valueOf(s.variant),
                        s.level, s.xp, s.evo
                ));
            } catch (Exception e) {
                // Ignore invalid data
            }
        }
        return items;
    }

    public void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(storageMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            storageMap = gson.fromJson(reader, new TypeToken<Map<UUID, List<SavedPet>>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}