package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class DisruptorKit extends Kit {
   private static HashMap<Player, Integer> times;
   private static HashMap<Player, Integer> leapUses;
   private static HashMap<Location, Integer> locations;
   private static HashMap<Game, List<Location>> locationsGames;
   private double intensity = 1.1;
   private double intensity2 = 0.2;
   private static ItemStack tnt;

   public DisruptorKit() {
      super(KitType.Disruptor, Material.TNT, Lang.getList("disruptor-lore"));
      times = new HashMap<>();
      leapUses = new HashMap<>();
      locations = new HashMap<>();
      locationsGames = new HashMap<>();
      ItemStack leapAxe = new ItemStack(Material.IRON_AXE);
      ItemMeta leapMeta = leapAxe.getItemMeta();
      leapMeta.setDisplayName(ChatColor.WHITE + "Leap axe");
      leapAxe.setItemMeta(leapMeta);
      this.items.put(0, leapAxe);
      tnt = new ItemStack(Material.TNT, 1);
      ItemMeta tntMeta = tnt.getItemMeta();
      tntMeta.setDisplayName(ChatColor.WHITE + "Disruptor");
      tnt.setItemMeta(tntMeta);
      this.items.put(1, tnt);
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            List<Player> toRemove = new ArrayList<>();

            for (Player p : DisruptorKit.times.keySet()) {
               Game g = DragonEscape.getInstance().getGameManager().getGame(p);
               if (g != null && g.isGameStarted() && g.isAlive(p)) {
                  int newTime = DisruptorKit.times.get(p) + 1;
                  if (newTime == 11) {
                     ItemStack is = p.getInventory().getItem(1);
                     if (is == null) {
                        p.getInventory().setItem(1, DisruptorKit.tnt);
                     } else if (is.getAmount() < 3) {
                        p.getInventory().addItem(new ItemStack[]{DisruptorKit.tnt});
                     }

                     if (is != null && is.getAmount() == 3) {
                        toRemove.add(p);
                     }

                     newTime = 1;
                  }

                  DisruptorKit.times.put(p, newTime);
                  if (!toRemove.contains(p)) {
                     int time = 11 - newTime;
                     DragonEscape.sendActionBar(p, ChatColor.RED + "Next tnt in " + time + " " + (time == 1 ? "second" : "seconds"));
                  }
               } else {
                  toRemove.add(p);
               }
            }

            List<Location> locationsToRemove = new ArrayList<>();

            for (Location l : DisruptorKit.locations.keySet()) {
               int time = DisruptorKit.locations.get(l) - 1;
               if (time == 0) {
                  locationsToRemove.add(l);
               } else {
                  DisruptorKit.locations.put(l, time);
               }
            }

            for (Player px : toRemove) {
               DisruptorKit.times.remove(px);
            }

            for (Location lx : locationsToRemove) {
               DisruptorKit.locations.remove(lx);
            }
         }
      }, 20L, 20L);
      if (this.section != null) {
         double temp = this.section.getDouble("intensity");
         if (temp != 0.0) {
            this.intensity = temp;
         }

         temp = this.section.getDouble("intensity-2");
         if (temp != 0.0) {
            this.intensity2 = temp;
         }
      }
   }

   @Override
   public void giveItems(Player p) {
      super.giveItems(p);
      times.put(p, 0);
      leapUses.put(p, 3);
      p.setGameMode(GameMode.SURVIVAL);
   }

   @EventHandler
   @Override
   public void event(PlayerInteractEvent e) {
      if (KitManager.getKit(e.getPlayer()) == this && KitManager.getKit(e.getPlayer()) != null) {
         Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
         if (g != null && g.isGameStarted()) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
               if (e.getPlayer().getItemInHand() != null && e.getPlayer().getItemInHand().getType() == Material.IRON_AXE) {
                  if (KitManager.testCooldown(e.getPlayer())) {
                     int leaps = leapUses.get(e.getPlayer());
                     if (leaps == 0) {
                        e.getPlayer().sendMessage(Lang.getMessage("no-leaps-left"));
                     } else {
                        Player player = e.getPlayer();
                        KitManager.setCooldown(player, this.cooldown);
                        Vector vec = player.getLocation().getDirection();
                        vec.normalize();
                        vec.multiply(this.intensity);
                        vec.setY(vec.getY() + this.intensity2);
                        if (vec.getY() > 1.2) {
                           vec.setY(1.2);
                        }

                        player.setFallDistance(0.0F);
                        player.setVelocity(vec);
                        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
                        leapUses.put(player, leaps - 1);
                        player.sendMessage(
                           Lang.getMessage("leap-uses-left")
                              .replaceAll("%uses%", "" + leapUses.get(player))
                              .replaceAll("%use%", leapUses.get(player) == 1 ? "use" : "uses")
                        );
                     }
                  }
               }
            }
         }
      }
   }

   public static void reset(Game g) {
      if (locationsGames.containsKey(g) && locationsGames.get(g) != null) {
         for (Location l : locationsGames.get(g)) {
            l.getBlock().setType(Material.AIR);
            locations.remove(l);
         }
      }
   }

   public static void resetTimes(Game g) {
      for (Player p : g.getPlayers()) {
         if (KitManager.getKit(p).getKitName().equals("disruptor")) {
            if (p.getInventory().getItem(1) != null) {
               p.getInventory().getItem(1).setAmount(1);
            } else {
               p.getInventory().setItem(1, tnt);
            }

            if (times.containsKey(p)) {
               times.remove(p);
            }
         }
      }
   }

   public static void resetTime(Player p) {
      if (KitManager.getKit(p).getKitName().equals("disruptor")) {
         times.put(p, 0);
      }
   }

   public static void removeTime(Player p) {
      if (times.containsKey(p)) {
         times.remove(p);
      }
   }

   @EventHandler
   public void tntStvar(PlayerMoveEvent e) {
      Location l = new Location(e.getTo().getWorld(), (double)e.getTo().getBlockX(), (double)(e.getTo().getBlockY() - 1), (double)e.getTo().getBlockZ());
      if (locations.containsKey(l)) {
         l.getBlock().setType(Material.AIR);
         l.getWorld().createExplosion(l, 0.2F);
         locations.remove(l);
      }
   }

   @EventHandler
   public void place(final BlockPlaceEvent e) {
      if (KitManager.getKit(e.getPlayer()) == this && KitManager.getKit(e.getPlayer()) != null) {
         Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
         if (g != null && g.isGameStarted()) {
            g.getArena().addBlockState(e.getBlockReplacedState());
            if (!g.isSolo()) {
               Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
                  @Override
                  public void run() {
                     DisruptorKit.locations.put(e.getBlock().getLocation(), 58);
                  }
               }, 40L);
            } else {
               if (!locationsGames.containsKey(g)) {
                  locationsGames.put(g, new ArrayList<>());
               }

               locationsGames.get(g).add(e.getBlock().getLocation());
            }

            if (!times.containsKey(e.getPlayer())) {
               times.put(e.getPlayer(), 0);
            }
         }
      }
   }
}
