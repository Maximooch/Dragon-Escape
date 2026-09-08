package me.radoje17.dragonescape;

import com.sk89q.worldedit.WorldEditException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import me.radoje17.cubics.Cubics;
import me.radoje17.dragonescape.dragon.Dragon;
import me.radoje17.dragonescape.kits.DisruptorKit;
import me.radoje17.dragonescape.kits.Kit;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.InventoryUtils;
import me.radoje17.dragonescape.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class Game {
   protected String arenaName;
   private List<Location> dragonPoints = new ArrayList<>();
   private Iterator<Location> dragonPointsIterator;
   private Iterator<Location> spawnPointsIterator = null;
   protected Arena arena;
   protected List<Player> players = new ArrayList<>();
   private HashMap<Player, Long> times = new LinkedHashMap<>();
   private HashMap<Player, String> timesString = new HashMap<>();
   private int timer = 31;
   private long startTime = -1L;
   protected boolean timerStarted = false;
   protected boolean gameStarted = false;
   protected boolean solo = false;
   private Dragon dragon;
   int gameID;
   private Location spawnpoint = null;
   private HashMap<Integer, Integer> distances = new HashMap<>();
   private HashMap<Player, Integer> currentDragonpoints = new HashMap<>();
   private HashMap<Player, Integer> currentDistance = new HashMap<>();
   private HashMap<Player, Integer> votes = new HashMap<>();
   private HashMap<Player, Integer> mapVotes = new HashMap<>();
   private List<Player> toSpectate = new ArrayList<>();
   protected double timerTitle = 0.0;
   protected boolean frozen = false;
   private int soloDeaths = 0;
   private double dragonSpeed;
   private String[] arenas;
   private Scoreboard scoreboard;
   private Objective objective;
   private int mapLength = 0;
   private int taskID = -1;
   private Team team;
   private boolean active = false;
   private boolean dontChangeTimer = false;
   private boolean votingEnabled = true;
   private boolean ending = false;
   private int scoreboardTicks;

   public void openVotingInventory() {
      boolean includeMaps = this.arenaName.equals("none");

      for (Player p : this.players) {
         if (includeMaps) {
            p.openInventory(InventoryUtils.mapsVoteInventory(p, this));
         } else {
            p.openInventory(InventoryUtils.voteInventory(p, this));
         }
      }
   }

   public void openVotingInventory(Player p) {
      if (!this.solo) {
         if (this.arenaName.equals("none")) {
            p.openInventory(InventoryUtils.mapsVoteInventory(p, this));
         } else {
            p.openInventory(InventoryUtils.voteInventory(p, this));
         }
      }
   }

   public HashMap<Player, Integer> getVotes() {
      return this.votes;
   }

   public HashMap<Player, Integer> getMapVotes() {
      return this.mapVotes;
   }

   public Game() {
   }

   public Game(String arenaName) throws WorldEditException, IOException {
      this.arenaName = arenaName;
      this.gameID = DragonEscape.getInstance().getGameManager().getNextGameID();
      this.arena = ArenaUtils.getArena(arenaName, false);
      ArenaUtils.makeUnavailable(this.arena);
      this.dragonPoints = this.arena.getDragonPoints();
      this.dragonPointsIterator = this.arena.getDragonPoints().iterator();
      if (this.dragonPoints.size() >= 1) {
         Location l = this.dragonPoints.get(this.dragonPoints.size() - 1);
         int currDistance = 0;
         this.distances.put(this.dragonPoints.size() - 1, currDistance);

         for (int i = this.dragonPoints.size() - 2; i >= 0; i--) {
            currDistance = (int)((double)currDistance + this.dragonPoints.get(i).distance(l));
            this.distances.put(i, currDistance);
            l = this.dragonPoints.get(i);
         }

         this.mapLength = currDistance;
      }

      ScoreboardManager manager = Bukkit.getScoreboardManager();
      this.scoreboard = manager.getNewScoreboard();
      this.team = this.scoreboard.registerNewTeam("Ghosts");
      this.team.setCanSeeFriendlyInvisibles(true);
      this.objective = this.scoreboard.registerNewObjective("dragon", "escape");
      this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      this.objective.setDisplayName(ChatColor.RED + "DragonEscape");
   }

   public Game(List<Player> players) {
      this.arenaName = "none";
      this.arenas = ArenaUtils.getRandomArenas();
      this.gameID = DragonEscape.getInstance().getGameManager().getNextGameID();
      if (this.dragonPoints.size() >= 1) {
         Location l = this.dragonPoints.get(this.dragonPoints.size() - 1);
         int currDistance = 0;
         this.distances.put(this.dragonPoints.size() - 1, currDistance);

         for (int i = this.dragonPoints.size() - 2; i >= 0; i--) {
            currDistance = (int)((double)currDistance + this.dragonPoints.get(i).distance(l));
            this.distances.put(i, currDistance);
            l = this.dragonPoints.get(i);
         }

         this.mapLength = currDistance;
      }

      ScoreboardManager manager = Bukkit.getScoreboardManager();
      this.scoreboard = manager.getNewScoreboard();
      this.team = this.scoreboard.registerNewTeam("Ghosts");
      this.team.setCanSeeFriendlyInvisibles(true);
      this.objective = this.scoreboard.registerNewObjective("dragon", "escape");
      this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
      this.objective.setDisplayName(ChatColor.RED + "DragonEscape");

      for (Player p : players) {
         this.addPlayer(p);
      }

      InventoryUtils.updateInventories();
   }

   public Game(String arenaName, Player p) throws WorldEditException, IOException {
      this.arenaName = arenaName;
      this.gameID = DragonEscape.getInstance().getGameManager().getNextGameID();
      Location l = ArenaUtils.getSpawnPoint(arenaName);
      if (l != null && !KitManager.getKit(p).isBreaksBlocks()) {
         this.arena = new Arena(l, ArenaUtils.getArenaTime(arenaName));
      } else {
         this.arena = ArenaUtils.getArena(arenaName, true);
      }

      if (this.arena.COORDS != -1) {
         ArenaUtils.makeUnavailable(this.arena);
      }

      p.setHealth(20.0);
      p.setFoodLevel(20);
      p.setGameMode(GameMode.ADVENTURE);
      DragonEscape.getInstance().getGameManager().removeFromNoDamage(p);
      DragonEscape.getInstance().getGameManager().addInventory(p);
      p.getInventory().clear();
      DragonEscape.getInstance().getGameManager().addPlayer(p, this);
      this.players.add(p);
      this.solo = true;
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

      if (this.arena.isReady()) {
         this.startGame();
      } else {
         p.sendMessage(Lang.getMessage("arena-loading").replaceAll("%arena%", arenaName));
         p.setGameMode(GameMode.ADVENTURE);
         ItemStack kitSelector = new ItemStack(Material.STICK, 1);
         ItemMeta kitSelectorMeta = kitSelector.getItemMeta();
         kitSelectorMeta.setDisplayName(ChatColor.WHITE + "Kit Selector");
         kitSelector.setItemMeta(kitSelectorMeta);
         p.getInventory().setItem(8, InventoryUtils.getQuitItem());
         p.teleport(DragonEscape.getLobby());
         this.taskID = Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
            @Override
            public void run() {
               if (Game.this.arena.hasLoadFailed()) {
                  Game.this.abortLoading();
               } else if (Game.this.arena.isReady()) {
                  Game.this.startGame();
               }
            }
         }, 20L, 20L).getTaskId();
      }
   }

   public void makeSolo() {
      this.solo = true;
      this.timer = 10;
   }

   public boolean isSolo() {
      return this.solo;
   }

   public void updateScoreboard() {
      if (this.gameStarted && !this.ending && this.mapLength > 0 && !this.dragonPoints.isEmpty()) {
         for (Player p : this.players) {
            if (!this.currentDragonpoints.containsKey(p)) continue;
            int currDragonpoint = this.currentDragonpoints.get(p);
            int dragonpoint = currDragonpoint;
            if (currDragonpoint + 1 < this.dragonPoints.size()) {
               dragonpoint = currDragonpoint + 1;
            }

            Location playerLocation = p.getLocation();
            int distance = (int)playerLocation.distance(this.dragonPoints.get(dragonpoint));
            if (distance <= 20) {
               currDragonpoint = Integer.min(this.dragonPoints.size() - 1, currDragonpoint + 1);
               distance = (int)playerLocation.distance(this.dragonPoints.get(Integer.min(this.dragonPoints.size() - 1, dragonpoint + 1)));
            }

            this.currentDragonpoints.put(p, currDragonpoint);
            this.currentDistance.put(p, distance);
         }

         if (++this.scoreboardTicks % 4 != 0) return;
         for (Player p : this.players) {
            if (p.getGameMode() != GameMode.SPECTATOR && this.currentDistance.containsKey(p)) {
               Score s = this.objective.getScore(p.getName());
               int newScore = 100 - this.getDistanceToFinish(p) * 100 / this.mapLength;
               if (newScore > s.getScore()) {
                  s.setScore(newScore);
               }
            }
         }
      }
   }

   private int getDistanceToFinish(Player p) {
      return this.currentDistance.get(p) + this.distances.get(this.currentDragonpoints.get(p));
   }

   private int getDistance(Location l, int dragonpoint) {
      return (int)l.distance(this.dragonPoints.get(dragonpoint));
   }

   public void hideAllPlayers(Player pl) {
      for (Player p : Bukkit.getOnlinePlayers()) {
         pl.hidePlayer(p);
      }
   }

   public void showSolo() {
      if (this.solo) {
         if (this.players.size() >= 1) {
            try {
               if (!DragonEscape.getInstance().getGameManager().getDeSettings(this.players.get(0)).showPlayers()) {
                  return;
               }
            } catch (SQLException var4) {
               var4.printStackTrace();
            }

            for (Player p : DragonEscape.getInstance().getGameManager().getPlayers().keySet()) {
               if (p != this.players.get(0)) {
                  Game g = DragonEscape.getInstance().getGameManager().getGame(p);
                  if (g != null && g.getArena() != null && g.getArenaName().equals(this.getArenaName()) && g.getArena().COORDS == this.arena.COORDS) {
                     this.players.get(0).showPlayer(p);
                  }
               }
            }
         }
      }
   }

   public void showSolo(Player p) {
      if (this.solo) {
         if (this.players.size() >= 1) {
            try {
               if (!DragonEscape.getInstance().getGameManager().getDeSettings(this.players.get(0)).showPlayers()) {
                  return;
               }
            } catch (SQLException var3) {
               var3.printStackTrace();
            }

            Game g = DragonEscape.getInstance().getGameManager().getGame(p);
            if (g != null) {
               if (g.getArena() != null) {
                  if (g.getArenaName().equals(this.getArenaName()) && g.getArena().COORDS == this.arena.COORDS) {
                     this.players.get(0).showPlayer(p);
                  }
               }
            }
         }
      }
   }

   private void abortLoading() {
      for (Player player : new ArrayList<>(this.players)) {
         player.sendMessage(ChatColor.RED + "Arena loading failed. Please try another map; see the server log.");
         this.removePlayer(player);
      }
      this.endGame();
   }

   public void startGame() {
      this.timerStarted = false;
      if (this.players.size() == 0 && !this.solo) {
         DragonEscape.getInstance().getGameManager().removeGame(this);
         this.arena.restore();
      } else if (this.players.size() != 0) {
         if (this.arena.COORDS != -1) {
            ArenaUtils.makeUnavailable(this.arena);
         }

         if (this.solo) {
            if (this.taskID != -1) {
               Bukkit.getScheduler().cancelTask(this.taskID);
            }

            Player p = this.players.get(0);
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            this.spawnpoint = this.getNextSpawnpoint();
            if (!this.spawnpoint.getChunk().isLoaded()) {
               this.spawnpoint.getChunk().load();
            }

            p.teleport(this.spawnpoint);
            p.getInventory().clear();
            p.getInventory().setItem(6, InventoryUtils.getSettingsBook());
            p.getInventory().setItem(7, InventoryUtils.getQuitItem());
            p.getInventory().setItem(8, InventoryUtils.getRestartItem());
            Kit k = KitManager.getKit(p);
            if (!k.isSoloFriendly()) {
               k = KitManager.getKit("leap");
               KitManager.setKit(p, KitManager.getKit("leap"));
            }

            if (k != null) {
               k.giveItems(p);
            }

            DisruptorKit.removeTime(p);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            DragonEscape.getInstance().getGameManager().addGameInProgress(this.gameID, this);
            p.setPlayerTime(this.arena.getArenaTime(), false);

            try {
               GameManager.showScoreboard(p, this);
            } catch (SQLException var7) {
               p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var7.getLocalizedMessage()));
               var7.printStackTrace();
            }

            try {
               GameManager.getArenaStats(p.getName(), this.arenaName).addJoin();
            } catch (Exception var6) {
               var6.printStackTrace();
            }
         } else {
            this.frozen = true;
            PotionEffect effect = new PotionEffect(PotionEffectType.INVISIBILITY, 3000000, 1);

            for (Player px : this.players) {
               try {
                  if (DragonEscape.getInstance().getGameManager().getDeSettings(px).showPlayers()) {
                     for (Player pl : this.players) {
                        if (pl != px) {
                           px.hidePlayer(px);
                        }
                     }
                  }
               } catch (SQLException var8) {
                  var8.printStackTrace();
                  px.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var8.getLocalizedMessage()));
               }

               px.addPotionEffect(effect);
               if (this.toSpectate.contains(px)) {
                  px.setGameMode(GameMode.ADVENTURE);
                  px.teleport(this.getFirstSpawnpoint());
               } else {
                  px.setGameMode(GameMode.ADVENTURE);
                  px.teleport(this.getNextSpawnpoint());
                  px.getInventory().clear();
                  Kit kx = KitManager.getKit(px);
                  if (!kx.isPublicFriendly()) {
                     kx = KitManager.getKit("leap");
                     KitManager.setKit(px, KitManager.getKit("leap"));
                  }

                  if (kx != null) {
                     kx.giveItems(px);
                  }

                  px.setHealth(20.0);
                  px.setFoodLevel(20);
                  px.setPlayerTime(this.arena.getArenaTime(), false);
                  this.currentDragonpoints.put(px, 0);
               }
            }

            this.updateScoreboard();

            for (Player px : this.players) {
               if (this.isAlive(px) || this.toSpectate.contains(px)) {
                  px.setScoreboard(this.scoreboard);
               }
            }

            DragonEscape.getInstance().getGameManager().addGameInProgress(this.gameID, this);
            this.active = true;
            InventoryUtils.updateInventories();
            DragonEscape.getInstance().getGameManager().createPublicLobby();
            Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
               @Override
               public void run() {
                  for (Player p : Game.this.players) {
                     DragonEscape.sendTitle(p, "3", ChatColor.RED);
                     p.playSound(p.getLocation(), Sound.NOTE_PLING, 100.0F, 0.0F);
                  }
               }
            }, 20L);
            Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
               @Override
               public void run() {
                  for (Player p : Game.this.players) {
                     DragonEscape.sendTitle(p, "2", ChatColor.YELLOW);
                     p.playSound(p.getLocation(), Sound.NOTE_PLING, 100.0F, 1.0F);
                  }
               }
            }, 40L);
            Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
               @Override
               public void run() {
                  for (Player p : Game.this.players) {
                     DragonEscape.sendTitle(p, "1", ChatColor.GREEN);
                     p.playSound(p.getLocation(), Sound.NOTE_PLING, 100.0F, 2.0F);
                  }
               }
            }, 60L);
            Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
               @Override
               public void run() {
                  for (Player p : Game.this.players) {
                     DragonEscape.sendTitle(p, "GO", ChatColor.GREEN);
                     p.playSound(p.getLocation(), Sound.NOTE_PLING, 100.0F, 3.0F);
                  }

                  Game.this.frozen = false;
                  if (Game.this.arena.getDragonPoints().size() != 0) {
                     Game.this.dragon = new Dragon(Game.this, Game.this.dragonSpeed);
                     Game.this.dragon.startFlying();
                  }

                  Game.this.gameStarted = true;
                  Game.this.startTime = Calendar.getInstance().getTimeInMillis();
               }
            }, 80L);
         }
      }
   }

   public boolean isActive() {
      return this.active;
   }

   public void titleTimerTick() {
      if (this.gameStarted) {
         String message = ChatColor.GOLD + "Timer: " + String.format("%.1f", this.timerTitle);

         for (Player p : this.players) {
            try {
               if (DragonEscape.getInstance().getGameManager().getDeSettings(p).displayTimer()) {
                  DragonEscape.sendTimer(p, message);
               }
            } catch (SQLException var5) {
               var5.printStackTrace();
               p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var5.getLocalizedMessage()));
            }
         }

         this.timerTitle += 0.1;
      }
   }

   public void vote(Player p, int vote) {
      this.votes.put(p, vote);
   }

   public void voteMap(Player p, int map) {
      this.mapVotes.put(p, map);
   }

   public void addToSpectate(Player p) {
      this.toSpectate.add(p);
   }

   public void removeToSpectate(Player p) {
      this.toSpectate.remove(p);
   }

   public boolean isToSpectate(Player p) {
      return this.toSpectate.contains(p);
   }

   public void endGame() {
      if (this.ending) return;
      this.ending = true;
      if (this.taskID != -1) {
         Bukkit.getScheduler().cancelTask(this.taskID);
         this.taskID = -1;
      }
      boolean end = false;
      if (this.dragon != null) {
         this.dragon.removeDragon();
      }

      String endMessage = "";
      if (this.times.size() == 0) {
         endMessage = Lang.getMessage("no-one-won");
      } else {
         endMessage = Lang.getMessage("end-message");
         int counter = 1;
         StringBuilder results = new StringBuilder();

         for (Player winner : this.times.keySet()) {
            results.append(
               Lang.getMessage("result")
                  .replaceAll("%place%", "" + counter)
                  .replaceAll("%name%", winner.getName())
                  .replaceAll("%time%", this.timesString.get(winner))
            );
            if (DragonEscape.isCubicsEnabled()) {
               int award = 0;
               byte var25;
               switch (counter) {
                  case 1:
                     var25 = 50;
                     break;
                  case 2:
                     var25 = 30;
                     break;
                  case 3:
                     var25 = 24;
                     break;
                  default:
                     var25 = 20;
               }

               if (this.solo) {
                  try {
                     if (DragonEscape.getInstance().getGameManager().getDeSettings(winner).sendRewardMessages()) {
                        winner.sendMessage(Lang.getMessage("cubics-awarded").replaceAll("%amount%", "20"));
                     }
                  } catch (SQLException var16) {
                     var16.printStackTrace();
                     winner.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var16.getLocalizedMessage()));
                  }

                  Cubics.getInstance().getCubicsAPI().addCubics(winner, 20);

                  try {
                     if (this.players.size() <= 0) {
                        GameManager.clearScoreboard(winner);
                     } else {
                        Player p = this.players.get(0);

                        try {
                           ArenaStats as = GameManager.getArenaStats(p.getName(), this.arenaName);
                           as.addFinish();
                           this.addPlayTime();
                           as.save();
                        } catch (Exception var15) {
                           var15.printStackTrace();
                           p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var15.getLocalizedMessage()));
                        }

                        if (DragonEscape.getInstance().getGameManager().getDeSettings(this.players.get(0)).autoRejoin() && this.solo) {
                           this.arena.restore(false);
                           p.teleport(this.spawnpoint);

                           for (int i = 0; i < 6; i++) {
                              p.getInventory().setItem(i, new ItemStack(Material.AIR));
                           }

                           KitManager.getKit(p).giveItems(p);
                           KitManager.removeCooldown(p);
                           p.setHealth(20.0);
                           p.setFoodLevel(20);
                           p.setFireTicks(0);
                           p.resetPlayerTime();
                           GameManager.showScoreboard(p, this);
                           end = true;
                           this.gameStarted = true;
                        }
                     }
                  } catch (SQLException var17) {
                     var17.printStackTrace();
                     this.players.get(0).sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var17.getLocalizedMessage()));
                  }
               } else {
                  try {
                     MySQLConnector.addStats(winner.getUniqueId().toString(), counter - 1, 0, 0, 0, 0);
                  } catch (SQLException var14) {
                     var14.printStackTrace();
                  }

                  try {
                     if (DragonEscape.getInstance().getGameManager().getDeSettings(winner).sendRewardMessages()) {
                        winner.sendMessage(Lang.getMessage("cubics-awarded").replaceAll("%amount%", "" + var25));
                     }
                  } catch (SQLException var13) {
                     var13.printStackTrace();
                     winner.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var13.getLocalizedMessage()));
                  }

                  Cubics.getInstance().getCubicsAPI().addCubics(winner, var25);
               }
            }

            counter++;

            try {
               if (MySQLConnector.addTime(
                  winner.getUniqueId().toString(), this.arenaName, this.times.get(winner), this.solo, KitManager.getKit(winner).getKitName()
               )) {
                  winner.sendMessage(
                     Lang.getMessage("new-personal-best").replaceAll("%arena%", this.arenaName).replaceAll("%time%", this.timesString.get(winner))
                  );
               }
            } catch (SQLException var12) {
               var12.printStackTrace();
               winner.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var12.getLocalizedMessage()));
            }
         }

         endMessage = endMessage.replaceAll("%results%", results.toString());
      }

      this.timerStarted = false;
      DragonEscape.getInstance().getGameManager().removeGameInProgress(this.gameID);
      this.timerTitle = 0.0;
      if (end) {
         this.ending = false;
         this.times.clear();
      } else {
         this.active = false;

         for (Player p : this.players) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            p.sendMessage(endMessage);
            if (!this.times.containsKey(p)) {
               try {
                  MySQLConnector.addStats(p.getUniqueId().toString(), 3, 1, 0, 0, 0);
               } catch (SQLException var11) {
                  var11.printStackTrace();
               }
            }
         }

         if (this.scoreboard != null) {
            for (Player px : this.players) {
               px.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
         }

         DragonEscape.getInstance().getGameManager().removeGameFromLists(this);
         final boolean gameStarted2 = this.gameStarted;
         this.gameStarted = false;
         Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
            @Override
            public void run() {
               if (gameStarted2 && !Game.this.solo) {
                  Game newGame = DragonEscape.getInstance().getGameManager().getAvailableGame(Game.this.players.size());
                  if (newGame != null) {
                     DragonEscape.getInstance().getGameManager().removeGame(Game.this);

                     for (Player p : new ArrayList<>(Game.this.players)) {
                        if (p.isOnline()) newGame.addPlayer(p);
                     }
                  } else {
                     DragonEscape.getInstance().getGameManager().createGame(Game.this);
                  }
               } else {
                  DragonEscape.getInstance().getGameManager().removeGame(Game.this);
               }

               for (Player p : Game.this.players) {
                  p.resetPlayerTime();
                  p.setGameMode(GameMode.ADVENTURE);
               }

               if (Game.this.arena != null) {
                  Game.this.arena.restore();
               }

               InventoryUtils.updateInventories();
            }
         }, 60L);
      }
   }

   public String getArenaName() {
      return this.arenaName;
   }

   public Location getFirstSpawnpoint() {
      return this.arena.getSpawnpoints().get(0);
   }

   public Location getNextSpawnpoint() {
      if (this.spawnPointsIterator == null || !this.spawnPointsIterator.hasNext()) {
         this.spawnPointsIterator = this.arena.getSpawnpoints().iterator();
      }

      Location l = this.spawnPointsIterator.next();
      Chunk c = l.getChunk();
      if (!c.isLoaded()) {
         ArenaUtils.getWorld().loadChunk(l.getChunk());
      }

      return l;
   }

   public Location getNextDragonpoint() {
      return !this.dragonPointsIterator.hasNext() ? null : this.dragonPointsIterator.next();
   }

   public void updateVotingInventories() {
      for (Player p : this.players) {
         if (InventoryUtils.isVotingInventoryOpen(p)) {
            this.openVotingInventory(p);
         }
      }
   }

   public void addPlayer(Player p) {
      if (this.isAcceptingPlayers()) {
         if (!this.players.contains(p)) {
            this.players.add(p);
         }

         String message = !this.isGameStarted()
            ? Lang.getMessage("joined").replaceAll("%player%", p.getName())
            : Lang.getMessage("spectating").replaceAll("%player%", p.getName());

         for (Player pl : this.players) {
            pl.showPlayer(p);

            try {
               if (DragonEscape.getInstance().getGameManager().getDeSettings(pl).sendJoinMessages()) {
                  pl.sendMessage(message);
               }
            } catch (SQLException var6) {
               var6.printStackTrace();
               pl.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var6.getLocalizedMessage()));
            }
         }

         if (!this.solo) {
            this.team.addPlayer(p);
         }

         p.setHealth(20.0);
         p.setFoodLevel(20);
         p.setGameMode(GameMode.ADVENTURE);
         DragonEscape.getInstance().getGameManager().removeFromNoDamage(p);
         DragonEscape.getInstance().getGameManager().addInventory(p);
         p.getInventory().clear();
         DragonEscape.getInstance().getGameManager().addPlayer(p, this);
         if (!this.isGameStarted()) {
            ItemStack kitSelector = new ItemStack(Material.STICK, 1);
            ItemMeta kitSelectorMeta = kitSelector.getItemMeta();
            kitSelectorMeta.setDisplayName(ChatColor.WHITE + "Kit Selector");
            kitSelector.setItemMeta(kitSelectorMeta);
            if (!this.solo) {
               this.giveVotingBook(p);
            }

            p.getInventory().setItem(4, kitSelector);
            p.getInventory().setItem(8, InventoryUtils.getQuitItem());
            p.teleport(DragonEscape.getLobby());
            if (this.dontChangeTimer && this.players.size() >= 3) {
               this.timer = 15;
            }

            if (this.players.size() >= 2) {
               this.timerStarted = true;
            }

            if (this.solo) {
               this.timerStarted = true;
            }
         } else {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(this.getFirstAlivePlayer());
         }

         for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!this.players.contains(pl)) {
               p.hidePlayer(pl);
            }
         }

         InventoryUtils.updateInventories();
      }
   }

   private void giveVotingBook(Player p) {
      ItemStack votingBook = new ItemStack(Material.BOOK, 1);
      ItemMeta votingBookMeta = votingBook.getItemMeta();
      votingBookMeta.setDisplayName(Lang.getMessage("voting-book-name"));
      votingBook.setItemMeta(votingBookMeta);
      p.getInventory().setItem(0, votingBook);
   }

   public void removePlayer(Player p) {
      if (!this.gameStarted && !this.solo) {
         this.mapVotes.remove(p);
         this.votes.remove(p);
         this.updateVotingInventories();
      }

      p.setAllowFlight(false);
      p.setFlying(false);

      for (PotionEffect effect : p.getActivePotionEffects()) {
         p.removePotionEffect(effect.getType());
      }

      if (this.isToSpectate(p)) {
         this.removeToSpectate(p);
      }

      if (!this.solo && this.scoreboard != null) {
         this.objective.getScore(p.getName()).setScore(-1);
         p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
      }

      if (this.solo) {
         try {
            MySQLConnector.addStats(p.getUniqueId().toString(), -1, this.soloDeaths, 1, 0, 0);

            try {
               ArenaStats as = GameManager.getArenaStats(p.getName(), this.arenaName);
               as.addLeave();
               this.addPlayTime();
               as.save();
            } catch (Exception var7) {
               var7.printStackTrace();
               p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var7.getLocalizedMessage()));
            }
         } catch (SQLException var8) {
            var8.printStackTrace();
         }
      }

      p.removePotionEffect(PotionEffectType.INVISIBILITY);
      this.players.remove(p);
      p.setHealth(20.0);
      p.setFoodLevel(20);
      p.teleport(DragonEscape.getGlobalLobby());
      String message = Lang.getMessage("player-left").replaceAll("%player%", p.getName());

      for (Player pl : Bukkit.getOnlinePlayers()) {
         p.showPlayer(pl);
         if (this.players.contains(pl)) {
            try {
               if (DragonEscape.getInstance().getGameManager().getDeSettings(pl).sendJoinMessages()) {
                  pl.sendMessage(message);
               }
            } catch (SQLException var6) {
               var6.printStackTrace();
               pl.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var6.getLocalizedMessage()));
            }
         }
      }

      p.setGameMode(GameMode.ADVENTURE);
      p.setFireTicks(0);
      p.resetPlayerTime();
      DragonEscape.getInstance().getGameManager().restoreInventory(p);
      DragonEscape.getInstance().getGameManager().removePlayer(p);
      if (this.players.size() < 2 && this.timerStarted) {
         for (Player plx : this.players) {
            plx.sendMessage(Lang.getMessage("not-enough-players"));
            this.giveVotingBook(plx);
         }

         this.timer = 30;
         this.timerStarted = false;
      }

      if (this.getNumberOfAlivePlayers() == 0 && this.arenaName != null && !this.arenaName.equals("none")) {
         this.endGame();
         DragonEscape.getInstance().getGameManager().removeGame(this);
      } else if (this.players.size() == 0 && !this.arenaName.equals("none")) {
         this.endGame();
         DragonEscape.getInstance().getGameManager().removeGame(this);
      } else {
         KitManager.removeCooldown(p);
         InventoryUtils.updateInventories();
         DragonEscape.getInstance().getGameManager().restoreInventory(p);
         if (this.players.size() == 0 && !this.solo && !this.arenaName.equals("none")) {
            DragonEscape.getInstance().getGameManager().removeGame(this);
            this.arena.restore();
         }
      }
   }

   public boolean isAcceptingPlayers() {
      return !this.ending && this.slots() > this.players.size();
   }

   public List<Player> getPlayers() {
      return this.players;
   }

   public Dragon getDragon() {
      return this.dragon;
   }

   public void addPlayTime() throws SQLException {
      if (this.solo && this.players.size() != 0 && this.startTime != -1L) {
         GameManager.getArenaStats(this.players.get(0).getName(), this.arenaName)
            .addPlayTime((int)(Calendar.getInstance().getTimeInMillis() - this.startTime) / 1000);
         this.startTime = -1L;
      }
   }

   public void timerTick() {
      if (this.timer == 1 && this.arena != null && !this.arena.isReady()) {
         if (this.arena.hasLoadFailed()) abortLoading();
         return;
      }
      if (this.timer == 30 || this.timer == 20 || this.timer == 10 || this.timer == 5 || this.timer <= 3) {
         for (Player p : this.players) {
            p.sendMessage(Lang.getMessage("game-starting-in").replaceAll("%time%", this.timer + " second" + (this.timer != 1 ? "s" : "")));
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 100.0F, 1.0F);
         }
      }

      if (this.timer == 15) {
         this.votingEnabled = false;
         String selectedArena = null;
         if (this.arenaName.equalsIgnoreCase("none")) {
            if (this.mapVotes.size() > 0) {
               this.arenaName = this.arenas[Collections.max(this.mapVotes.values())];
            } else {
               this.arenaName = this.arenas[new Random().nextInt(3)];
            }

            try {
               this.arena = ArenaUtils.getArena(this.arenaName, false);
            } catch (Exception var6) {
               Exception e = var6;
               var6.printStackTrace();

               for (Player p : this.players) {
                  p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", e.getLocalizedMessage()));
               }
               this.abortLoading();
               return;
            }

            ArenaUtils.makeUnavailable(this.arena);
            selectedArena = Lang.getMessage("selected-arena").replaceAll("%arena%", this.arenaName);
            this.dragonPoints = this.arena.getDragonPoints();
            this.dragonPointsIterator = this.arena.getDragonPoints().iterator();
            if (this.dragonPoints.size() >= 1) {
               Location l = this.dragonPoints.get(this.dragonPoints.size() - 1);
               int currDistance = 0;
               this.distances.put(this.dragonPoints.size() - 1, currDistance);

               for (int i = this.dragonPoints.size() - 2; i >= 0; i--) {
                  currDistance = (int)((double)currDistance + this.dragonPoints.get(i).distance(l));
                  this.distances.put(i, currDistance);
                  l = this.dragonPoints.get(i);
               }

               this.mapLength = currDistance;
            }
         }

         int speed = 0;
         if (this.votes.values().size() > 0) {
            speed = Collections.max(this.votes.values());
         }

         this.dragonSpeed = DragonEscape.getConfiguration().getDouble("dragon-speed-" + (speed + 1));
         String selectedSpeed;
         if (speed == 0) {
            selectedSpeed = Lang.getMessage("selected-speed").replaceAll("%speed%", Lang.getMessage("easy-speed"));

            for (Player p : this.players) {
               p.playSound(p.getLocation(), Sound.WOLF_BARK, 100.0F, 0.0F);
            }
         } else if (speed == 1) {
            selectedSpeed = Lang.getMessage("selected-speed").replaceAll("%speed%", Lang.getMessage("medium-speed"));

            for (Player p : this.players) {
               p.playSound(p.getLocation(), Sound.IRONGOLEM_DEATH, 100.0F, 0.0F);
            }
         } else {
            selectedSpeed = Lang.getMessage("selected-speed").replaceAll("%speed%", Lang.getMessage("hard-speed"));

            for (Player p : this.players) {
               p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 100.0F, 0.0F);
            }
         }

         for (Player p : this.players) {
            p.getInventory().setItem(0, new ItemStack(Material.AIR));
            p.sendMessage(Lang.getMessage("no-voting"));
            p.sendMessage(selectedSpeed);
            p.closeInventory();
            if (selectedArena != null) {
               p.sendMessage(selectedArena);
            }
         }
      }

      if (this.timer == 1) {
         this.timerStarted = false;
         this.startGame();
      }

      this.timer--;
   }

   public void finish(Player p) {
      if (!this.times.containsKey(p)) {
         String s = this.saveTime(p);
         String goalMessage = Lang.getMessage("reached-goal").replaceAll("%player%", p.getName()).replaceAll("%time%", s);

         for (Player pl : this.players) {
            pl.sendMessage(goalMessage);
         }

         if (this.getNumberOfAlivePlayers() - 1 == 0) {
            this.endGame();
         } else {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(this.getFirstAlivePlayer());
            if (!this.solo) {
               this.objective.getScore(p.getName()).setScore(0);
            }
         }
      }
   }

   public int getNumberOfAlivePlayers() {
      int alivePlayers = 0;

      for (Player p : this.players) {
         if (p.getGameMode() != GameMode.SPECTATOR) {
            alivePlayers++;
         }
      }

      return alivePlayers;
   }

   public void die(Player p) {
      if (this.gameStarted && p.getGameMode() != GameMode.SPECTATOR) {
         DragonEscape.getInstance().getGameManager().removeLiquidDamage(p);
         if (this.solo) {
            this.restart(true);
         } else {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            String message = Lang.getMessage("died").replaceAll("%player%", p.getName());

            for (Player pl : this.players) {
               pl.sendMessage(message);
            }

            KitManager.removeCooldown(p);
            if (this.getNumberOfAlivePlayers() - 1 == 0) {
               p.setGameMode(GameMode.ADVENTURE);
               this.endGame();
               return;
            }

            p.teleport(this.getFirstAlivePlayer());
            p.setGameMode(GameMode.SPECTATOR);
            this.objective.getScore(p.getName()).setScore(-1);
         }
      }
   }

   public Player getFirstAlivePlayer() {
      for (Player p : this.players) {
         if (p.getGameMode() != GameMode.SPECTATOR) {
            return p;
         }
      }

      this.endGame();
      return null;
   }

   public boolean isGameStarted() {
      return this.gameStarted;
   }

   public boolean isTimerStarted() {
      return this.timerStarted;
   }

   public int getGameID() {
      return this.gameID;
   }

   public int slots() {
      return this.solo ? 1 : (this.arena == null ? 16 : this.arena.getSpawnpoints().size());
   }

   public int getTimer() {
      return this.timer;
   }

   public long getTime(Player p) {
      return !this.times.containsKey(p) ? -1L : this.times.get(p) - this.startTime;
   }

   private String saveTime(Player p) {
      long time = Calendar.getInstance().getTimeInMillis() - this.startTime;
      this.times.put(p, time);
      String s = TimeUtils.formatTime(time);
      this.timesString.put(p, s);
      return s;
   }

   public void restart(boolean death) {
      if (this.solo) {
         this.ending = false;
         this.timerTitle = 0.0;
         this.gameStarted = false;
         Player p = this.players.get(0);
         DragonEscape.getInstance().getGameManager().addToNoDamage(p);

         for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
         }

         for (int i = 0; i < 6; i++) {
            p.getInventory().setItem(i, new ItemStack(Material.AIR, 1));
         }

         p.teleport(this.spawnpoint);
         p.setFireTicks(0);
         p.setHealth(20.0);

         try {
            if (DragonEscape.getInstance().getGameManager().getDeSettings(p).sendResetMessages()) {
               p.sendMessage(Lang.getMessage("timer-reset"));
            }
         } catch (SQLException var6) {
            var6.printStackTrace();
            p.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var6.getLocalizedMessage()));
         }

         KitManager.removeCooldown(p);
         Kit k = KitManager.getKit(p);
         if (!k.isSoloFriendly()) {
            k = KitManager.getKit("leap");
            KitManager.setKit(p, KitManager.getKit("leap"));
         }

         if (k != null) {
            k.giveItems(p);
         } else {
            KitManager.getKit("leap").giveItems(p);
         }

         this.arena.restore(false);
         DisruptorKit.reset(this);
         DisruptorKit.resetTimes(this);
         this.soloDeaths++;

         try {
            this.addPlayTime();
            GameManager.getArenaStats(p.getName(), this.arenaName).addDeath();
            if (!death) {
               GameManager.getArenaStats(p.getName(), this.arenaName).addReset();
            }
         } catch (Exception var5) {
            var5.printStackTrace();
            p.sendMessage(Lang.getMessage("error-occured").replaceAll("%syntax%", var5.getLocalizedMessage()));
         }
      }
   }

   public boolean isAlive(Player p) {
      return p.getGameMode() != GameMode.SPECTATOR;
   }

   public void startTimer() {
      this.startTime = Calendar.getInstance().getTimeInMillis();
      this.gameStarted = true;
      this.players.get(0).setHealth(20.0);
      DisruptorKit.resetTime(this.players.get(0));
      DragonEscape.getInstance().getGameManager().removeFromNoDamage(this.players.get(0));
   }

   public Arena getArena() {
      return this.arena;
   }

   public boolean isFrozen() {
      return this.frozen;
   }

   public String[] getArenaChoice() {
      return this.arenas;
   }
}
