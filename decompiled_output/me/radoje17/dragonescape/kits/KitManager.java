package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Lang;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class KitManager implements Listener {
   private static HashMap<String, Kit> kits;
   private static List<ItemStack> kitItems;
   private static HashMap<Player, Kit> playerKits;
   private static HashMap<Player, Integer> cooldowns;
   private static List<Player> messageCooldown;

   public KitManager() {
      kits = new HashMap<>();
      kitItems = new ArrayList<>();
      playerKits = new HashMap<>();
      cooldowns = new HashMap<>();
      messageCooldown = new ArrayList<>();
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            List<Player> toRemove = new ArrayList<>();

            for (Player p : KitManager.cooldowns.keySet()) {
               int newTime = KitManager.cooldowns.get(p) - 1;
               if (newTime == 0) {
                  toRemove.add(p);
               } else {
                  KitManager.setCooldown(p, newTime);
               }
            }

            for (Player px : toRemove) {
               KitManager.removeCooldown(px);
               if (DragonEscape.getInstance().getGameManager().hasGame(px)) {
                  px.sendMessage(Lang.getMessage("kit-cooldown-expired").replaceAll("%kit%", KitManager.getKit(px).kitName));
               }
            }

            KitManager.messageCooldown.clear();
         }
      }, 20L, 20L);
      new LeapKit();
      new DisruptorKit();
      new BrewerKit();
      new FurnaceKit();
      new DiggerKit();
      new ArcherKit();
      new NoneKit();
      new LeapVerticalKit();
   }

   public static Kit getRandomSoloKit() {
      List<Kit> acceptable = new ArrayList<>();

      for (String kit : kits.keySet()) {
         if (kits.get(kit).isSoloFriendly()) {
            acceptable.add(kits.get(kit));
         }
      }

      return acceptable.get(new Random().nextInt(acceptable.size()));
   }

   public static Kit getKit(Player p) {
      if (!playerKits.containsKey(p)) {
         playerKits.put(p, getKit("leap"));
      }

      return playerKits.get(p);
   }

   public static Kit getKit(String s) {
      return s == null ? null : kits.get(s.replaceAll(" ", ""));
   }

   public static HashMap<String, Kit> getKits() {
      return kits;
   }

   public static void setKit(Player p, Kit kit) {
      playerKits.put(p, kit);
   }

   public static void setCooldown(Player p, int time) {
      cooldowns.put(p, time);
   }

   public static int getCooldown(Player p) {
      return cooldowns.containsKey(p) ? cooldowns.get(p) : -1;
   }

   public static void removeCooldown(Player p) {
      if (cooldowns.containsKey(p)) {
         cooldowns.remove(p);
      }
   }

   public static boolean testCooldown(Player p) {
      int s;
      if ((s = getCooldown(p)) != -1) {
         if (!messageCooldown.contains(p)) {
            p.sendMessage(
               Lang.getMessage("kit-cooldown").replaceAll("%kit%", getKit(p).kitName).replaceAll("%time%", s + " " + (s == 1 ? "second" : "seconds"))
            );
            messageCooldown.add(p);
         }

         return false;
      } else {
         return true;
      }
   }

   public static void addKit(Kit k) {
      kits.put(k.getKitName(), k);
      kitItems.add(k.menuItem);
   }

   public static List<ItemStack> getMenuItems() {
      return kitItems;
   }

   public static ConfigurationSection getKitConfiguration() {
      return DragonEscape.getConfiguration().getConfigurationSection("kits");
   }
}
