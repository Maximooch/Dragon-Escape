package me.radoje17.dragonescape;

import java.sql.SQLException;
import java.util.Stack;
import me.radoje17.dragonescape.kits.Kit;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.InventoryUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

public class PracticeGame extends Game {
   private Player p;
   private Kit k;
   private Stack<Location> spawnPoints;

   public PracticeGame(Player p, Kit kit, String arenaName) {
      this.p = p;
      KitManager.setKit(p, kit);
      this.arenaName = arenaName;
      this.gameID = DragonEscape.getInstance().getGameManager().getNextGameID();
      if (!ArenaUtils.hasSpawnPoint(arenaName)) {
         DragonEscape.getInstance().getGameManager().removeGame(this);
      } else {
         this.spawnPoints = new Stack<>();
         this.spawnPoints.push(ArenaUtils.getSpawnPoint(arenaName));
         this.arena = new Arena(this.spawnPoints.peek());
         DragonEscape.getInstance().getGameManager().addPlayer(p, this);
         this.solo = true;
         this.players.add(p);
         this.hideAllPlayers(p);
         this.showSolo();

         for (Player pl : DragonEscape.getInstance().getGameManager().getPlayers().keySet()) {
            if (pl != this.players.get(0)) {
               Game g = DragonEscape.getInstance().getGameManager().getGame(pl);
               if (g != null && g.getArena() != null && g.getArenaName().equals(this.getArenaName()) && g.getArena().COORDS == this.arena.COORDS) {
                  g.showSolo(this.players.get(0));
               }
            }
         }

         this.startGame();
      }
   }

   @Override
   public void startGame() {
      this.timerStarted = false;
      this.gameStarted = true;
      DragonEscape.getInstance().getGameManager().addInventory(this.p);
      this.p.getInventory().clear();
      this.k = KitManager.getKit(this.p);
      if (this.k != null) {
         this.k.giveItems(this.p);
      }

      this.p.getInventory().setItem(7, InventoryUtils.getSettingsBook());
      ItemStack removeSpawnPoint = new ItemStack(Material.INK_SACK, 1, (short)1);
      ItemMeta removeMeta = removeSpawnPoint.getItemMeta();
      removeMeta.setDisplayName(Lang.getMessage("remove-spawnpoint"));
      removeSpawnPoint.setItemMeta(removeMeta);
      this.p.getInventory().setItem(6, InventoryUtils.getQuitItem());
      this.p.getInventory().setItem(8, InventoryUtils.getRestartItem());
      this.p.getInventory().setItem(5, removeSpawnPoint);
      ItemStack addSpawnPoint = new ItemStack(Material.INK_SACK, 1, (short)10);
      ItemMeta addMeta = addSpawnPoint.getItemMeta();
      addMeta.setDisplayName(Lang.getMessage("add-spawnpoint"));
      addSpawnPoint.setItemMeta(addMeta);
      this.p.getInventory().setItem(4, addSpawnPoint);
      ItemStack fly = new ItemStack(Material.FEATHER, 1);
      ItemMeta flyMeta = fly.getItemMeta();
      flyMeta.setDisplayName(Lang.getMessage("fly-item-name"));
      fly.setItemMeta(flyMeta);
      this.p.getInventory().setItem(3, fly);
      this.p.teleport(this.getSpawnPoint());

      try {
         GameManager.showScoreboard(this.p, this);
      } catch (SQLException var8) {
         this.p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var8.getLocalizedMessage()));
      }

      DragonEscape.getInstance().getGameManager().addGameInProgress(this.gameID, this);
   }

   public Location getSpawnPoint() {
      return this.spawnPoints.peek();
   }

   public void addSpawnPoint() {
      this.spawnPoints.add(this.p.getLocation());
   }

   public void removeSpawnPoint() {
      if (this.spawnPoints.size() != 1) {
         this.spawnPoints.pop();
      }
   }

   @Override
   public void restart(boolean death) {
      this.die(this.p);
   }

   @Override
   public void finish(Player p) {
      this.die(p);
   }

   @Override
   public void die(Player p) {
      p.teleport(this.getSpawnPoint());
      p.setHealth(20.0);
      p.setFireTicks(0);

      for (PotionEffect effect : p.getActivePotionEffects()) {
         p.removePotionEffect(effect.getType());
      }

      p.getInventory().setItem(0, new ItemStack(Material.AIR));
      p.getInventory().setItem(1, new ItemStack(Material.AIR));
      p.getInventory().setItem(2, new ItemStack(Material.AIR));
      if (this.k != null) {
         this.k.giveItems(p);
      }

      KitManager.removeCooldown(p);
      this.timerTitle = 0.0;
      this.gameStarted = false;
   }
}
