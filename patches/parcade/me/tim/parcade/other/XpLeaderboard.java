package me.tim.parcade.other;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import me.tim.link.mysql.database.MySQL;
import me.tim.parcade.enums.Lang;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class XpLeaderboard implements CommandExecutor {
   private static Map<Integer, LinkedHashMap<String, Long>> leaderboardPages = new HashMap<>();
   private static boolean running = false;

   public static void updateLeaderboardScheduler() {
      if (!running) {
         running = true;
         final String command = "SELECT pc_players.player_name, parkourXp FROM cubics_data INNER JOIN pc_players ON player_uuid = uuid ORDER BY parkourXp DESC LIMIT 1000";
         (new BukkitRunnable() {
            public void run() {
               XpLeaderboard.leaderboardPages.clear();
               ResultSet set = MySQL.query(command);

               try {
                  int pageEntry = 0;
                  int page = 1;
                  LinkedHashMap<String, Long> thisPage = new LinkedHashMap<>();

                  while (set.next()) {
                     thisPage.put(set.getString(1), Long.valueOf(set.getLong(2)));
                     if (++pageEntry == 10) {
                        pageEntry = 0;
                        XpLeaderboard.leaderboardPages.put(page, thisPage);
                        page++;
                        thisPage = new LinkedHashMap<>();
                     }
                  }
                  if (!thisPage.isEmpty()) {
                     XpLeaderboard.leaderboardPages.put(page, thisPage);
                  }
               } catch (SQLException var5) {
                  Debug.send("There was an error trying to load the xp leaderboard");
               }

               Debug.send("Reloaded XP Leaderboard");
            }
         }).runTaskTimerAsynchronously(Parcade.getParcade(), 20L, 72000L);
      }
   }

   public static void displayLeaderboard(Player player, int page) {
      Lang.send(player, "" + ChatColor.RED + ChatColor.BOLD + "Parcade XP Leaderboard " + ChatColor.GRAY + page + "/100");
      Lang.send(player, ChatColor.GRAY + "(Updates hourly)");
      int position = (page - 1) * 10 + 1;

      for (Entry<String, Long> pageEntry : leaderboardPages.getOrDefault(page, new LinkedHashMap<String, Long>()).entrySet()) {
         String pre;
         if (position == 10) {
            pre = "X";
         } else if (position == 100) {
            pre = "XX";
         } else if (position == 1000) {
            pre = "XXX";
         } else {
            pre = String.valueOf(position);
         }

         String entryName = pageEntry.getKey();
         String playerName = player.getName().equals(entryName) ? ChatColor.GREEN + entryName : ChatColor.DARK_AQUA + entryName;
         Lang.send(player, ChatColor.LIGHT_PURPLE + pre + ". " + ChatColor.YELLOW + pageEntry.getValue() + " " + playerName);
         position++;
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         return false;
      } else {
         Player player = (Player)sender;
         if (args.length == 0) {
            displayLeaderboard(player, 1);
            return true;
         } else {
            String page = args[0];
            if (!page.matches("-?(0|[1-9]\\d*)")) {
               Lang.send(player, ChatColor.RED + "Invalid input!");
               return false;
            } else {
               int pageNum;
               try {
                  pageNum = Integer.parseInt(page);
               } catch (Exception var9) {
                  pageNum = 1;
               }

               if (pageNum < 1) {
                  pageNum = 1;
               } else if (pageNum > 100) {
                  pageNum = 100;
               }

               displayLeaderboard(player, pageNum);
               return true;
            }
         }
      }
   }
}
