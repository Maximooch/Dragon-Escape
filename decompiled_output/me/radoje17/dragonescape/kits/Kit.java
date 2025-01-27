package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.radoje17.dragonescape.DragonEscape;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class Kit implements Listener {
   HashMap<Integer, ItemStack> items;
   String kitName;
   int cooldown = 0;
   ItemStack menuItem;
   KitType type;
   boolean soloFriendly = true;
   boolean publicFriendly = true;
   boolean breaksBlocks = true;
   ConfigurationSection section;

   public Kit(KitType type, Material material, List<String> description) {
      this.items = new HashMap<>();
      this.kitName = type.toString().toLowerCase();
      this.section = KitManager.getKitConfiguration().getConfigurationSection(this.kitName);
      if (this.section != null) {
         this.cooldown = this.section.getInt("cooldown");
      }

      this.menuItem = new ItemStack(material);
      ItemMeta menuMeta = this.menuItem.getItemMeta();
      menuMeta.setDisplayName(ChatColor.WHITE + this.kitName.substring(0, 1).toUpperCase() + this.kitName.substring(1));
      List<String> lore = new ArrayList<>();

      for (String s : description) {
         lore.add(ChatColor.WHITE + s);
      }

      menuMeta.setLore(lore);
      this.menuItem.setItemMeta(menuMeta);
      this.type = type;
      KitManager.addKit(this);
      Bukkit.getPluginManager().registerEvents(this, DragonEscape.getInstance());
   }

   public void giveItems(Player p) {
      for (int index : this.items.keySet()) {
         p.getInventory().setItem(index, this.items.get(index));
      }
   }

   @EventHandler
   public abstract void event(PlayerInteractEvent var1);

   public KitType getType() {
      return this.type;
   }

   public String getKitName() {
      return this.kitName;
   }

   public ItemStack getMenuItem() {
      return this.menuItem;
   }

   public boolean isSoloFriendly() {
      return this.soloFriendly;
   }

   public boolean isPublicFriendly() {
      return this.publicFriendly;
   }

   public boolean isBreaksBlocks() {
      return this.breaksBlocks;
   }

   public boolean isPracticeFriendly() {
      return this.soloFriendly && !this.breaksBlocks;
   }
}
