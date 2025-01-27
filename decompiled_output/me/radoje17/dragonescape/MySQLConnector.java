package me.radoje17.dragonescape;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MySQLConnector {
   private static File file;
   private static Connection connection;
   private static String host;
   private static int port;
   private static String username;
   private static String password;
   private static String database;

   public MySQLConnector() {
      file = new File(DragonEscape.getInstance().getDataFolder() + "/mysql.yml");
      if (!file.exists()) {
         DragonEscape.getInstance().saveResource("mysql.yml", false);
      }

      FileConfiguration data = YamlConfiguration.loadConfiguration(file);
      host = data.getString("mysql.hostname");
      port = data.getInt("mysql.port");
      username = data.getString("mysql.username");
      password = data.getString("mysql.password");
      database = data.getString("mysql.database");

      try {
         initializeConnection();
         DragonEscape.getInstance().getLogger().info("Connection to the MySQL database has been established successfully.");
         prepareStatement(
               "CREATE TABLE IF NOT EXISTS de_stats(uuid varchar(36), wins int DEFAULT 0, 2nd int DEFAULT 0, 3rd int DEFAULT 0, losses int DEFAULT 0, deaths int DEFAULT 0, soloGames INT DEFAULT 0, blocksBroken int DEFAULT 0, blocksPlaced int DEFAULT 0, PRIMARY KEY(uuid));"
            )
            .executeUpdate();
         prepareStatement(
               "CREATE TABLE IF NOT EXISTS de_times(uuid varchar(36), arenaName varchar(16), time bigint, kit varchar(16) DEFAULT \"leap\", date BIGINT DEFAULT -1, approved tinyint(4), disabled tinyint(4), disabledBy varchar(36), disabledDate bigint(20));"
            )
            .executeUpdate();
         prepareStatement(
               "CREATE TABLE IF NOT EXISTS de_times_solo(uuid varchar(36), arenaName varchar(16), time bigint, kit varchar(16) DEFAULT \"leap\", date BIGINT DEFAULT -1, approved tinyint(4), disabled tinyint(4), disabledBy varchar(36), disabledDate bigint(20));"
            )
            .executeUpdate();
         prepareStatement(
               "CREATE TABLE IF NOT EXISTS de_reports(reportID int NOT NULL PRIMARY KEY AUTO_INCREMENT, uuid varchar(36), runId int NOT NULL, date bigint(20), disabled tinyint(4), disabledBy varchar(36), disabledDate bigint(20));"
            )
            .executeUpdate();
         prepareStatement(
               "CREATE TABLE IF NOT EXISTS de_reports_solo(reportID int NOT NULL PRIMARY KEY AUTO_INCREMENT, uuid varchar(36), runId int NOT NULL, date bigint(20), disabled tinyint(4), disabledBy varchar(36), disabledDate bigint(20));"
            )
            .executeUpdate();
         Bukkit.getScheduler().runTaskTimerAsynchronously(DragonEscape.getInstance(), new Runnable() {
            @Override
            public void run() {
               try {
                  MySQLConnector.prepareStatement("SELECT 'a'").executeQuery().next();
               } catch (SQLException var2) {
                  var2.printStackTrace();
               }
            }
         }, 1200L, 1200L);
      } catch (SQLException var3) {
         DragonEscape.getInstance().getLogger().severe("Error occurred: " + var3.getLocalizedMessage());
         var3.printStackTrace();
      }
   }

   public static void closeConnetion() {
      try {
         connection.close();
      } catch (SQLException var1) {
         var1.printStackTrace();
      }
   }

   public static PreparedStatement prepareStatement(String query) throws SQLException {
      return connection.prepareStatement(query);
   }

   public static Connection getConnection() {
      return connection;
   }

   public static void initializeConnection() throws SQLException {
      FileConfiguration data = YamlConfiguration.loadConfiguration(file);
      if (!file.exists()) {
         DragonEscape.getInstance().saveResource("mysql.yml", false);
      }

      host = data.getString("mysql.hostname");
      port = data.getInt("mysql.port");
      username = data.getString("mysql.username");
      password = data.getString("mysql.password");
      database = data.getString("mysql.database");
      connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false", username, password);
   }

   public static boolean addTime(String uuid, String arenaName, long time, boolean solo, String kit) throws SQLException {
      long date = Calendar.getInstance().getTimeInMillis();
      long oldTime = getTime(uuid, arenaName, kit, solo);
      if (oldTime != -1L && oldTime <= time) {
         return false;
      } else {
         prepareStatement(
               "INSERT INTO de_times"
                  + (solo ? "_solo" : "")
                  + " (uuid, arenaName, time, kit, date) VALUES ('"
                  + uuid
                  + "', '"
                  + arenaName
                  + "', "
                  + time
                  + ", \""
                  + kit.toLowerCase()
                  + "\", "
                  + date
                  + ")"
            )
            .executeUpdate();
         return true;
      }
   }

   public static long getTime(String uuid, String arenaName, boolean solo) throws SQLException {
      ResultSet rs = prepareStatement(
            "SELECT time FROM de_times"
               + (solo ? "_solo" : "")
               + " WHERE disabled=0 AND uuid='"
               + uuid
               + "' AND arenaName='"
               + arenaName
               + "' ORDER BY time LIMIT 1"
         )
         .executeQuery();
      return rs.next() ? rs.getLong(1) : -1L;
   }

   public static long getTime(String uuid, String arenaName, String kitName, boolean solo) throws SQLException {
      ResultSet rs = prepareStatement(
            "SELECT time FROM de_times"
               + (solo ? "_solo" : "")
               + " WHERE disabled = 0 AND kit='"
               + kitName.toLowerCase()
               + "' AND uuid='"
               + uuid
               + "' AND arenaName='"
               + arenaName
               + "' ORDER BY time LIMIT 1"
         )
         .executeQuery();
      return rs.next() ? rs.getLong(1) : -1L;
   }

   public static void addStats(String uuid, int place, int deaths, int soloGamesPlayed, int blocksBroken, int blocksPlaced) throws SQLException {
      int firstPlaces = 0;
      int secondPlaces = 0;
      int thirdPlaces = 0;
      int losses = 0;
      if (place == 0) {
         firstPlaces = 1;
      } else if (place == 1) {
         secondPlaces = 1;
      } else if (place == 2) {
         thirdPlaces = 2;
      } else if (place >= 3) {
         losses = 1;
      }

      prepareStatement(
            "INSERT INTO de_stats(uuid, wins, 2nd, 3rd, losses, deaths, soloGames, blocksBroken, blocksPlaced) VALUES ('"
               + uuid
               + "', "
               + firstPlaces
               + ", "
               + secondPlaces
               + ", "
               + thirdPlaces
               + ", "
               + losses
               + ", "
               + deaths
               + ", "
               + soloGamesPlayed
               + ", "
               + blocksBroken
               + ", "
               + blocksPlaced
               + ") ON DUPLICATE KEY UPDATE wins = wins + "
               + firstPlaces
               + ", 2nd = 2nd+"
               + secondPlaces
               + ", 3rd= 3rd + "
               + thirdPlaces
               + ", losses = losses+"
               + losses
               + ", deaths = deaths+"
               + deaths
               + ", soloGames = soloGames+"
               + soloGamesPlayed
               + ", blocksBroken = blocksBroken+"
               + blocksBroken
               + ", blocksPlaced = blocksPlaced+"
               + blocksPlaced
               + ";"
         )
         .executeUpdate();
   }

   public static List<Integer> getReports(Player reporter, boolean solo) {
      List<Integer> reports = new ArrayList<>();

      try {
         ResultSet resultSet = prepareStatement(
               "SELECT runID FROM de_reports "
                  + (solo ? "_solo" : "")
                  + " WHERE uuid='"
                  + reporter.getUniqueId().toString()
                  + "' AND disabled = 0 ORDER BY runID;"
            )
            .executeQuery();

         while (resultSet.next()) {
            reports.add(resultSet.getInt(1));
         }
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

      return reports;
   }

   public static void approveRun(int runID, Player p, boolean solo) throws SQLException {
      prepareStatement(
            "UPDATE de_times"
               + (solo ? "_solo" : "")
               + " SET approved = 1, disabled = 0, disabledBy='"
               + p.getUniqueId().toString()
               + "', disabledDate = "
               + Calendar.getInstance().getTimeInMillis()
               + " WHERE runID = "
               + runID
         )
         .executeUpdate();
   }

   public static void disableRun(int runID, Player p, boolean solo) throws SQLException {
      prepareStatement(
            "UPDATE de_times"
               + (solo ? "_solo" : "")
               + " SET approved = 0, disabled = 1, disabledBy ='"
               + p.getUniqueId().toString()
               + "', disabledDate = "
               + Calendar.getInstance().getTimeInMillis()
               + " WHERE runID = "
               + runID
         )
         .executeUpdate();
   }

   public static int disableAllRuns(String uuid, Player p, boolean solo) throws SQLException {
      String table = solo ? "de_times_solo" : "de_times";
      return prepareStatement(
            "UPDATE "
               + table
               + " SET approved = 0, disabled = 1, disabledBy ='"
               + p.getUniqueId().toString()
               + "', disabledDate = "
               + Calendar.getInstance().getTimeInMillis()
               + " WHERE uuid = '"
               + uuid
               + "' AND disabled = 0"
         )
         .executeUpdate();
   }

   public static void reportRun(int runID, Player p, boolean solo) throws SQLException {
      prepareStatement(
            "INSERT INTO de_reports"
               + (solo ? "_solo" : "")
               + " (uuid, runID, date) VALUES ('"
               + p.getUniqueId().toString()
               + "', "
               + runID
               + ", "
               + Calendar.getInstance().getTimeInMillis()
               + ")"
         )
         .executeUpdate();
   }
}
