package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import me.radoje17.dragonescape.PracticeGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class BrewerKit extends Kit {
   private ArrayList<ItemStack> splashPotions;
   private ArrayList<ItemStack> potions;
   private HashMap<Player, Integer> times;

   private void addSplashPotions() {
      this.splashPotions = new ArrayList<>();
      Potion pot = new Potion(PotionType.SPEED);
      pot.setSplash(true);
      ItemStack speedPotion1 = new ItemStack(Material.POTION, 1);
      PotionMeta meta1 = (PotionMeta)speedPotion1.getItemMeta();
      meta1.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2), false);
      meta1.setDisplayName(ChatColor.WHITE + "Speed " + ChatColor.AQUA + "III" + ChatColor.YELLOW + " 10s");
      speedPotion1.setItemMeta(meta1);
      pot.apply(speedPotion1);
      this.splashPotions.add(speedPotion1);
      ItemStack speedPotion2 = new ItemStack(Material.POTION, 1);
      PotionMeta meta2 = (PotionMeta)speedPotion2.getItemMeta();
      meta2.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1), false);
      meta2.setDisplayName(ChatColor.WHITE + "Speed " + ChatColor.AQUA + "II" + ChatColor.YELLOW + " 20s");
      speedPotion2.setItemMeta(meta2);
      pot.apply(speedPotion2);
      this.splashPotions.add(speedPotion2);
      ItemStack speedPotion3 = new ItemStack(Material.POTION, 1);
      PotionMeta meta3 = (PotionMeta)speedPotion3.getItemMeta();
      meta3.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0), false);
      meta3.setDisplayName(ChatColor.WHITE + "Speed " + ChatColor.AQUA + "I" + ChatColor.YELLOW + " 30s");
      speedPotion3.setItemMeta(meta3);
      pot.apply(speedPotion3);
      this.splashPotions.add(speedPotion3);
      ItemStack slownessPotion = new ItemStack(Material.POTION, 1);
      PotionMeta meta4 = (PotionMeta)slownessPotion.getItemMeta();
      meta4.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 200, 1), false);
      meta4.setDisplayName(ChatColor.WHITE + "Slowness " + ChatColor.AQUA + "II" + ChatColor.YELLOW + " 10s");
      slownessPotion.setItemMeta(meta4);
      Potion pot1 = new Potion(PotionType.SLOWNESS);
      pot1.setSplash(true);
      pot1.apply(slownessPotion);
      this.splashPotions.add(slownessPotion);
      ItemStack jumpPotion = new ItemStack(Material.POTION, 1);
      PotionMeta meta5 = (PotionMeta)jumpPotion.getItemMeta();
      meta5.addCustomEffect(new PotionEffect(PotionEffectType.JUMP, 60, 2), false);
      meta5.setDisplayName(ChatColor.WHITE + "Jump " + ChatColor.AQUA + "III" + ChatColor.YELLOW + " 3s");
      jumpPotion.setItemMeta(meta5);
      Potion pot2 = new Potion(PotionType.JUMP);
      pot2.setSplash(true);
      pot2.apply(jumpPotion);
      this.splashPotions.add(jumpPotion);
   }

   private void addPotions() {
      this.potions = new ArrayList<>();
      Potion pot = new Potion(PotionType.SPEED);
      pot.setSplash(false);
      ItemStack speedPotion1 = new ItemStack(Material.POTION, 1);
      PotionMeta meta1 = (PotionMeta)speedPotion1.getItemMeta();
      meta1.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2), false);
      meta1.setDisplayName(ChatColor.WHITE + "Speed " + ChatColor.AQUA + "III" + ChatColor.YELLOW + " 10s");
      speedPotion1.setItemMeta(meta1);
      pot.apply(speedPotion1);
      this.potions.add(speedPotion1);
      ItemStack speedPotion2 = new ItemStack(Material.POTION, 1);
      PotionMeta meta2 = (PotionMeta)speedPotion2.getItemMeta();
      meta2.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1), false);
      meta2.setDisplayName(ChatColor.WHITE + "Speed " + ChatColor.AQUA + "II" + ChatColor.YELLOW + " 20s");
      speedPotion2.setItemMeta(meta2);
      pot.apply(speedPotion2);
      this.potions.add(speedPotion2);
      ItemStack speedPotion3 = new ItemStack(Material.POTION, 1);
      PotionMeta meta3 = (PotionMeta)speedPotion3.getItemMeta();
      meta3.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0), false);
      meta3.setDisplayName(ChatColor.WHITE + "Speed " + ChatColor.AQUA + "I" + ChatColor.YELLOW + " 30s");
      speedPotion3.setItemMeta(meta3);
      pot.apply(speedPotion3);
      this.potions.add(speedPotion3);
      ItemStack slownessPotion = new ItemStack(Material.POTION, 1);
      PotionMeta meta4 = (PotionMeta)slownessPotion.getItemMeta();
      meta4.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 200, 1), false);
      meta4.setDisplayName(ChatColor.WHITE + "Slowness " + ChatColor.AQUA + "II" + ChatColor.YELLOW + " 10s");
      slownessPotion.setItemMeta(meta4);
      Potion pot1 = new Potion(PotionType.SLOWNESS);
      pot1.setSplash(false);
      pot1.apply(slownessPotion);
      this.potions.add(slownessPotion);
      ItemStack jumpPotion = new ItemStack(Material.POTION, 1);
      PotionMeta meta5 = (PotionMeta)jumpPotion.getItemMeta();
      meta5.addCustomEffect(new PotionEffect(PotionEffectType.JUMP, 60, 2), false);
      meta5.setDisplayName(ChatColor.WHITE + "Jump " + ChatColor.AQUA + "III" + ChatColor.YELLOW + " 3s");
      jumpPotion.setItemMeta(meta5);
      Potion pot2 = new Potion(PotionType.JUMP);
      pot2.setSplash(false);
      pot2.apply(jumpPotion);
      this.potions.add(jumpPotion);
   }

   public BrewerKit() {
      super(KitType.Brewer, Material.POTION, Lang.getList("brewer-lore"));
      this.breaksBlocks = false;
      this.times = new HashMap<>();
      this.addSplashPotions();
      this.addPotions();
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            List<Player> toRemove = new ArrayList<>();

            for (Player p : BrewerKit.this.times.keySet()) {
               Game g = DragonEscape.getInstance().getGameManager().getGame(p);
               if (g != null && g.isGameStarted() && g.isAlive(p)) {
                  int newTime = BrewerKit.this.times.get(p) - 1;
                  if (newTime == 0) {
                     if (p.getInventory().getItem(0).getType() == Material.GLASS_BOTTLE) {
                        p.getInventory().setItem(0, BrewerKit.this.getRandomPotion(g));
                     } else if (p.getInventory().getItem(1).getType() == Material.GLASS_BOTTLE) {
                        p.getInventory().setItem(1, BrewerKit.this.getRandomPotion(g));
                     }

                     newTime = 15;
                  }

                  if (p.getInventory().getItem(0).getType() == Material.POTION && p.getInventory().getItem(1).getType() == Material.POTION) {
                     newTime = 15;
                  }

                  BrewerKit.this.times.put(p, newTime);
               } else {
                  toRemove.add(p);
               }
            }

            for (Player px : toRemove) {
               BrewerKit.this.times.remove(px);
            }
         }
      }, 20L, 20L);
   }

   private ItemStack getRandomPotion(Game g) {
      return !g.isSolo() && !(g instanceof PracticeGame)
         ? this.splashPotions.get(new Random().nextInt(this.potions.size()))
         : this.potions.get(new Random().nextInt(this.potions.size()));
   }

   @Override
   public void giveItems(Player p) {
      Game g = DragonEscape.getInstance().getGameManager().getGame(p);
      if (g != null) {
         int start = p.getInventory().getItem(0) != null ? 1 : 0;
         p.getInventory().setItem(start, this.getRandomPotion(g));
         p.getInventory().setItem(start + 1, this.getRandomPotion(g));
         this.times.put(p, 15);
      }
   }

   @EventHandler
   public void rightClick(PlayerInteractEvent e) {
      if (e.getItem() != null) {
         Kit k = KitManager.getKit(e.getPlayer());
         if (k == this) {
            Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
            if (g != null && g.isGameStarted() && (g.isSolo() || g instanceof PracticeGame)) {
               if (e.getItem().getItemMeta() instanceof PotionMeta) {
                  PotionMeta meta = (PotionMeta)e.getItem().getItemMeta();

                  for (PotionEffect effect : meta.getCustomEffects()) {
                     e.getPlayer().addPotionEffect(effect);
                  }

                  e.getPlayer().setItemInHand(new ItemStack(Material.GLASS_BOTTLE, 1));
               }
            }
         }
      }
   }

   @EventHandler
   public void potionThrow(final PlayerInteractEvent e) {
      if (e.getItem() != null) {
         Kit k = KitManager.getKit(e.getPlayer());
         if (k == this) {
            Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
            if (g != null) {
               if (!g.isGameStarted()) {
                  e.setCancelled(true);
                  e.getPlayer().updateInventory();
               } else {
                  if (e.getPlayer().getItemInHand() != null && e.getPlayer().getItemInHand().getType() == Material.POTION) {
                     Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                           e.getPlayer().setItemInHand(new ItemStack(Material.GLASS_BOTTLE, 1));
                           e.getPlayer().updateInventory();
                        }
                     }, 1L);
                  }
               }
            }
         }
      }
   }

   @Override
   public void event(PlayerInteractEvent e) {
   }
}
