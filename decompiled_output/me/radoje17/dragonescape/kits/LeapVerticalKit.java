package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class LeapVerticalKit extends Kit {
   private static double mult = 1.42;
   private static double y = -0.01;
   private static double horizontal = -0.3;
   private static double yAbove = 0.0;
   private HashMap<Player, Integer> limit = new HashMap<>();

   public LeapVerticalKit() {
      super(KitType.LEAPCLASSIC, Material.IRON_AXE, Lang.getList("leap-vertical-lore"));
      this.breaksBlocks = false;
      this.publicFriendly = false;
      ItemStack leapAxe = new ItemStack(Material.IRON_AXE);
      ItemMeta leapMeta = leapAxe.getItemMeta();
      leapMeta.setDisplayName(ChatColor.WHITE + "Leap axe");
      leapAxe.setItemMeta(leapMeta);
      this.items.put(0, leapAxe);
      if (this.section != null) {
         mult = this.section.getDouble("mult");
         horizontal = this.section.getDouble("horizontal");
         y = this.section.getDouble("y");
         yAbove = this.section.getDouble("yAbove");
      }

      this.menuItem = new ItemStack(Material.IRON_AXE);
      ItemMeta menuMeta = this.menuItem.getItemMeta();
      menuMeta.setDisplayName(ChatColor.WHITE + "Leap Classic");
      List<String> lore = new ArrayList<>();

      for (String s : Lang.getList("leap-vertical-lore")) {
         lore.add(ChatColor.WHITE + s);
      }

      menuMeta.setLore(lore);
      this.menuItem.setItemMeta(menuMeta);
      this.type = this.type;
   }

   @Override
   public void giveItems(Player p) {
      super.giveItems(p);
      this.limit.put(p, 4);
   }

   public static void update() {
      mult = DragonEscape.getConfiguration().getDouble("kits.leapclassic.mult");
      horizontal = DragonEscape.getConfiguration().getDouble("kits.leapclassic.horizontal");
      y = DragonEscape.getConfiguration().getDouble("kits.leapclassic.y");
      yAbove = DragonEscape.getConfiguration().getDouble("kits.leapclassic.yAbove");
   }

   @EventHandler
   @Override
   public void event(PlayerInteractEvent e) {
      Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
      if (KitManager.getKit(e.getPlayer()) == this || g == null) {
         if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getPlayer().getItemInHand() != null && e.getPlayer().getItemInHand().getType() == Material.IRON_AXE) {
               if (g == null || KitManager.testCooldown(e.getPlayer())) {
                  if (this.limit.containsKey(e.getPlayer()) && g != null && this.limit.get(e.getPlayer()) == 0) {
                     e.getPlayer().sendMessage(Lang.getMessage("no-leaps-left"));
                  } else {
                     Player player = e.getPlayer();
                     double yDirection = e.getPlayer().getLocation().getDirection().getY();
                     Vector dir = player.getLocation().getDirection();
                     Vector horizontal = player.getLocation().getDirection().setY(0);
                     double hor = LeapVerticalKit.horizontal;
                     double mult = LeapVerticalKit.mult;
                     double y = LeapVerticalKit.y;
                     double yAbove = LeapVerticalKit.yAbove;
                     if (yDirection >= 0.89717) {
                        dir.add(new Vector(0.0, yAbove, 0.0)).multiply(mult);
                     } else {
                        dir.add(new Vector(0.0, y, 0.0)).multiply(mult);
                     }

                     horizontal.multiply(hor);
                     player.setVelocity(dir.add(horizontal));
                     e.getPlayer().getWorld().playEffect(e.getPlayer().getLocation(), Effect.BLAZE_SHOOT, 0);
                     if (this.limit.containsKey(e.getPlayer()) && g != null) {
                        KitManager.setCooldown(player, this.cooldown);
                        this.limit.put(e.getPlayer(), this.limit.get(e.getPlayer()) - 1);
                        player.sendMessage(
                           Lang.getMessage("leap-uses-left")
                              .replaceAll("%uses%", "" + this.limit.get(player))
                              .replaceAll("%use%", this.limit.get(player) == 1 ? "use" : "uses")
                        );
                     }
                  }
               }
            }
         }
      }
   }
}
