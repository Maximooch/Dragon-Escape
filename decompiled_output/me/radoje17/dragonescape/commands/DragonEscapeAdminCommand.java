package me.radoje17.dragonescape.commands;

import com.sk89q.worldedit.WorldEditException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import me.radoje17.dragonescape.Leaderboard;
import me.radoje17.dragonescape.MySQLConnector;
import me.radoje17.dragonescape.NBTEditor;
import me.radoje17.dragonescape.PlayerCountHologram;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.InventoryUtils;
import me.radoje17.dragonescape.utils.LocationUtils;
import me.radoje17.dragonescape.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DragonEscapeAdminCommand implements CommandExecutor, Listener {
   private final String PREFIX = ChatColor.DARK_GRAY + "[pc] " + ChatColor.GRAY;
   private static HashMap<Player, DragonEscapeAdminCommand.Configuring> configuring;
   private static ItemStack addSpawnpoint;
   private static ItemStack addDragonpoint;

   @EventHandler
   public void rightClick(PlayerInteractEvent e) {
      if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
         if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = e.getItem();
            if (item != null && item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
               if (e.getItem().getItemMeta().getDisplayName().equals(ChatColor.GRAY + "Add Spawnpoint")) {
                  configuring.get(e.getPlayer()).addSpawnpoint(e.getPlayer().getLocation());
                  e.getPlayer().sendMessage("Spawnpoint has been added");
               }

               if (e.getItem().getItemMeta().getDisplayName().equals(ChatColor.GRAY + "Add Dragonpoint")) {
                  configuring.get(e.getPlayer()).addDragonpoint(e.getPlayer().getLocation());
                  e.getPlayer().sendMessage("Dragonpoint has been added");
               }
            }
         }
      }
   }

   @EventHandler
   public void click(InventoryClickEvent e) {
      if (configuring.containsKey(e.getWhoClicked())) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void drop(PlayerDropItemEvent e) {
      if (configuring.containsKey(e.getPlayer())) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void pickup(PlayerPickupItemEvent e) {
      if (configuring.containsKey(e.getPlayer())) {
         e.setCancelled(true);
      }
   }

   public DragonEscapeAdminCommand() {
      configuring = new HashMap<>();
      addSpawnpoint = NBTEditor.getHead("http://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
      ItemMeta addSpawnpointMeta = addSpawnpoint.getItemMeta();
      addSpawnpointMeta.setDisplayName(ChatColor.GRAY + "Add Spawnpoint");
      addSpawnpoint.setItemMeta(addSpawnpointMeta);
      addDragonpoint = NBTEditor.getHead("http://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
      ItemMeta addDragonpointMeta = addDragonpoint.getItemMeta();
      addDragonpointMeta.setDisplayName(ChatColor.GRAY + "Add Dragonpoint");
      addDragonpoint.setItemMeta(addDragonpointMeta);
   }

   public static void removeAllConfigurings() {
      Iterator iter = configuring.keySet().iterator();

      while (iter.hasNext()) {
         removeConfiguring((Player)iter.next());
      }
   }

   private static void removeConfiguring(Player p) {
      p.sendMessage("You are no longer configuring arena " + configuring.get(p).getArenaName());
      p.teleport(DragonEscape.getGlobalLobby());
      configuring.remove(p);
      DragonEscape.getInstance().getGameManager().restoreInventory(p);
   }

   public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
      if (!sender.hasPermission("dragonescape.help")) {
         sender.sendMessage("No perm!");
         return false;
      } else if (args.length == 0) {
         sender.sendMessage("Help");
         sender.sendMessage("/dea create [map] [schematic]");
         sender.sendMessage("/dea remove [map]");
         sender.sendMessage("/dea configure [map]");
         sender.sendMessage("/dea setLobby");
         sender.sendMessage("/dea setGlobalLobby");
         sender.sendMessage("/dea maintenance");
         sender.sendMessage("/dea spawnpoints [map]");
         sender.sendMessage("/dea dragonpoints [map]");
         sender.sendMessage("/dea removespawnpoint [map] [id]");
         sender.sendMessage("/dea removedragonpoint [map] [id]");
         sender.sendMessage("/dea waterkills [map] [true/false]");
         sender.sendMessage("/dea setSchematicName [map] [name] (solo (true/false))");
         sender.sendMessage("/dea reloadLang");
         sender.sendMessage("/dea overwriteLang");
         sender.sendMessage("/dea setLeaderboard");
         sender.sendMessage("/dea setPlayerCount");
         sender.sendMessage("/dea setParkourName [arena] [parkourname]");
         sender.sendMessage("/dea reports [solo/public]");
         sender.sendMessage("/dea setSpawnPoint [arena]");
         sender.sendMessage("/dea removeRun [player] [arena] [solo/public] [kit]");
         sender.sendMessage("/dea setTime [arena] [time]");
         sender.sendMessage("/dea setSpecial [arena] [true/false]");
         sender.sendMessage("/dea removeAllRuns [uuid]");
         return false;
      } else if (args[0].equalsIgnoreCase("removeAllRuns")) {
         if (args.length < 2) {
            sender.sendMessage("/dea " + label + " [uuid]");
            return false;
         } else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(args[1]));
            sender.sendMessage("Removing all " + op.getName() + "'s runs..");
            Bukkit.getScheduler().runTaskAsynchronously(DragonEscape.getInstance(), new Runnable() {
               @Override
               public void run() {
                  try {
                     sender.sendMessage("Removed " + MySQLConnector.disableAllRuns(args[1], (Player)sender, true) + " solo runs!");
                     sender.sendMessage("Removed " + MySQLConnector.disableAllRuns(args[1], (Player)sender, false) + " public runs!");
                  } catch (SQLException var2) {
                     var2.printStackTrace();
                     sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%syntax%", var2.getLocalizedMessage()));
                  }
               }
            });
            return false;
         }
      } else if (args[0].equalsIgnoreCase("fix")) {
         ArenaUtils.fix();
         sender.sendMessage("Fixed!");
         return false;
      } else if (args[0].equalsIgnoreCase("forcestart")) {
         int counter = 0;

         for (Game g : DragonEscape.getInstance().getGameManager().getSortedGames()) {
            if (!g.isGameStarted()) {
               g.startGame();
               counter++;
            }
         }

         sender.sendMessage("Force started " + counter + " game(s)!");
         return false;
      } else if (args[0].equalsIgnoreCase("removeRun")) {
         if (args.length < 5) {
            sender.sendMessage("/dea removeRun [player] [arena] [solo/public] [kit]");
            return false;
         } else {
            String player = args[1];
            String arena = args[2];
            if (!ArenaUtils.isArena(arena)) {
               sender.sendMessage("That arena does not exist!");
               return false;
            } else {
               int solo;
               if (args[3].equalsIgnoreCase("solo")) {
                  solo = 1;
               } else {
                  if (!args[3].equalsIgnoreCase("public")) {
                     sender.sendMessage("/dea removeRun [player] [arena] [solo/public] [kit]");
                     return false;
                  }

                  solo = 0;
               }

               String kit = args[4];
               if (KitManager.getKit(kit) == null) {
                  sender.sendMessage("That kit does not exist!");
                  return false;
               } else {
                  try {
                     ResultSet response = MySQLConnector.prepareStatement(
                           "CALL removeDeRun(\""
                              + player
                              + "\", \""
                              + arena
                              + "\", \""
                              + kit
                              + "\", "
                              + solo
                              + ", \""
                              + (sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "CONSOLE")
                              + "\", "
                              + Calendar.getInstance().getTimeInMillis()
                              + ")"
                        )
                        .executeQuery();
                     if (!response.next()) {
                        sender.sendMessage("No run found for given parameters.");
                        return false;
                     }

                     if (response.getString(1).equalsIgnoreCase("noPlayer")) {
                        sender.sendMessage("That player could not be found!");
                        return false;
                     }

                     if (response.getString(1).equalsIgnoreCase("noRuns")) {
                        sender.sendMessage("No run found for given parameters.");
                        return false;
                     }

                     sender.sendMessage(player + "'s run on arena " + arena + " " + TimeUtils.formatTime(response.getLong(1)) + " has been removed!");
                  } catch (SQLException var13) {
                     var13.printStackTrace();
                     sender.sendMessage(var13.getLocalizedMessage());
                  }

                  return false;
               }
            }
         }
      } else {
         if (args[0].equalsIgnoreCase("uradi")) {
            for (Game gx : DragonEscape.getInstance().getGameManager().getSortedGames()) {
               if (gx.isGameStarted()) {
                  gx.endGame();
               }
            }
         }

         if (args[0].equalsIgnoreCase("setSpawnPoint")) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Player only command.");
               return false;
            } else if (args.length < 2) {
               sender.sendMessage(Lang.getMessage("invalid-syntax").replaceAll("%syntax%", "/dea setSpawnPoint [arena]"));
               return false;
            } else {
               String arenaName = args[1];
               if (!ArenaUtils.isArena(arenaName)) {
                  sender.sendMessage(Lang.getMessage("arena-not-found").replaceAll("%arena%", arenaName));
                  return false;
               } else {
                  Location l = ((Player)sender).getLocation();
                  ArenaUtils.setSpawnPoint(arenaName, l);
                  sender.sendMessage("Solo Spawnpoint set, arena: " + arenaName + " X: " + l.getX() + " Y: " + l.getY() + " Z: " + l.getZ());
                  return false;
               }
            }
         } else if (args[0].equalsIgnoreCase("reports")) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Player only command.");
               return false;
            } else if (args.length < 2) {
               sender.sendMessage("/dea reports [solo/public]");
               return false;
            } else {
               ((Player)sender).openInventory(InventoryUtils.getReportsInventory(0, args[1].equalsIgnoreCase("solo")));
               return false;
            }
         } else if (args[0].equalsIgnoreCase("setParkourName")) {
            if (args.length < 3) {
               sender.sendMessage("/dea setParkourName [arena] [parkourname]");
               return false;
            } else {
               String arenaName = args[1];
               if (!ArenaUtils.isArena(arenaName)) {
                  sender.sendMessage("Arena " + arenaName + " not found");
                  return false;
               } else {
                  String parkourName = args[2];
                  ArenaUtils.setParkourName(arenaName, parkourName);
                  sender.sendMessage("Parkour name has been set successfully " + arenaName + " -> " + parkourName);
                  return false;
               }
            }
         } else if (args[0].equalsIgnoreCase("setLeaderboard")) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Player only command");
               return false;
            } else {
               sender.sendMessage("Leaderboard has been set");
               Location l = ((Player)sender).getLocation();
               DragonEscape.getInstance().getConfig().set("leaderboard-location", LocationUtils.locationToStringWorld(l));
               DragonEscape.getInstance().saveConfig();

               try {
                  Leaderboard.setLocation(l);
               } catch (SQLException var14) {
                  sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var14.getLocalizedMessage()));
                  var14.printStackTrace();
               }

               return false;
            }
         } else if (args[0].equalsIgnoreCase("setPlayerCount")) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Player only command");
               return false;
            } else {
               sender.sendMessage("Player count hologram location has been set");
               Location l = ((Player)sender).getLocation();
               DragonEscape.getInstance().getConfig().set("player-count-location", LocationUtils.locationToStringWorld(l));
               DragonEscape.getInstance().saveConfig();

               try {
                  PlayerCountHologram.setLocation(l);
               } catch (SQLException var15) {
                  sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var15.getLocalizedMessage()));
                  var15.printStackTrace();
               }

               return false;
            }
         } else if (args[0].equalsIgnoreCase("reloadLang")) {
            Lang.reloadConfig();
            sender.sendMessage(Lang.getMessage("lang-reloaded"));
            return false;
         } else if (args[0].equalsIgnoreCase("overwriteLang")) {
            Lang.reloadConfig(true);
            sender.sendMessage(Lang.getMessage("lang-reloaded"));
            return false;
         } else if (args[0].equalsIgnoreCase("overwritelang")) {
            Lang.overwrite();
            return false;
         } else if (args[0].equalsIgnoreCase("setTime")) {
            if (args.length < 3) {
               sender.sendMessage("/dea setTime [arena] [time]");
               return false;
            } else {
               String arenaName = ArenaUtils.getArenaName(args[1]);
               if (!ArenaUtils.isArena(arenaName)) {
                  sender.sendMessage(this.PREFIX + ChatColor.RED + "Arena " + arenaName + " does not exist.");
                  return false;
               } else {
                  long t;
                  try {
                     t = Long.parseLong(args[2]);
                  } catch (Exception var16) {
                     sender.sendMessage("Time has to be a number!");
                     return false;
                  }

                  ArenaUtils.setArenaTime(arenaName, t);
                  sender.sendMessage("Arena " + arenaName + "'s time has been set to " + t);
                  return false;
               }
            }
         } else if (args[0].equalsIgnoreCase("setSchematicName")) {
            if (args.length < 4) {
               sender.sendMessage("/dea setSchematicName [map] [name] (solo (true/false))");
               return false;
            } else {
               String arenaName = ArenaUtils.getArenaName(args[1]);
               if (!ArenaUtils.isArena(arenaName)) {
                  sender.sendMessage(this.PREFIX + ChatColor.RED + "Arena " + arenaName + " does not exist.");
                  return false;
               } else {
                  String schematicName = args[2];
                  boolean solox = false;
                  if (args[3].equalsIgnoreCase("true")) {
                     solox = true;
                  } else if (!args[3].equalsIgnoreCase("false")) {
                     sender.sendMessage("/dea setSchematicName [map] [name] (solo (true/false))");
                     return false;
                  }

                  File f = new File(DragonEscape.getInstance().getDataFolder() + "/schematics/" + schematicName + ".schematic");
                  if (!f.exists()) {
                     sender.sendMessage(this.PREFIX + ChatColor.RED + "Warning, schematic " + schematicName + ".schematic could not be found.");
                  }

                  ArenaUtils.setSchematicName(arenaName, schematicName, solox);
                  sender.sendMessage(
                     this.PREFIX + (solox ? "Solo" : "Public") + " schematic for arena " + arenaName + " has been set to " + schematicName + "."
                  );
                  return false;
               }
            }
         } else if (args[0].equalsIgnoreCase("setSpecial")) {
            if (args.length < 3) {
               sender.sendMessage("/dea setSpecial [arena] [true/false]");
               return false;
            } else {
               String arenaName = ArenaUtils.getArenaName(args[1]);
               if (!ArenaUtils.isArena(arenaName)) {
                  sender.sendMessage(this.PREFIX + ChatColor.RED + "Arena " + arenaName + " does not exist.");
                  return false;
               } else {
                  boolean special = args[2].equalsIgnoreCase("true");
                  ArenaUtils.setSpecial(arenaName, special);
                  if (special) {
                     sender.sendMessage(
                        this.PREFIX
                           + "Arena "
                           + ChatColor.YELLOW
                           + ArenaUtils.getArenaName(arenaName)
                           + ChatColor.GRAY
                           + " is "
                           + ChatColor.GREEN
                           + "special "
                           + ChatColor.GRAY
                           + "now!"
                     );
                  } else {
                     sender.sendMessage(
                        this.PREFIX
                           + "Arena "
                           + ChatColor.YELLOW
                           + ArenaUtils.getArenaName(arenaName)
                           + ChatColor.GRAY
                           + " is not "
                           + ChatColor.GREEN
                           + "special "
                           + ChatColor.GRAY
                           + "anymore!"
                     );
                  }

                  return false;
               }
            }
         } else if (args[0].equalsIgnoreCase("waterkills")) {
            if (args.length < 3) {
               sender.sendMessage("/" + label + " " + args[0] + " [map] [true/false]");
               return false;
            } else {
               String arenaName = ArenaUtils.getArenaName(args[1]);
               if (!ArenaUtils.isArena(arenaName) && !arenaName.equalsIgnoreCase("all")) {
                  sender.sendMessage("Arena " + arenaName + " does not exist.");
                  return false;
               } else {
                  boolean boolValue = false;
                  if (args[2].equalsIgnoreCase("true")) {
                     boolValue = true;
                  } else if (!args[2].equalsIgnoreCase("false")) {
                     sender.sendMessage("/" + label + " " + args[0] + " [map] [true/false]");
                     return false;
                  }

                  if (arenaName.equalsIgnoreCase("all")) {
                     for (String s : ArenaUtils.getArenas()) {
                        ArenaUtils.setWaterKills(s, boolValue);
                     }

                     sender.sendMessage(this.PREFIX + "Water-Kills has been set to " + boolValue + " on all arenas.");
                  } else {
                     ArenaUtils.setWaterKills(arenaName, boolValue);
                     sender.sendMessage(this.PREFIX + "Water-Kills has been set to " + boolValue + " on arena " + arenaName + ".");
                  }

                  return false;
               }
            }
         } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
               sender.sendMessage("/dea " + args[0].toLowerCase() + " [map]");
               return false;
            } else {
               String arenaName = args[1];
               if (!ArenaUtils.isArena(arenaName)) {
                  sender.sendMessage("Arena " + arenaName + " does not exist.");
                  return false;
               } else {
                  ArenaUtils.removeArena(arenaName);
                  sender.sendMessage("Arena " + arenaName + " has been successfully removed.");
                  return false;
               }
            }
         } else if (!args[0].equalsIgnoreCase("spawnpoints") && !args[0].equalsIgnoreCase("dragonpoints")) {
            if (args[0].equalsIgnoreCase("govno") && args.length == 2) {
               for (int i = 0; i < 5; i++) {
                  try {
                     ArenaUtils.pasteSchematic(args[1]);
                  } catch (WorldEditException var17) {
                     var17.printStackTrace();
                  } catch (IOException var18) {
                     var18.printStackTrace();
                  }
               }

               return false;
            } else if (!args[0].equalsIgnoreCase("removespawnpoint") && !args[0].equalsIgnoreCase("removedragonpoint")) {
               if (args[0].equalsIgnoreCase("configure")) {
                  if (!(sender instanceof Player)) {
                     sender.sendMessage("This is player-only command.");
                     return false;
                  } else if (args.length < 2) {
                     sender.sendMessage("/dea configure [map]");
                     return false;
                  } else if (configuring.containsKey(sender)) {
                     removeConfiguring((Player)sender);
                     return false;
                  } else {
                     String arenaName = ArenaUtils.getArenaName(args[1]);
                     if (!ArenaUtils.isArena(arenaName)) {
                        sender.sendMessage("no map " + arenaName);
                        return false;
                     } else {
                        sender.sendMessage("This might take some time.");

                        try {
                           ArenaUtils.pasteSchematic(ArenaUtils.getSchematicName(arenaName));
                        } catch (WorldEditException var19) {
                           sender.sendMessage("World edit exception has occurred: " + var19.getLocalizedMessage());
                           return false;
                        } catch (IOException var20) {
                           sender.sendMessage("IOException has occurred: " + var20.getLocalizedMessage());
                           return false;
                        }

                        int coords = ArenaUtils.getCoords() - 2500;
                        configuring.put((Player)sender, new DragonEscapeAdminCommand.Configuring(arenaName));
                        ((Player)sender).teleport(new Location(ArenaUtils.getWorld(), (double)coords, 60.0, (double)coords));
                        sender.sendMessage("You are now configuring arena " + arenaName);
                        DragonEscape.getInstance().getGameManager().addInventory((Player)sender);
                        ((Player)sender).getInventory().clear();
                        ((Player)sender).getInventory().setItem(3, addSpawnpoint);
                        ((Player)sender).getInventory().setItem(5, addDragonpoint);
                        ((Player)sender).setGameMode(GameMode.CREATIVE);
                        return false;
                     }
                  }
               } else if (args[0].equalsIgnoreCase("create")) {
                  if (args.length < 3) {
                     sender.sendMessage("/dea create [map] [schematic]");
                     return false;
                  } else {
                     String map = args[1];
                     if (ArenaUtils.isArena(map)) {
                        sender.sendMessage(map + " already exists");
                        return false;
                     } else {
                        String schematic = args[2];

                        try {
                           Bukkit.broadcastMessage("Initializing arena " + map + ", expect lag!");
                           ArenaUtils.createArena(map, schematic);
                           Bukkit.broadcastMessage("Job done (" + map + ")!");
                        } catch (IOException | WorldEditException var21) {
                           sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var21.getLocalizedMessage()));
                           var21.printStackTrace();
                        }

                        return false;
                     }
                  }
               } else if (args[0].equalsIgnoreCase("setLobby")) {
                  DragonEscape.setLobby(((Player)sender).getLocation());
                  sender.sendMessage("Lobby has been set!");
                  return false;
               } else if (args[0].equalsIgnoreCase("setGlobalLobby")) {
                  DragonEscape.setGlobalLobby(((Player)sender).getLocation());
                  sender.sendMessage("Global lobby has been set!");
                  return false;
               } else if (args[0].equalsIgnoreCase("maintenance")) {
                  if (DragonEscape.getInstance().getGameManager().reverseMaintenance()) {
                     Bukkit.broadcastMessage("Maintenance mode has been enabled.");
                  } else {
                     Bukkit.broadcastMessage("Maintenance mode has been disabled.");
                  }

                  return false;
               } else if (args[0].equalsIgnoreCase("tp") && sender instanceof Player) {
                  ((Player)sender).teleport(ArenaUtils.getWorld().getSpawnLocation());
                  sender.sendMessage("You have been teleported to the Dragon Escape world.");
                  return false;
               } else if (!args[0].equalsIgnoreCase("benchmark")) {
                  sender.sendMessage("Subcommand " + ChatColor.ITALIC + args[0].toLowerCase() + ChatColor.RESET + " does not exist.");
                  return false;
               } else {
                  sender.sendMessage("Benchmark has been started");
                  if (args.length != 1) {
                     String arenaName = ArenaUtils.getArenaName(args[1]);
                     long startTime = Calendar.getInstance().getTimeInMillis();

                     try {
                        ArenaUtils.pasteSchematic(ArenaUtils.getSchematicName(arenaName));
                        sender.sendMessage(new DragonEscapeAdminCommand.Fensi(arenaName, Calendar.getInstance().getTimeInMillis() - startTime).output());
                     } catch (WorldEditException var24) {
                        var24.printStackTrace();
                     } catch (IOException var25) {
                        sender.sendMessage("Schematic " + arenaName + ".schematic not found.");
                     }

                     return false;
                  } else {
                     List<DragonEscapeAdminCommand.Fensi> benchmark = new ArrayList<>();
                     long startTime = Calendar.getInstance().getTimeInMillis();

                     for (String s : ArenaUtils.getArenas()) {
                        long a = Calendar.getInstance().getTimeInMillis();

                        try {
                           ArenaUtils.pasteSchematic(ArenaUtils.getSchematicName(s));
                           benchmark.add(new DragonEscapeAdminCommand.Fensi(s, Calendar.getInstance().getTimeInMillis() - a));
                        } catch (WorldEditException var22) {
                           var22.printStackTrace();
                        } catch (IOException var23) {
                           System.out.println("Schematic " + s + ".schematic not found.");
                        }
                     }

                     sender.sendMessage("Benchmark done in " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
                     Collections.sort(benchmark);

                     for (DragonEscapeAdminCommand.Fensi f : benchmark) {
                        sender.sendMessage(f.output());
                     }

                     return false;
                  }
               }
            } else if (args.length < 3) {
               sender.sendMessage("/dea " + args[0].toLowerCase() + " [map] [id]");
               return false;
            } else {
               String map = ArenaUtils.getArenaName(args[1]);
               if (!ArenaUtils.isArena(map)) {
                  sender.sendMessage("no arena");
                  return false;
               } else {
                  int id = 0;

                  try {
                     id = Integer.parseInt(args[2]) - 1;
                  } catch (Exception var27) {
                     sender.sendMessage("ID has to be a number.");
                     return false;
                  }

                  String message = args[0].toLowerCase().replaceFirst("remove", "");
                  message = message.substring(0, 1).toUpperCase() + message.substring(1);

                  try {
                     if (args[0].equalsIgnoreCase("removespawnpoint")) {
                        ArenaUtils.removeSpawnpoint(map, id);
                     } else {
                        ArenaUtils.removeDragonpoint(map, id);
                     }
                  } catch (IndexOutOfBoundsException var26) {
                     sender.sendMessage(message + " with id " + id + " does not exist on map " + map + ".");
                     return false;
                  }

                  sender.sendMessage(message + " with id " + id + " has been removed from map " + map + ".");
                  return false;
               }
            }
         } else if (args.length < 2) {
            sender.sendMessage("/dea " + args[0].toLowerCase() + " [map]");
            return false;
         } else {
            String map = ArenaUtils.getArenaName(args[1]);
            if (!ArenaUtils.isArena(map)) {
               sender.sendMessage("no arena");
               return false;
            } else {
               List<Location> points = args[0].equalsIgnoreCase("spawnpoints") ? ArenaUtils.getSpawnpoints(map, 0) : ArenaUtils.getDragonpoints(map, 0);
               int i = 0;
               DecimalFormat format = new DecimalFormat("#.##");
               if (points.size() <= 0) {
                  sender.sendMessage("Arena " + map + " has no " + args[0].toLowerCase());
                  return false;
               } else {
                  sender.sendMessage(args[0].substring(0, 1).toUpperCase() + args[0].substring(1).toLowerCase() + " on arena " + map + ":");

                  for (Location l : points) {
                     sender.sendMessage(++i + ". X: " + format.format(l.getX()) + " Y: " + format.format(l.getY()) + " Z: " + format.format(l.getZ()));
                  }

                  return false;
               }
            }
         }
      }
   }

   private class Configuring {
      private String arenaName;

      public Configuring(String arenaName) {
         this.arenaName = ArenaUtils.getArenaName(arenaName);
      }

      public String getArenaName() {
         return this.arenaName;
      }

      public void addSpawnpoint(Location l) {
         if (l.getX() > 0.0) {
            l.setX(l.getX() % 2500.0);
         }

         if (l.getZ() > 0.0) {
            l.setZ(l.getZ() % 2500.0);
         }

         ArenaUtils.addSpawnpoint(this.arenaName, l);
      }

      public void addDragonpoint(Location l) {
         ArenaUtils.addDragonpoint(
            this.arenaName,
            new Location(
               ArenaUtils.getWorld(),
               l.getBlockX() > 0 ? (double)(l.getBlockX() % 2500) : (double)l.getBlockX(),
               (double)l.getBlockY(),
               l.getBlockZ() > 0 ? (double)(l.getBlockZ() % 2500) : (double)l.getBlockZ()
            )
         );
      }
   }

   class Fensi implements Comparable<DragonEscapeAdminCommand.Fensi> {
      String arenaName;
      long time;

      Fensi(String arenaName, long time) {
         this.arenaName = arenaName;
         this.time = time;
      }

      public int compareTo(DragonEscapeAdminCommand.Fensi o) {
         return Long.compare(this.time, o.time);
      }

      public String output() {
         ChatColor c = ChatColor.RED;
         if (this.time <= 50L) {
            c = ChatColor.GREEN;
         } else if (this.time <= 100L) {
            c = ChatColor.YELLOW;
         }

         return this.arenaName + " - " + c + this.time + "ms";
      }
   }
}
