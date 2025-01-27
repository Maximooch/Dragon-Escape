package me.radoje17.dragonescape;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ArenaStats {
   private String p;
   private String uuid = null;
   private String arenaName;
   int joins = 0;
   int finishes = 0;
   int deaths = 0;
   int leaves = 0;
   int resets = 0;
   int playTime = 0;

   public ArenaStats(String player, String arenaName) throws SQLException {
      this.p = player;
      this.arenaName = arenaName.toLowerCase();
      this.load();
      System.out.println("wohoo " + player);
   }

   public void load() throws SQLException {
      ResultSet set = MySQLConnector.prepareStatement(
            " SELECT player_uuid, joins, finishes, deaths, leaves, resets, playTime FROM pc_players INNER JOIN de_arenaStats ON player_uuid = uuid WHERE player_name='"
               + this.p
               + "' AND arenaName='"
               + this.arenaName
               + "'"
         )
         .executeQuery();
      if (!set.next()) {
         set = MySQLConnector.prepareStatement("SELECT player_uuid FROM pc_players WHERE player_name='" + this.p + "'").executeQuery();
         if (set.next()) {
            this.uuid = set.getString(1);
         }
      } else {
         this.uuid = set.getString(1);
         this.joins = set.getInt(2);
         this.finishes = set.getInt(3);
         this.deaths = set.getInt(4);
         this.leaves = set.getInt(5);
         this.resets = set.getInt(6);
         this.playTime = set.getInt(7);
      }
   }

   public boolean everythingNull() {
      return this.finishes == 0 && this.deaths == 0 && this.leaves == 0 && this.resets == 0 && this.playTime == 0 && this.joins == 0;
   }

   public void save() throws SQLException {
      if (this.uuid != null) {
         MySQLConnector.prepareStatement(
               "INSERT INTO de_arenaStats (uuid, arenaName, joins, finishes, deaths, leaves, resets, playTime) VALUES ('"
                  + this.uuid
                  + "', '"
                  + this.arenaName.toLowerCase()
                  + "', "
                  + this.joins
                  + ", "
                  + this.finishes
                  + ", "
                  + this.deaths
                  + ", "
                  + this.leaves
                  + ", "
                  + this.resets
                  + ", "
                  + this.playTime
                  + ")ON DUPLICATE KEY UPDATE joins = "
                  + this.joins
                  + ", finishes = "
                  + this.finishes
                  + ", deaths = "
                  + this.deaths
                  + ", leaves = "
                  + this.leaves
                  + ", resets = "
                  + this.resets
                  + ", playTime = "
                  + this.playTime
                  + ";"
            )
            .executeUpdate();
      }
   }

   public void addStats(int joins, int finishes, int deaths, int resets, int playTime) {
      this.joins = joins;
      this.finishes += finishes;
      this.deaths += deaths;
      this.resets += resets;
      this.playTime += playTime;
   }

   public void addFinish() {
      this.finishes++;
   }

   public void addLeave() {
      this.leaves++;
   }

   public void addJoin() {
      System.out.println("added join");
      this.joins++;
      System.out.println("joins1: " + this.joins);
   }

   public void addDeath() {
      this.deaths++;
   }

   public void addDeaths(int count) {
      this.deaths += count;
   }

   public void addReset() {
      this.resets++;
   }

   public void addPlayTime(int playTime) {
      this.playTime += playTime;
   }

   public int getFinishes() {
      return this.finishes;
   }

   public int getJoins() {
      System.out.println("joins: " + this.joins);
      return this.joins;
   }

   public int getLeaves() {
      return this.leaves;
   }

   public int getDeaths() {
      return this.deaths;
   }

   public int getReset() {
      return this.resets;
   }

   public String getPlayTime() {
      int days = this.playTime / 86400;
      this.playTime %= 86400;
      int minutes = this.playTime / 60;
      int seconds = this.playTime % 60;
      StringBuilder result = new StringBuilder();
      if (days > 0) {
         if (days == 1) {
            result.append("1 day ");
         } else {
            result.append(days + " days ");
         }
      }

      if (minutes > 0) {
         if (minutes == 1) {
            result.append("1 minute ");
         } else {
            result.append(minutes + " minutes ");
         }
      }

      if (seconds > 0) {
         if (seconds == 1) {
            result.append("1 second ");
         } else {
            result.append(seconds + " seconds ");
         }
      } else if (days == 0 && minutes == 0) {
         return "0 seconds";
      }

      return result.toString();
   }
}
