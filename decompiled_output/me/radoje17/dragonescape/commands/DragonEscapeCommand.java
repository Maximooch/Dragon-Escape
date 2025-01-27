package me.radoje17.dragonescape.commands;

import com.sk89q.worldedit.WorldEditException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.radoje17.dragonescape.ArenaStats;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.GameManager;
import me.radoje17.dragonescape.Lang;
import me.radoje17.dragonescape.MySQLConnector;
import me.radoje17.dragonescape.PracticeGame;
import me.radoje17.dragonescape.kits.Kit;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.kits.LeapVerticalKit;
import me.radoje17.dragonescape.listeners.InventoryListener;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.InventoryUtils;
import me.radoje17.dragonescape.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class DragonEscapeCommand implements CommandExecutor, TabCompleter {
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender.hasPermission("leap") && args.length > 0 && args[0].equalsIgnoreCase("leap")) {
         if (args.length == 1) {
            sender.sendMessage("/dea leap cooldown <value>");
            sender.sendMessage("/dea leap mult <value>");
            sender.sendMessage("/dea leap horizontal <value>");
            sender.sendMessage("/dea leap y <value>");
            sender.sendMessage("/dea leap yAbove <value>");
            return false;
         } else if (!args[1].equalsIgnoreCase("cooldown")
            && !args[1].equalsIgnoreCase("mult")
            && !args[1].equalsIgnoreCase("horizontal")
            && !args[1].equalsIgnoreCase("y")
            && !args[1].equalsIgnoreCase("yAbove")) {
            sender.sendMessage("/dea cooldown <value>");
            sender.sendMessage("/dea mult <value>");
            sender.sendMessage("/dea horizontal <value>");
            sender.sendMessage("/dea y <value>");
            sender.sendMessage("/dea yAbove <value>");
            return false;
         } else if (args.length == 2) {
            sender.sendMessage("/dea leap " + args[1].toLowerCase() + " <value>");
            return false;
         } else {
            try {
               DragonEscape.getConfiguration()
                  .set("kits.leapclassic." + (args[1].equalsIgnoreCase("yabove") ? "yAbove" : args[1].toLowerCase()), Double.parseDouble(args[2]));
               DragonEscape.getInstance().saveConfig();
               LeapVerticalKit.update();
            } catch (Exception var11) {
               sender.sendMessage("Value has to be a number!");
            }

            sender.sendMessage("Value has been set!");
            return false;
         }
      } else if (args.length == 0) {
         for (String s : Lang.getList("dragon-escape-player-help")) {
            sender.sendMessage(s.replaceAll("%command%", label));
         }

         return false;
      } else if (args[0].equalsIgnoreCase("mapstats")) {
         if ((args.length >= 3 || sender instanceof Player) && args.length >= 2) {
            String arenaName = args[1];
            if (!ArenaUtils.isArena(arenaName)) {
               sender.sendMessage(Lang.getMessage("arena-not-found").replaceAll("%arena%", arenaName));
               return false;
            } else {
               arenaName = ArenaUtils.getArenaName(arenaName);
               String player = sender.getName();
               if (args.length > 2) {
                  player = args[2];
               }

               try {
                  ArenaStats stats = GameManager.getArenaStats(player, arenaName);
                  if (stats == null || stats.everythingNull()) {
                     if (player.equalsIgnoreCase(sender.getName())) {
                        sender.sendMessage(Lang.getMessage("no-map-stats-you").replaceAll("%arena%", arenaName));
                     } else {
                        sender.sendMessage(
                           Lang.getMessage("no-map-stats").replaceAll("%player%", Bukkit.getOfflinePlayer(player).getName()).replaceAll("%arena%", arenaName)
                        );
                     }

                     return false;
                  }

                  for (String s : Lang.getList("arena-stats")) {
                     sender.sendMessage(
                        s.replaceAll("%player%", Bukkit.getOfflinePlayer(player).getName())
                           .replaceAll("%arena%", "" + arenaName)
                           .replaceAll("%joins%", "" + stats.getJoins())
                           .replaceAll("%leaves%", "" + stats.getLeaves())
                           .replaceAll("%finishes%", "" + stats.getFinishes())
                           .replaceAll("%deaths%", "" + stats.getDeaths())
                           .replaceAll("%resets%", "" + stats.getReset())
                           .replaceAll("%playtime%", stats.getPlayTime())
                     );
                  }
               } catch (SQLException var19) {
                  var19.printStackTrace();
                  sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var19.getLocalizedMessage()));
               }

               return false;
            }
         } else {
            sender.sendMessage(Lang.getMessage("invalid-syntax").replaceAll("%syntax%", "/" + label + args[0] + " <arena> <player>"));
            return false;
         }
      } else if (!args[0].equalsIgnoreCase("spectate") && !args[0].equalsIgnoreCase("spec")) {
         if (args[0].equalsIgnoreCase("stats")) {
            String player = sender.getName();
            if (args.length >= 2) {
               player = args[1];
            }

            try {
               ResultSet set = MySQLConnector.prepareStatement(
                     "select player_name, wins, 2nd, 3rd, losses, deaths, soloGames, blocksBroken, blocksPlaced from pc_players INNER JOIN de_stats ON de_stats.uuid = pc_players.player_uuid WHERE lower(player_name) = '"
                        + player
                        + "';"
                  )
                  .executeQuery();
               if (!set.next()) {
                  sender.sendMessage(Lang.getMessage("player-not-found").replaceAll("%player%", player));
                  return false;
               }

               String message = Lang.getMessage("stats");
               if (player.equalsIgnoreCase(sender.getName())) {
                  message = message.replaceAll("%player%", "Your");
               } else {
                  message = message.replaceAll("%player%", set.getString(1) + "'s");
               }

               message = message.replaceAll("%1st%", "" + set.getInt(2));
               message = message.replaceAll("%2nd%", "" + set.getInt(3));
               message = message.replaceAll("%3rd%", "" + set.getInt(4));
               message = message.replaceAll("%losses%", "" + set.getInt(5));
               message = message.replaceAll("%deaths%", "" + set.getInt(6));
               message = message.replaceAll("%soloGames%", "" + set.getInt(7));
               message = message.replaceAll("%blocksBroken%", "" + set.getInt(8));
               message = message.replaceAll("%blocksPlaced%", "" + set.getInt(9));
               sender.sendMessage(message);
            } catch (SQLException var12) {
               var12.printStackTrace();
               sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var12.getLocalizedMessage()));
            }

            return false;
         } else if (args[0].equalsIgnoreCase("settings") && sender instanceof Player) {
            try {
               ((Player)sender).openInventory(InventoryUtils.getSettingsInventory((Player)sender));
            } catch (SQLException var13) {
               var13.printStackTrace();
               sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var13.getLocalizedMessage()));
            }

            return false;
         } else if (!args[0].equalsIgnoreCase("lb") && !args[0].equalsIgnoreCase("leaderboard")) {
            if (args[0].equalsIgnoreCase("random")) {
               if (!(sender instanceof Player)) {
                  sender.sendMessage("Player only command");
                  return false;
               } else if (DragonEscape.getInstance().getGameManager().hasGame((Player)sender)) {
                  sender.sendMessage(Lang.getMessage("already-ingame"));
                  return false;
               } else {
                  String arena = ArenaUtils.getRandomArena();
                  KitManager.setKit((Player)sender, KitManager.getRandomSoloKit());

                  try {
                     DragonEscape.getInstance().getGameManager().getSoloGame(arena, (Player)sender);
                  } catch (IOException | WorldEditException var14) {
                     var14.printStackTrace();
                     sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var14.getLocalizedMessage()));
                  }

                  return false;
               }
            } else if (args[0].equalsIgnoreCase("public")) {
               if (DragonEscape.getInstance().getGameManager().isMaintenance()) {
                  sender.sendMessage(Lang.getMessage("maintenance-in-progress"));
                  return false;
               } else {
                  String arenaName;
                  if (args.length >= 2 && ArenaUtils.isArena(arenaName = ArenaUtils.getArenaName(args[1]))) {
                     try {
                        DragonEscape.getInstance().getGameManager().getGame(arenaName).addPlayer((Player)sender);
                     } catch (IOException | WorldEditException var15) {
                        sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var15.getLocalizedMessage()));
                        var15.printStackTrace();
                     }

                     return false;
                  } else {
                     ((Player)sender).openInventory(InventoryUtils.getMapsInventory(0, false, false));
                     return false;
                  }
               }
            } else if (args[0].equalsIgnoreCase("practice")) {
               if (DragonEscape.getInstance().getGameManager().isMaintenance()) {
                  sender.sendMessage(Lang.getMessage("maintenance-in-progress"));
                  return false;
               } else {
                  String arenaName;
                  if (args.length < 2 || !ArenaUtils.isArena(arenaName = ArenaUtils.getArenaName(args[1]))) {
                     ((Player)sender).openInventory(InventoryUtils.getMapsInventory(0, true, true));
                     return false;
                  } else {
                     Kit kit;
                     if (args.length >= 3 && (kit = KitManager.getKit(args[2])) != null && kit.isPracticeFriendly()) {
                        new PracticeGame((Player)sender, kit, arenaName);
                        return false;
                     } else {
                        InventoryListener.addPracticeGameToStart((Player)sender, arenaName);
                        ((Player)sender).openInventory(InventoryUtils.getPracticeKitSelectMenu((Player)sender));
                        return false;
                     }
                  }
               }
            } else if (args[0].equalsIgnoreCase("browse")) {
               ((Player)sender).openInventory(InventoryUtils.getActiveGamesInventory());
               return false;
            } else if (args[0].equalsIgnoreCase("solo") && sender instanceof Player) {
               if (DragonEscape.getInstance().getGameManager().isMaintenance()) {
                  sender.sendMessage(Lang.getMessage("maintenance-in-progress"));
                  return false;
               } else {
                  String arenaName;
                  if (args.length < 2 || !ArenaUtils.isArena(arenaName = ArenaUtils.getArenaName(args[1]))) {
                     ((Player)sender).openInventory(InventoryUtils.getMapsInventory(0, true, false));
                     return false;
                  } else {
                     Kit kit;
                     if (args.length >= 3 && (kit = KitManager.getKit(args[2])) != null && kit.isSoloFriendly()) {
                        KitManager.setKit((Player)sender, kit);

                        try {
                           DragonEscape.getInstance().getGameManager().getSoloGame(arenaName, (Player)sender);
                        } catch (IOException | WorldEditException var16) {
                           var16.printStackTrace();
                           sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var16.getLocalizedMessage()));
                        }

                        return false;
                     } else {
                        InventoryListener.addGameToStart((Player)sender, arenaName);
                        ((Player)sender).openInventory(InventoryUtils.getKitSelectMenu((Player)sender, true));
                        return false;
                     }
                  }
               }
            } else if (args[0].equalsIgnoreCase("leave")) {
               Game g = DragonEscape.getInstance().getGameManager().getGame((Player)sender);
               if (g == null) {
                  sender.sendMessage(Lang.getMessage("not-in-game"));
                  return false;
               } else {
                  g.removePlayer((Player)sender);

                  try {
                     if (DragonEscape.getInstance().getGameManager().getDeSettings((Player)sender).sendJoinMessages()) {
                        sender.sendMessage(Lang.getMessage("you-left"));
                     }
                  } catch (SQLException var17) {
                     var17.printStackTrace();
                     sender.sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var17.getLocalizedMessage()));
                  }

                  return false;
               }
            } else {
               for (String s : Lang.getList("dragon-escape-player-help")) {
                  sender.sendMessage(s.replaceAll("%command%", label));
               }

               return false;
            }
         } else if (args.length < 3) {
            sender.sendMessage(Lang.getMessage("invalid-syntax").replaceAll("%syntax%", "/" + label + " lb [arena] [kit]"));
            return false;
         } else {
            String arenaName = args[1];
            if (!ArenaUtils.isArena(arenaName)) {
               sender.sendMessage(Lang.getMessage("arena-not-found").replaceAll("%arena%", arenaName));
               return false;
            } else {
               String kit = args[2].toLowerCase();
               if (KitManager.getKit(kit) == null) {
                  sender.sendMessage(Lang.getMessage("kit-not-found"));
                  return false;
               } else {
                  int page = 0;
                  if (args.length > 3) {
                     try {
                        page = Integer.parseInt(args[3]) - 1;
                        if (page < 0) {
                           page = 0;
                        }

                        if (page >= 10) {
                           page = 9;
                        }
                     } catch (Exception var18) {
                     }
                  }

                  try {
                     ResultSet setx = MySQLConnector.prepareStatement(
                           "(SELECT min(time) AS \"time\", player_name, '1' AS 'Public' FROM de_times JOIN pc_players ON player_uuid = uuid WHERE arenaName='"
                              + arenaName
                              + "' AND kit='"
                              + kit
                              + "' AND disabled=0 GROUP BY player_name) UNION (SELECT min(time) AS \"time\", player_name, '0' AS 'Public' FROM de_times_solo JOIN pc_players ON player_uuid = uuid WHERE arenaName='"
                              + arenaName
                              + "' AND kit='"
                              + kit
                              + "' AND disabled=0 GROUP BY player_name) ORDER BY time LIMIT "
                              + page * 10
                              + ", 10;"
                        )
                        .executeQuery();
                     int cntr = page * 10 + 1;
                     if (!setx.next()) {
                        if (page == 0) {
                           sender.sendMessage(Lang.getMessage("no-runs-at-all"));
                        } else {
                           sender.sendMessage(Lang.getMessage("no-runs"));
                        }

                        return false;
                     }

                     sender.sendMessage(
                        Lang.getMessage("leaderboard-header")
                           .replaceAll("%arena%", ArenaUtils.getArenaName(arenaName))
                           .replaceAll("%page%", "" + (page + 1))
                           .replaceAll("%kit%", KitManager.getKit(kit).getKitName())
                     );

                     do {
                        String message = Lang.getMessage("leaderboard-line")
                           .replaceAll("%place%", "" + cntr)
                           .replaceAll("%time%", TimeUtils.formatTime(setx.getLong(1)))
                           .replaceAll("%username%", setx.getString(2))
                           .replaceAll("%public%", setx.getBoolean(3) ? "(Public)" : "");
                        sender.sendMessage(message);
                        cntr++;
                     } while (setx.next());
                  } catch (SQLException var20) {
                     var20.printStackTrace();
                  }

                  return false;
               }
            }
         }
      } else if (!(sender instanceof Player)) {
         sender.sendMessage("Player only command");
         return false;
      } else {
         Game g = DragonEscape.getInstance().getGameManager().getGame((Player)sender);
         if (g == null) {
            sender.sendMessage(Lang.getMessage("not-in-game"));
            return false;
         } else if (g.isSolo() || g instanceof PracticeGame) {
            sender.sendMessage(Lang.getMessage("public-only"));
            return false;
         } else if (g.isGameStarted()) {
            sender.sendMessage(Lang.getMessage("game-started"));
            return false;
         } else {
            if (g.isToSpectate((Player)sender)) {
               sender.sendMessage(Lang.getMessage("not-to-spectate"));
               g.removeToSpectate((Player)sender);
            } else {
               sender.sendMessage(Lang.getMessage("to-spectate"));
               g.addToSpectate((Player)sender);
            }

            return false;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
      List<String> lista = new ArrayList<>();
      if (args.length == 0) {
         return Collections.emptyList();
      } else if (args.length == 1) {
         lista.add("leave");
         lista.add("browse");
         lista.add("solo");
         lista.add("solo");
         lista.add("public");
         lista.add("settings");
         lista.add("leaderboard");
         lista.add("stats");
         lista.add("practice");
         lista.add("spectate");
         lista.add("random");
         lista.removeIf(s1 -> !s1.startsWith(args[0].toLowerCase()));
         return lista;
      } else {
         if (args[0].equalsIgnoreCase("practice")) {
            if (args.length == 2) {
               String arenaStart = args[1];

               for (String arena : ArenaUtils.getArenas()) {
                  if (arena.toLowerCase().startsWith(arenaStart.toLowerCase())) {
                     lista.add(arena);
                  }
               }

               return lista;
            }

            if (args.length == 3) {
               for (String k : KitManager.getKits().keySet()) {
                  if (KitManager.getKits().get(k).isSoloFriendly() && !KitManager.getKits().get(k).isBreaksBlocks()) {
                     lista.add(k);
                  }
               }

               return lista;
            }
         }

         return Collections.emptyList();
      }
   }
}
