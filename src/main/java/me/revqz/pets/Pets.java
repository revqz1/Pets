package me.revqz.pets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public final class Pets extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Map<UUID, List<ActivePet>> playerPets = new HashMap<>();
    private final Set<UUID> traitsDisabled = new HashSet<>();
    private final Map<PetItemManager.Rarity, Integer> rarityStrengths = new HashMap<>();

    private PetItemManager petItemManager;
    private MiningSystem miningSystem;
    private EggSystem eggSystem;

    private int maxPets;
    private double bobSpeed;
    private double bobHeight;
    private double spacing;

    public static class ActivePet {
        public ItemDisplay model;
        public TextDisplay hologram;
        public boolean isAnimating = false;

        public ActivePet(ItemDisplay model, TextDisplay hologram) {
            this.model = model;
            this.hologram = hologram;
        }

        public void teleport(Location loc) {
            if (isAnimating) return;
            model.teleport(loc);
            hologram.teleport(loc.clone().add(0, 0.6, 0));
        }

        public void remove() {
            model.remove();
            hologram.remove();
        }

        public void updateHologram(int level, double xp, double maxXp) {
            hologram.setText(ChatColor.AQUA + "Lvl " + level + "\n" + ChatColor.YELLOW + (int)xp + "/" + (int)maxXp);
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        this.petItemManager = new PetItemManager(this);
        this.miningSystem = new MiningSystem(this);
        this.eggSystem = new EggSystem(this);

        Objects.requireNonNull(getCommand("pet")).setExecutor(this);
        Objects.requireNonNull(getCommand("pet")).setTabCompleter(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(eggSystem, this);

        // --- NEW LISTENER REGISTRATION ---
        getServer().getPluginManager().registerEvents(new PetEvents.PetExperienceListener(this), this);

        new BukkitRunnable() {
            double tick = 0;
            @Override
            public void run() {
                tick += 1;
                for (UUID uuid : playerPets.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        List<ActivePet> pets = playerPets.get(uuid);
                        if (pets == null || pets.isEmpty()) continue;

                        if (miningSystem.isMining(player)) {
                            miningSystem.tickMining(player, pets, tick);
                        } else {
                            updatePetFormation(player, pets, tick);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
        for (List<ActivePet> pets : playerPets.values()) pets.forEach(ActivePet::remove);
        playerPets.clear();
    }

    public List<ActivePet> getPlayerPets(UUID uuid) { return playerPets.get(uuid); }
    public PetItemManager getPetItemManager() { return petItemManager; }
    public MiningSystem getMiningSystem() { return miningSystem; }
    public int getStrengthForRarity(PetItemManager.Rarity rarity) { return rarityStrengths.getOrDefault(rarity, 1); }

    private void updatePetFormation(Player player, List<ActivePet> pets, double tick) {
        Location playerLoc = player.getLocation();
        double playerYaw = Math.toRadians(playerLoc.getYaw());

        int petsPerRow = 5;
        double spreadAngle = Math.toRadians(100);

        for (int i = 0; i < pets.size(); i++) {
            ActivePet pet = pets.get(i);
            if (pet.isAnimating) continue;

            int row = i / petsPerRow;
            int col = i % petsPerRow;

            int petsInThisRow = (row == pets.size() / petsPerRow) ? (pets.size() % petsPerRow) : petsPerRow;
            if (petsInThisRow == 0) petsInThisRow = petsPerRow;

            double radius = 2.0 + (row * 1.5);

            double angleStep = (petsInThisRow > 1) ? spreadAngle / (petsInThisRow - 1) : 0;
            double startAngle = -spreadAngle / 2;
            double offsetAngle = (petsInThisRow > 1) ? startAngle + (col * angleStep) : 0;

            double finalAngle = playerYaw + Math.PI + offsetAngle + Math.PI / 2;

            double offsetX = radius * Math.cos(finalAngle);
            double offsetZ = radius * Math.sin(finalAngle);

            Location targetLoc = playerLoc.clone().add(offsetX, 1.2 + Math.sin(tick * bobSpeed) * (bobHeight * 0.5), offsetZ);

            targetLoc.setYaw(playerLoc.getYaw() + 180);
            targetLoc.setPitch(0);

            pet.model.setTeleportDuration(2);
            pet.hologram.setTeleportDuration(2);
            pet.teleport(targetLoc);

            pet.model.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), new AxisAngle4f()));

            if (!traitsDisabled.contains(player.getUniqueId())) {
                PetItemManager.Variant var = petItemManager.getVariant(pet.model.getItemStack());
                if (var == PetItemManager.Variant.GOLD) {
                    player.getWorld().spawnParticle(Particle.WAX_OFF, pet.model.getLocation().add(0, 0.5, 0), 1);
                } else if (var == PetItemManager.Variant.DIAMOND) {
                    player.getWorld().spawnParticle(Particle.SCRAPE, pet.model.getLocation().add(0, 0.5, 0), 1);
                } else if (var == PetItemManager.Variant.RAINBOW) {
                    player.getWorld().spawnParticle(Particle.END_ROD, pet.model.getLocation().add(0, 0.5, 0), 1);
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Interaction interaction) {
            if (interaction.getPersistentDataContainer().has(petItemManager.eggKey, PersistentDataType.BYTE)) {
                ItemDisplay display = null;
                for (ItemDisplay e : interaction.getLocation().getNearbyEntitiesByType(ItemDisplay.class, 1)) {
                    if (petItemManager.isEggItem(e.getItemStack())) {
                        display = e;
                        break;
                    }
                }
                if (display != null) {
                    eggSystem.startCracking(event.getPlayer(), interaction, display);
                } else {
                    interaction.remove();
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (petItemManager.isPetItem(item)) {
                event.setCancelled(true);
                List<ActivePet> pets = playerPets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

                if (pets.size() >= maxPets) {
                    player.sendMessage(getMsg("max-pets").replace("%max%", String.valueOf(maxPets)));
                    return;
                }

                ItemDisplay petModel = player.getWorld().spawn(player.getLocation(), ItemDisplay.class);
                ItemStack petItem = item.clone();
                petItem.setAmount(1);
                petModel.setItemStack(petItem);
                petModel.setBillboard(Display.Billboard.FIXED);

                TextDisplay hologram = player.getWorld().spawn(player.getLocation().add(0,0.5,0), TextDisplay.class);
                int lvl = petItemManager.getLevel(petItem);
                double xp = petItemManager.getXP(petItem);
                double max = petItemManager.getRequiredXP(lvl);

                hologram.setText(ChatColor.AQUA + "Lvl " + lvl + "\n" + ChatColor.YELLOW + (int)xp + "/" + (int)max);
                hologram.setBillboard(Display.Billboard.CENTER);
                hologram.setBackgroundColor(org.bukkit.Color.fromARGB(0,0,0,0));

                pets.add(new ActivePet(petModel, hologram));

                item.setAmount(item.getAmount() - 1);
                player.sendMessage(getMsg("pet-equipped").replace("%pet%", petItem.getItemMeta().getDisplayName()));
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (playerPets.containsKey(player.getUniqueId()) && !playerPets.get(player.getUniqueId()).isEmpty()) {
                if (item.getType().isBlock() && item.getType().isSolid()) return;
                if (petItemManager.isEggItem(item)) return;
                if (event.getClickedBlock() != null && !event.getClickedBlock().getType().isAir()) {
                    miningSystem.startMining(player, event.getClickedBlock());
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (playerPets.containsKey(event.getPlayer().getUniqueId())) {
            if (miningSystem.isMining(event.getPlayer())) {
                miningSystem.stopMining(event.getPlayer());
                event.getPlayer().sendMessage(ChatColor.YELLOW + "Pets returning!");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (playerPets.containsKey(event.getPlayer().getUniqueId())) {
            playerPets.remove(event.getPlayer().getUniqueId()).forEach(ActivePet::remove);
            miningSystem.stopMining(event.getPlayer());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("egg") && sender instanceof Player p && p.hasPermission("pet.give")) {
            p.getInventory().addItem(petItemManager.createMysteryEgg(null));
            p.sendMessage("§aReceived a Mystery Egg! Place it to hatch!");
            return true;
        }
        if (args[0].equalsIgnoreCase("remove") && sender instanceof Player p) {
            try {
                int index = Integer.parseInt(args[1]) - 1;
                List<ActivePet> pets = playerPets.get(p.getUniqueId());
                if (pets != null && index >= 0 && index < pets.size()) {
                    ActivePet rm = pets.remove(index);
                    if (rm.model.getItemStack() != null) p.getInventory().addItem(rm.model.getItemStack());
                    rm.remove();
                    p.sendMessage(getMsg("pet-removed").replace("%num%", String.valueOf(index + 1)));
                }
            } catch (Exception e) {}
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && sender instanceof Player p && p.hasPermission("pet.give")) {
            if (args.length < 2) return true;
            String head = args[1];
            int strength = 1;
            PetItemManager.Variant variant = PetItemManager.Variant.NORMAL;
            if (args.length >= 3) try { strength = Integer.parseInt(args[2]); } catch(Exception e) {}
            if (args.length >= 4) try { variant = PetItemManager.Variant.valueOf(args[3].toUpperCase()); } catch(Exception e) {}
            p.getInventory().addItem(petItemManager.createPetItem(head, strength, PetItemManager.Rarity.COMMON, variant, 1, 0));
            p.sendMessage(getMsg("pet-given").replace("%pet%", head));
            return true;
        }
        if (args[0].equalsIgnoreCase("trait") && sender instanceof Player p) {
            if (args.length < 2) return true;
            if (args[1].equalsIgnoreCase("off")) traitsDisabled.add(p.getUniqueId());
            else traitsDisabled.remove(p.getUniqueId());
            return true;
        }
        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("pet.admin")) {
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "Pets reloaded!");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give", "remove", "egg", "trait", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("trait")) return List.of("on", "off");
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 3) return List.of("1", "5", "10");
            if (args.length == 4) return List.of("NORMAL", "GOLD", "DIAMOND", "RAINBOW");
        }
        return new ArrayList<>();
    }

    private void loadConfigValues() {
        reloadConfig();
        this.maxPets = getConfig().getInt("settings.max-pets", 4);
        this.spacing = getConfig().getDouble("settings.pet-spacing", 0.8);
        this.bobSpeed = getConfig().getDouble("settings.bob-speed", 0.15);
        this.bobHeight = getConfig().getDouble("settings.bob-height", 0.15);
        rarityStrengths.clear();
        for (PetItemManager.Rarity r : PetItemManager.Rarity.values()) {
            rarityStrengths.put(r, getConfig().getInt("rarity-strength." + r.name(), 1));
        }
    }

    private String getMsg(String key) {
        String prefix = getConfig().getString("messages.prefix", "&8[&6Pets&8] &r");
        return ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages." + key, ""));
    }
}