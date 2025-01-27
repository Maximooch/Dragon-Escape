package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArcherKit extends Kit {
   private HashMap<Player, Integer> times;

   public ArcherKit() {
      super(KitType.Archer, Material.BOW, Lang.getList("archer-lore"));
      this.breaksBlocks = false;
      this.times = new HashMap<>();
      final ItemStack arrow = new ItemStack(Material.ARROW, 1);
      ItemStack bow = new ItemStack(Material.BOW, 1);
      ItemMeta bowMeta = bow.getItemMeta();
      bowMeta.spigot().setUnbreakable(true);
      bow.setItemMeta(bowMeta);
      this.items.put(1, arrow);
      this.items.put(0, bow);
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            List<Player> toRemove = new ArrayList<>();

            for (Player p : ArcherKit.this.times.keySet()) {
               Game g = DragonEscape.getInstance().getGameManager().getGame(p);
               if (g != null && g.isGameStarted() && g.isAlive(p)) {
                  int newTime = ArcherKit.this.times.get(p) + 1;
                  if (newTime == 10) {
                     ItemStack is = p.getInventory().getItem(1);
                     if (is == null) {
                        p.getInventory().setItem(1, arrow);
                     } else if (is.getAmount() < 2) {
                        p.getInventory().addItem(new ItemStack[]{arrow});
                     }

                     newTime = 10;
                  }

                  ArcherKit.this.times.put(p, newTime);
               } else {
                  toRemove.add(p);
               }
            }

            for (Player px : toRemove) {
               ArcherKit.this.times.remove(px);
            }
         }
      }, 20L, 20L);
      this.publicFriendly = false;
   }

   @EventHandler
   public void projetileLand(ProjectileHitEvent e) {
      if (e.getEntity().getShooter() instanceof Player) {
         Player p = (Player)e.getEntity().getShooter();
         if (KitManager.getKit(p) == this) {
            p.teleport(
               new Location(
                  p.getWorld(),
                  e.getEntity().getLocation().getX(),
                  e.getEntity().getLocation().getY() + 1.0,
                  e.getEntity().getLocation().getZ(),
                  p.getLocation().getYaw(),
                  p.getLocation().getPitch()
               )
            );
            e.getEntity().remove();
         }
      }
   }

   @Override
   public void giveItems(Player p) {
      super.giveItems(p);
      this.times.put(p, 0);
   }

   @Override
   public void event(PlayerInteractEvent e) {
   }
}
