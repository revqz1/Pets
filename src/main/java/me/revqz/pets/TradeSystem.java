package me.revqz.pets;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TradeSystem implements Listener {

    private final Pets plugin;
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();
    private final ItemStack filler;
    private final ItemStack readyBtn;
    private final ItemStack notReadyBtn;

    public TradeSystem(Pets plugin) {
        this.plugin = plugin;
        this.filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = filler.getItemMeta(); m.setDisplayName(" "); filler.setItemMeta(m);

        this.readyBtn = new ItemStack(Material.LIME_DYE);
        ItemMeta m2 = readyBtn.getItemMeta(); m2.setDisplayName(ChatColor.GREEN + "Ready!"); readyBtn.setItemMeta(m2);

        this.notReadyBtn = new ItemStack(Material.GRAY_DYE);
        ItemMeta m3 = notReadyBtn.getItemMeta(); m3.setDisplayName(ChatColor.RED + "Not Ready"); notReadyBtn.setItemMeta(m3);
    }

    public void startTrade(Player p1, Player p2) {
        TradeSession session = new TradeSession(p1, p2);
        activeTrades.put(p1.getUniqueId(), session);
        activeTrades.put(p2.getUniqueId(), session);
        session.open();
    }

    class TradeSession {
        Player p1, p2;
        Inventory inv;
        boolean p1Ready = false;
        boolean p2Ready = false;

        public TradeSession(Player p1, Player p2) {
            this.p1 = p1;
            this.p2 = p2;
            this.inv = Bukkit.createInventory(null, 54, "Trading...");
            setupGui();
        }

        void setupGui() {
            for(int i=4; i<50; i+=9) inv.setItem(i, filler);
            updateButtons();
        }

        void open() {
            p1.openInventory(inv);
            p2.openInventory(inv);
        }

        void updateButtons() {
            inv.setItem(48, p1Ready ? readyBtn : notReadyBtn);
            inv.setItem(50, p2Ready ? readyBtn : notReadyBtn);

            if(p1Ready && p2Ready) {
                completeTrade();
            }
        }

        void completeTrade() {
            List<ItemStack> p1Offers = getItems(true);
            List<ItemStack> p2Offers = getItems(false);

            for(ItemStack item : p1Offers) plugin.getDataManager().addPetToStorage(p2, item);
            for(ItemStack item : p2Offers) plugin.getDataManager().addPetToStorage(p1, item);

            activeTrades.remove(p1.getUniqueId());
            activeTrades.remove(p2.getUniqueId());

            p1.closeInventory();
            p2.closeInventory();

            p1.sendMessage(ChatColor.GREEN + "Trade Complete! Check your Pet Storage.");
            p2.sendMessage(ChatColor.GREEN + "Trade Complete! Check your Pet Storage.");
            p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        }

        List<ItemStack> getItems(boolean leftSide) {
            List<ItemStack> items = new ArrayList<>();
            int[] rows = {0, 9, 18, 27, 36};
            for(int rowStart : rows) {
                for(int i=0; i<4; i++) {
                    int slot = rowStart + (leftSide ? i : i + 5);
                    ItemStack item = inv.getItem(slot);
                    if(item != null && !item.getType().isAir()) items.add(item);
                }
            }
            return items;
        }

        void cancel() {
            List<ItemStack> p1Items = getItems(true);
            List<ItemStack> p2Items = getItems(false);

            for(ItemStack item : p1Items) plugin.getDataManager().addPetToStorage(p1, item);
            for(ItemStack item : p2Items) plugin.getDataManager().addPetToStorage(p2, item);

            activeTrades.remove(p1.getUniqueId());
            activeTrades.remove(p2.getUniqueId());

            p1.sendMessage(ChatColor.RED + "Trade Cancelled. Items returned to storage.");
            p2.sendMessage(ChatColor.RED + "Trade Cancelled. Items returned to storage.");
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(!activeTrades.containsKey(player.getUniqueId())) return;

        TradeSession session = activeTrades.get(player.getUniqueId());
        Inventory top = event.getView().getTopInventory();

        if(event.getClickedInventory() == top) {
            int slot = event.getSlot();

            if(slot == 48 && player.equals(session.p1)) {
                session.p1Ready = !session.p1Ready;
                session.updateButtons();
                event.setCancelled(true);
            } else if(slot == 50 && player.equals(session.p2)) {
                session.p2Ready = !session.p2Ready;
                session.updateButtons();
                event.setCancelled(true);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if(activeTrades.containsKey(event.getPlayer().getUniqueId())) {
            activeTrades.get(event.getPlayer().getUniqueId()).cancel();
        }
    }
}