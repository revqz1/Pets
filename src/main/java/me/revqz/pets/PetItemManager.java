package me.revqz.pets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PetItemManager {

    private final JavaPlugin plugin;
    public final NamespacedKey petKey;
    public final NamespacedKey strengthKey;
    public final NamespacedKey eggKey;
    public final NamespacedKey eggRarityKey;
    public final NamespacedKey variantKey;
    public final NamespacedKey levelKey;
    public final NamespacedKey xpKey;

    public enum Variant {
        NORMAL(1.0, ChatColor.GRAY),
        GOLD(1.5, ChatColor.GOLD),
        DIAMOND(2.5, ChatColor.AQUA),
        RAINBOW(5.0, ChatColor.LIGHT_PURPLE);

        final double multiplier;
        final ChatColor color;

        Variant(double multiplier, ChatColor color) {
            this.multiplier = multiplier;
            this.color = color;
        }
    }

    public enum Rarity {
        COMMON("#969696", 50),
        UNCOMMON("#7DBE6F", 30),
        RARE("#0C65C6", 15),
        EPIC("#7E2EFF", 4),
        SUPREME("#E8384F", 1);

        final String hex;
        final int chance;

        Rarity(String hex, int chance) {
            this.hex = hex;
            this.chance = chance;
        }

        public ChatColor getColor() { return ChatColor.of(hex); }
    }

    public PetItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.petKey = new NamespacedKey(plugin, "is_pet_item");
        this.strengthKey = new NamespacedKey(plugin, "pet_strength");
        this.eggKey = new NamespacedKey(plugin, "is_pet_egg");
        this.eggRarityKey = new NamespacedKey(plugin, "egg_rarity_lock");
        this.variantKey = new NamespacedKey(plugin, "pet_variant");
        this.levelKey = new NamespacedKey(plugin, "pet_level");
        this.xpKey = new NamespacedKey(plugin, "pet_xp");
    }

    public ItemStack createPetItem(String ownerName, int strength, Rarity rarity, Variant variant, int level, double xp) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            meta.setOwner(ownerName);
            String prefix = (variant != Variant.NORMAL) ? variant.color + "" + ChatColor.BOLD + variant.name() + " " : "";
            meta.setDisplayName(prefix + rarity.getColor() + ownerName);

            updatePetLore(meta, rarity, strength, variant, level, xp);

            meta.getPersistentDataContainer().set(petKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(strengthKey, PersistentDataType.INTEGER, strength);
            meta.getPersistentDataContainer().set(variantKey, PersistentDataType.STRING, variant.name());
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
            meta.getPersistentDataContainer().set(xpKey, PersistentDataType.DOUBLE, xp);

            if (variant != Variant.NORMAL) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void updatePetLore(SkullMeta meta, Rarity rarity, int baseStrength, Variant variant, int level, double currentXp) {
        List<String> lore = new ArrayList<>();
        int totalStrength = (int) ((baseStrength + (level * 2)) * variant.multiplier);
        double reqXp = getRequiredXP(level);

        lore.add(ChatColor.GRAY + "Rarity: " + rarity.getColor() + rarity.name());
        if (variant != Variant.NORMAL) lore.add(ChatColor.GRAY + "Variant: " + variant.color + variant.name());
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + level);
        lore.add(ChatColor.GRAY + "XP: " + ChatColor.AQUA + (int)currentXp + " / " + (int)reqXp);
        lore.add(ChatColor.GRAY + "Damage: " + ChatColor.RED + "⚔ " + totalStrength);
        lore.add("");
        lore.add(ChatColor.GRAY + "Right-click to equip!");
        meta.setLore(lore);
    }

    public double getRequiredXP(int level) {
        return 100 * Math.pow(1.2, level - 1);
    }

    public ItemStack createMysteryEgg(Rarity fixedRarity) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            String b64 = plugin.getConfig().getString("settings.egg-texture", "");
            if (!b64.isEmpty()) applyTexture(meta, b64); else meta.setOwner("MHF_Question");
            meta.setDisplayName((fixedRarity == null ? ChatColor.YELLOW + "Mystery" : fixedRarity.getColor() + fixedRarity.name()) + " Pet Egg");
            meta.getPersistentDataContainer().set(eggKey, PersistentDataType.BYTE, (byte) 1);
            if (fixedRarity != null) meta.getPersistentDataContainer().set(eggRarityKey, PersistentDataType.STRING, fixedRarity.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyTexture(SkullMeta meta, String b64) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", b64));
        meta.setPlayerProfile(profile);
    }

    public boolean isPetItem(ItemStack item) { return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(petKey, PersistentDataType.BYTE); }
    public boolean isEggItem(ItemStack item) { return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(eggKey, PersistentDataType.BYTE); }

    public Variant getVariant(ItemStack item) {
        if (!isPetItem(item)) return Variant.NORMAL;
        try { return Variant.valueOf(item.getItemMeta().getPersistentDataContainer().get(variantKey, PersistentDataType.STRING)); } catch (Exception e) { return Variant.NORMAL; }
    }

    public int getStrength(ItemStack item) { return !isPetItem(item) ? 0 : item.getItemMeta().getPersistentDataContainer().getOrDefault(strengthKey, PersistentDataType.INTEGER, 0); }
    public int getLevel(ItemStack item) { return !isPetItem(item) ? 1 : item.getItemMeta().getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1); }
    public double getXP(ItemStack item) { return !isPetItem(item) ? 0 : item.getItemMeta().getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.DOUBLE, 0.0); }

    public Rarity rollRarity() {
        int total = 0; for (Rarity r : Rarity.values()) total += r.chance;
        int roll = new Random().nextInt(total);
        int current = 0;
        for (Rarity r : Rarity.values()) { current += r.chance; if (roll < current) return r; }
        return Rarity.COMMON;
    }
}