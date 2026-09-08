package me.radoje17.dragonescape;

import com.sk89q.worldedit.WorldEditException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import me.radoje17.cubics.Cubics;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.InventoryUtils;
import me.radoje17.dragonescape.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class GameManager implements Listener {
   private List<Game> games;
   private List<Player> restartCooldown;
   private HashMap<Player, DeSettings> settigns;
   private HashMap<Player, Game> players;
   private HashMap<Player, ItemStack[]> inventories;
   private List<Player> noDamage;
   private HashMap<Integer, Game> gamesInProgress;
   private boolean maintenance = false;
   private List<Player> liquids;
   private static HashMap<String, HashMap<String, ArenaStats>> stats;
   int gameIDCounter = 0;
   int minHeight;

   public void addInventory(Player p) {
      if (!this.inventories.containsKey(p)) {
         this.inventories.put(p, p.getInventory().getContents());
      }
   }

   public void restoreInventory(Player p) {
      if (this.inventories.containsKey(p)) {
         p.getInventory().clear();
         p.getInventory().setContents(this.inventories.remove(p));
         p.updateInventory();
      }
   }

   public GameManager() {
      this.games = new ArrayList<>();
      this.restartCooldown = new ArrayList<>();
      this.noDamage = new ArrayList<>();
      this.gamesInProgress = new HashMap<>();
      this.settigns = new HashMap<>();
      this.players = new HashMap<>();
      this.inventories = new HashMap<>();
      this.liquids = new ArrayList<>();
      this.minHeight = DragonEscape.getConfiguration().getInt("min-height");
      stats = new HashMap<>();
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            boolean updateInventories = false;

            for (Game g : new ArrayList<>(GameManager.this.games)) {
               if (g.isTimerStarted()) {
                  g.timerTick();
                  updateInventories = true;
               }
            }

            if (updateInventories) {
               InventoryUtils.updateInventories();
            }

            GameManager.this.restartCooldown.clear();
         }
      }, 20L, 20L);
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            for (Player p : new ArrayList<>(GameManager.this.liquids)) {
               Game g = GameManager.this.getGame(p);
               if (g == null) {
                  GameManager.this.liquids.remove(p);
               } else if (g.getArena() == null || g.getArena().isWaterKills()) {
                  if (p.getHealth() - 1.0 <= 0.0 && (g = GameManager.this.getGame(p)) != null) {
                     g.die(p);
                  } else {
                     p.setHealth(p.getHealth() - 1.0);
                  }
               }
            }
         }
      }, 5L, 5L);
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            for (int i : new ArrayList<>(GameManager.this.gamesInProgress.keySet())) {
               if (!GameManager.this.gamesInProgress.containsKey(i)) continue;
               if (GameManager.this.gamesInProgress.get(i).getDragon() != null) {
                  GameManager.this.gamesInProgress.get(i).getDragon().tick();
                  Game current = GameManager.this.gamesInProgress.get(i);
                  if (current != null) current.updateScoreboard();
               }
            }
         }
      }, 1L, 1L);
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            for (int i : new ArrayList<>(GameManager.this.gamesInProgress.keySet())) {
               if (!GameManager.this.gamesInProgress.containsKey(i)) continue;
               GameManager.this.gamesInProgress.get(i).titleTimerTick();
            }
         }
      }, 2L, 2L);
   }

   public static ArenaStats getArenaStats(String player, String arenaName) throws SQLException {
      player = player.toLowerCase();
      if (!ArenaUtils.isArena(arenaName)) {
         return null;
      } else {
         if (stats.get(player) == null) {
            stats.put(player, new HashMap<>());
         }

         ArenaStats as = stats.get(player).get(arenaName.toLowerCase());
         if (as == null) {
            as = new ArenaStats(player, arenaName);
            stats.get(player).put(arenaName.toLowerCase(), as);
         }

         return as;
      }
   }

   public Game getGame(String arenaName) throws WorldEditException, IOException {
      for (Game game : this.games) {
         if (game.isAcceptingPlayers() && game.getArenaName().equalsIgnoreCase(arenaName)) {
            return game;
         }
      }

      Game g = new Game(arenaName);
      this.games.add(g);
      return g;
   }

   public DeSettings updateSettings(Player p) throws SQLException {
      if (this.settigns.containsKey(p)) {
         this.settigns.get(p).update();
      } else {
         this.settigns.put(p, new DeSettings(p));
      }

      return this.settigns.get(p);
   }

   public DeSettings getDeSettings(Player p) throws SQLException {
      return this.settigns.containsKey(p) ? this.settigns.get(p) : this.updateSettings(p);
   }

   public void deleteSettings(Player p) {
      this.settigns.remove(p);
   }

   public Game getSoloGame(String arenaName, Player p) throws WorldEditException, IOException {
      return new Game(arenaName, p);
   }

   public Game createGame(String arenaName) throws WorldEditException, IOException {
      Game g = new Game(arenaName);
      this.games.add(g);
      return g;
   }

   public Game createGame(List<Player> players) {
      Game g = new Game(players);
      this.games.add(g);
      InventoryUtils.updateInventories();
      return g;
   }

   public Game getGame(Player p) {
      return !this.players.containsKey(p) ? null : this.players.get(p);
   }

   public void addPlayer(Player p, Game g) {
      this.players.put(p, g);
   }

   public void removeGameFromLists(Game g) {
      this.games.remove(g);
      this.gamesInProgress.remove(g.gameID);
   }

   public void createGame(Game oldGame) {
      for (Player p : oldGame.getPlayers()) {
         p.getInventory().clear();
      }

      this.createGame(oldGame.getPlayers());
      this.games.remove(oldGame);
      InventoryUtils.updateInventories();
   }

   public void removeGame(Game g) {
      for (Player p : g.getPlayers()) {
         this.players.remove(p);
         p.setGameMode(GameMode.SPECTATOR);
         p.setGameMode(GameMode.ADVENTURE);
         DragonEscape.getInstance().getGameManager().restoreInventory(p);
         p.teleport(DragonEscape.getGlobalLobby());

         for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!p.canSee(pl)) {
               p.showPlayer(pl);
            }
         }
      }

      this.games.remove(g);
      InventoryUtils.updateInventories();
   }

   @EventHandler
   public void startTimerOnBlockPlace(BlockPlaceEvent e) {
      Game g;
      if ((g = this.getGame(e.getPlayer())) != null) {
         if (!g.isGameStarted()) {
            if (this.players.containsKey(e.getPlayer())) {
               if (e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                  if (g.isSolo()) {
                     g.startTimer();
                  }
               }
            }
         }
      }
   }

   public int getPlayerCount() {
      return this.players.size();
   }

   @EventHandler
   public void move(PlayerMoveEvent e) {
      if (this.getGame(e.getPlayer()) != null) {
         if (this.players.containsKey(e.getPlayer())) {
            if (e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
               Game g = this.players.get(e.getPlayer());
               if (g.isFrozen()) {
                  Location l = e.getTo();
                  l.setX(e.getFrom().getX());
                  l.setZ(e.getFrom().getZ());
                  e.setTo(l);
               } else if (e.getPlayer().getLocation().getY() <= (double)this.minHeight) {
                  g.die(e.getPlayer());
               } else {
                  Block b = e.getPlayer().getLocation().getBlock();
                  boolean liquidsContains = this.liquids.contains(e.getPlayer());
                  if (b.getType() != Material.LAVA
                     && b.getType() != Material.STATIONARY_LAVA
                     && b.getType() != Material.WATER
                     && b.getType() != Material.STATIONARY_WATER) {
                     if (liquidsContains) {
                        this.liquids.remove(e.getPlayer());
                        e.getPlayer().setFireTicks(0);
                     }
                  } else if (!liquidsContains) {
                     this.liquids.add(e.getPlayer());
                  }

                  if ((g.isSolo() || g instanceof PracticeGame)
                     && !g.isGameStarted()
                     && !g.isTimerStarted()
                     && (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ())) {
                     g.startTimer();
                  }

                  if (e.getPlayer().getLocation().getY() <= 0.0) {
                     g.die(e.getPlayer());
                  } else {
                     Location l = e.getPlayer().getLocation();
                     if (l.getBlock().getType() == Material.PORTAL) {
                        g.finish(e.getPlayer());
                     }

                     l.setY(l.getY() - 1.0);
                     if (l.getBlock().getType() == Material.BARRIER) {
                        g.die(e.getPlayer());
                     }

                     if (l.getBlock().getType() == Material.BEACON) {
                        g.finish(e.getPlayer());
                     }

                     l.setY(l.getY() - 1.0);
                     if (l.getBlock().getType() == Material.BEACON) {
                        g.finish(e.getPlayer());
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void interact(PlayerInteractEvent e) {
      if (e.getPlayer().getItemInHand() != null) {
         Game g;
         if ((g = this.getGame(e.getPlayer())) != null) {
            if (e.getPlayer().getItemInHand().getType() == Material.MAGMA_CREAM) {
               try {
                  if (DragonEscape.getInstance().getGameManager().getDeSettings(e.getPlayer()).safetyMode() && !e.getPlayer().isSneaking()) {
                     e.getPlayer().sendMessage(Lang.getMessage("safety-mode-enabled"));
                     return;
                  }
               } catch (SQLException var5) {
                  var5.printStackTrace();
               }

               g.removePlayer(e.getPlayer());
            } else if (!this.restartCooldown.contains(e.getPlayer())
               && g.isSolo()
               && g.isGameStarted()
               && e.getPlayer().getItemInHand().getType() == Material.SUGAR) {
               g.restart(false);
               this.restartCooldown.add(e.getPlayer());
            } else if (e.getPlayer().getItemInHand() != null
               && e.getPlayer().getItemInHand().hasItemMeta()
               && e.getPlayer().getItemInHand().getItemMeta().hasDisplayName()
               && e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals(Lang.getMessage("settings-book"))) {
               try {
                  e.getPlayer().openInventory(InventoryUtils.getSettingsInventory(e.getPlayer()));
               } catch (SQLException var4) {
                  e.getPlayer().sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var4.getLocalizedMessage()));
               }
            }
         }
      }
   }

   public HashMap<Player, Game> getPlayers() {
      return this.players;
   }

   public void createPublicLobby() {
      for (Game g : this.games) {
         if (!g.isGameStarted() && !g.isSolo() && g.getArenaName().equals("none")) {
            return;
         }
      }

      this.createGame(new ArrayList<>());
   }

   public Game getAvailableGame(int slots) {
      for (Game g : this.games) {
         if (!g.gameStarted && g.slots() - g.players.size() - slots >= 0) {
            return g;
         }
      }

      return null;
   }

   @EventHandler
   public void command(PlayerCommandPreprocessEvent e) {
      if (e.getMessage().toLowerCase().startsWith("/join")
         || e.getMessage().toLowerCase().startsWith("/leave")
         || e.getMessage().toLowerCase().startsWith("/browse")
         || e.getMessage().toLowerCase().startsWith("/solo")
         || e.getMessage().toLowerCase().startsWith("/public")
         || e.getMessage().toLowerCase().startsWith("/settings")
         || e.getMessage().toLowerCase().startsWith("/leaderboard")
         || e.getMessage().toLowerCase().startsWith("/stats")
         || e.getMessage().toLowerCase().startsWith("/practice")
         || e.getMessage().toLowerCase().startsWith("/spectate")
         || e.getMessage().toLowerCase().startsWith("/random")
         || e.getMessage().toLowerCase().startsWith("/lb")
         || e.getMessage().toLowerCase().startsWith("/mapstats")) {
         e.setCancelled(true);
         if (e.getMessage().toLowerCase().startsWith("/join")) {
            e.getPlayer().chat("/de solo" + e.getMessage().substring(5));
         } else {
            e.getPlayer().chat("/de " + e.getMessage().substring(1));
         }
      } else if (e.getMessage().equalsIgnoreCase("/leave") || e.getMessage().equalsIgnoreCase("/lobby")) {
         e.setCancelled(true);
         e.getPlayer().chat("/de leave");
      } else if (e.getMessage().equalsIgnoreCase("/spectate")) {
         e.setCancelled(true);
         e.getPlayer().chat("/de spectate");
      } else if (!e.getMessage().startsWith("/de leaderboard") && !e.getMessage().startsWith("/dragonescape leaderboard")) {
         if (this.players.containsKey(e.getPlayer())
            && !e.getMessage().equalsIgnoreCase("/de leave")
            && !e.getMessage().equalsIgnoreCase("/de spectate")
            && !e.getMessage().toLowerCase().startsWith("/lb")
            && !e.getMessage().toLowerCase().startsWith("/de lb")) {
            e.setCancelled(true);
            if (e.getMessage().equalsIgnoreCase("/leave")) {
               Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
               if (g == null) {
                  e.getPlayer().sendMessage(Lang.getMessage("not-in-game"));
               } else {
                  g.removePlayer(e.getPlayer());

                  try {
                     if (DragonEscape.getInstance().getGameManager().getDeSettings(e.getPlayer()).sendJoinMessages()) {
                        e.getPlayer().sendMessage(Lang.getMessage("you-left"));
                     }
                  } catch (SQLException var4) {
                     var4.printStackTrace();
                     e.getPlayer().sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var4.getLocalizedMessage()));
                  }
               }
            } else {
               e.getPlayer().sendMessage(Lang.getMessage("command-in-game"));
            }
         }
      }
   }

   @EventHandler
   public void quit(PlayerQuitEvent e) {
      if (this.players.containsKey(e.getPlayer())) {
         this.players.get(e.getPlayer()).removePlayer(e.getPlayer());
      }

      this.deleteSettings(e.getPlayer());
      this.inventories.remove(e.getPlayer());
      this.noDamage.remove(e.getPlayer());
      this.restartCooldown.remove(e.getPlayer());
      this.liquids.remove(e.getPlayer());
      stats.remove(e.getPlayer().getName().toLowerCase());
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void damage(final EntityDamageEvent e) {
      e.setCancelled(true);
      if (e.getEntity() instanceof Player) {
         if (!this.noDamage.contains(e.getEntity())) {
            Game g = this.getGame((Player)e.getEntity());
            if (g != null) {
               if (e.getCause() != DamageCause.LAVA && e.getCause() != DamageCause.FIRE_TICK && e.getCause() != DamageCause.FIRE) {
                  e.getEntity().setFireTicks(0);
                  double newHealth = ((Player)e.getEntity()).getHealth() - e.getDamage();
                  if (newHealth > 20.0) {
                     newHealth = 20.0;
                  }

                  if (newHealth <= 0.0) {
                     g.die((Player)e.getEntity());
                  } else {
                     final double finalNewHealth = newHealth;
                     Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                           ((Player)e.getEntity()).setHealth(finalNewHealth);
                        }
                     }, 2L);
                  }
               } else {
                  e.setDamage(0.0);
               }
            }
         }
      }
   }

   @EventHandler
   public void death(final PlayerDeathEvent e) {
      e.setDeathMessage("");
      final Game g;
      if ((g = this.getGame(e.getEntity())) != null) {
         Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
            @Override
            public void run() {
               g.die(e.getEntity());
               e.getEntity().spigot().respawn();
            }
         }, 1L);
      }
   }

   public boolean hasGame(Player p) {
      return this.players.containsKey(p);
   }

   @EventHandler
   public void respawn(PlayerRespawnEvent e) {
      Game g;
      if ((g = this.getGame(e.getPlayer())) != null) {
         Player alive = g.getFirstAlivePlayer();
         if (alive != null) {
            e.setRespawnLocation(alive.getLocation());
         }
      }
   }

   @EventHandler
   public void foodChange(FoodLevelChangeEvent e) {
      e.setCancelled(true);
   }

   @EventHandler
   public void drop(PlayerDropItemEvent e) {
      if (this.getGame(e.getPlayer()) != null) {
         e.setCancelled(true);
      }
   }

   public boolean canSwitchToMaintenance() {
      return this.games.size() == 0;
   }

   public boolean reverseMaintenance() {
      this.maintenance = !this.maintenance;
      return this.maintenance;
   }

   public boolean isMaintenance() {
      return this.maintenance;
   }

   public boolean isMaintenanceAndNoActiveGames() {
      return this.maintenance && this.games.size() == 0;
   }

   public List<Game> getSortedGames() {
      List<Game> games = new ArrayList<>(this.games);
      games.sort(new Comparator<Game>() {
         public int compare(Game g1, Game g2) {
            if (g1.isTimerStarted() && !g2.isTimerStarted()) {
               return 1;
            } else if (!g1.isTimerStarted() && g2.isTimerStarted()) {
               return -1;
            } else {
               int g1Players = g1.getPlayers().size();
               int g2Players = g2.getPlayers().size();
               if (g1Players == g2Players) {
                  return 0;
               } else {
                  return g1Players > g2Players ? 1 : -1;
               }
            }
         }
      });
      return games;
   }

   public void removePlayer(Player p) {
      if (this.players.containsKey(p)) {
         this.players.remove(p);
      }
   }

   public int getNextGameID() {
      this.gameIDCounter++;
      return this.gameIDCounter;
   }

   public void addGameInProgress(int id, Game g) {
      this.gamesInProgress.put(id, g);
   }

   public void removeGameInProgress(int id) {
      this.gamesInProgress.remove(id);
   }

   public Game getGameByID(int id) {
      return this.gamesInProgress.get(id);
   }

   public void makeNotInProgress(Game g) {
      this.gamesInProgress.remove(g.gameID);
   }

   @EventHandler
   public void spawn(EntitySpawnEvent e) {
      if ((e.getEntity().getType() == EntityType.DROPPED_ITEM || e.getEntity().getType() == EntityType.PRIMED_TNT)
         && e.getEntity().getLocation().getWorld().getName().equals("dragonescape")) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void breakBlock(BlockBreakEvent e) {
      if (e.getPlayer().getWorld().getName().equals("dragonescape")) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void join(PlayerJoinEvent e) {
      for (Player p : this.players.keySet()) {
         p.hidePlayer(e.getPlayer());
      }

      e.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
   }

   public void addToNoDamage(Player p) {
      if (!this.noDamage.contains(p)) this.noDamage.add(p);
   }

   public void removeFromNoDamage(final Player p) {
      if (this.noDamage.contains(p)) {
         Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
            @Override
            public void run() {
               GameManager.this.noDamage.remove(p);
            }
         }, 5L);
      }
   }

   @EventHandler
   public void chunkUnload(ChunkUnloadEvent e) {
      // Let Bukkit unload inactive chunks, including the permanent solo-map world.
   }

   public void removeLiquidDamage(Player p) {
      this.liquids.remove(p);
      p.setFireTicks(0);
   }

   @EventHandler
   public void practiceSpawnpoints(PlayerInteractEvent e) {
      Game g = this.getGame(e.getPlayer());
      if (g != null && g instanceof PracticeGame) {
         if (e.getItem() != null && e.getItem().getType() == Material.INK_SACK) {
            if (e.getItem().getData().getData() == 10) {
               ((PracticeGame)g).addSpawnPoint();
               e.getPlayer().sendMessage(Lang.getMessage("spawnpoint-added"));
            } else if (e.getItem().getData().getData() == 1) {
               ((PracticeGame)g).removeSpawnPoint();
               e.getPlayer().sendMessage(Lang.getMessage("spawnpoint-removed"));
            }
         }
      }
   }

   @EventHandler
   public void practiceFlight(PlayerInteractEvent e) {
      Game g = this.getGame(e.getPlayer());
      if (g != null && g instanceof PracticeGame) {
         if (e.getItem() != null && e.getItem().getType() == Material.FEATHER) {
            if (e.getPlayer().getAllowFlight()) {
               e.getPlayer().setAllowFlight(false);
               e.getPlayer().setFlying(false);
               e.getPlayer().sendMessage(Lang.getMessage("flight-disabled"));
            } else {
               e.getPlayer().setAllowFlight(true);
               e.getPlayer().sendMessage(Lang.getMessage("flight-enabled"));
            }
         }
      }
   }

   @EventHandler
   public void blockPhysics(BlockPhysicsEvent e) {
      if (e.getBlock().getLocation().getWorld() == ArenaUtils.getWorld()) {
         e.setCancelled(true);
      }
   }

   public static void clearScoreboard(Player p) {
      if (p != null && p.isOnline()) {
         p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
      }
   }

   public static void showScoreboard(Player p, Game g) throws SQLException {
      ScoreboardManager manager = Bukkit.getScoreboardManager();
      List<String> scoreboard;
      if (g instanceof PracticeGame) {
         scoreboard = Lang.getList("practice-scoreboard");
      } else {
         scoreboard = Lang.getList("solo-scoreboard");
      }

      String worldRecorder = "none";
      String wr = TimeUtils.formatTime(0L);
      String kit = KitManager.getKit(p) != null ? KitManager.getKit(p).getKitName().toLowerCase() : "None";

      try {
         ResultSet set = MySQLConnector.prepareStatement(
               "select uuid, time from de_times_solo WHERE disabled = 0 AND kit='" + kit + "' AND arenaName='" + g.getArenaName() + "' ORDER BY time LIMIT 1;"
            )
            .executeQuery();
         if (set.next()) {
            worldRecorder = Bukkit.getOfflinePlayer(UUID.fromString(set.getString(1))).getName();
            wr = TimeUtils.formatTime(set.getLong(2));
         }
      } catch (SQLException var15) {
         var15.printStackTrace();
      }

      String pb = TimeUtils.formatTime(MySQLConnector.getTime(p.getUniqueId().toString(), g.getArenaName(), kit, true));
      int parkourXP = 0;
      Scoreboard board = null;
      board = manager.getNewScoreboard();
      Objective objective = board.registerNewObjective("super", "jump");
      objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      String title = scoreboard.get(0);
      objective.setDisplayName(title);
      Objective o = board.getObjective(DisplaySlot.SIDEBAR);

      for (int i = 1; i < scoreboard.size(); i++) {
         Score s = o.getScore(
            scoreboard.get(i)
               .replaceAll("%pb%", pb)
               .replaceAll("%wr%", wr)
               .replaceAll("%wr_holder%", worldRecorder)
               .replaceAll("%map%", g.getArenaName())
               .replaceAll("%kit%", kit)
               .replaceAll("%parkour_xp%", "" + Cubics.getInstance().getCubicsAPI().getparkourXp(p))
         );
         s.setScore(scoreboard.size() - i + 1);
      }

      p.setScoreboard(board);
   }

   @EventHandler
   public void weatherChange(WeatherChangeEvent e) {
      e.setCancelled(true);
   }
}
