package me.radoje17.dragonescape;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.entity.Player;

public class DeSettings {
   private Player p;
   private boolean joinMessages;
   private boolean rewardMessages;
   private boolean resetMessages;
   private boolean displayTimer;
   private boolean playerVisibility;
   private boolean soundEffects;
   private boolean safetyMode;
   private boolean autoRejoin;

   public DeSettings(Player p) throws SQLException {
      this.p = p;
      this.update();
   }

   public Player getPlayer() {
      return this.p;
   }

   public String getPlayerName() {
      return this.p.getName();
   }

   public void update() throws SQLException {
      ResultSet set = MySQLConnector.prepareStatement(
            "SELECT joinMessages, rewardMessages, resetMessages, displayTimer, playerVisibility, soundEffects, safetyMode, autoRejoin FROM de_stats WHERE uuid='"
               + this.p.getUniqueId().toString()
               + "';"
         )
         .executeQuery();
      if (set.next()) {
         this.joinMessages = set.getBoolean(1);
         this.rewardMessages = set.getBoolean(2);
         this.resetMessages = set.getBoolean(3);
         this.displayTimer = set.getBoolean(4);
         this.playerVisibility = set.getBoolean(5);
         this.soundEffects = set.getBoolean(6);
         this.safetyMode = set.getBoolean(7);
         this.autoRejoin = set.getBoolean(8);
      }
   }

   public void update(
      boolean joinMessages,
      boolean rewardMessages,
      boolean resetMessages,
      boolean displayTimer,
      boolean playerVisibility,
      boolean soundEffects,
      boolean safetyMode,
      boolean autoRejoin
   ) throws SQLException {
      this.joinMessages = joinMessages;
      this.rewardMessages = rewardMessages;
      this.resetMessages = resetMessages;
      this.displayTimer = displayTimer;
      this.playerVisibility = playerVisibility;
      this.soundEffects = soundEffects;
      this.safetyMode = safetyMode;
      this.autoRejoin = autoRejoin;
      Game g;
      if ((g = DragonEscape.getInstance().getGameManager().getGame(this.p)) != null && g.isSolo()) {
         if (playerVisibility) {
            g.showSolo();
         } else {
            g.hideAllPlayers(this.p);
         }
      }

      MySQLConnector.prepareStatement(
            "UPDATE de_stats SET joinMessages="
               + joinMessages
               + ", rewardMessages="
               + rewardMessages
               + ", resetMessages="
               + resetMessages
               + ", displayTimer="
               + displayTimer
               + ", playerVisibility="
               + playerVisibility
               + ", soundEffects="
               + soundEffects
               + ", safetyMode="
               + safetyMode
               + ", autoRejoin="
               + autoRejoin
               + " WHERE uuid='"
               + this.p.getUniqueId().toString()
               + "'"
         )
         .executeUpdate();
   }

   public boolean sendJoinMessages() {
      return this.joinMessages;
   }

   public boolean sendRewardMessages() {
      return this.rewardMessages;
   }

   public boolean sendResetMessages() {
      return this.resetMessages;
   }

   public boolean displayTimer() {
      return this.displayTimer;
   }

   public boolean showPlayers() {
      return this.playerVisibility;
   }

   public boolean playSoundEffects() {
      return this.soundEffects;
   }

   public boolean autoRejoin() {
      return this.autoRejoin;
   }

   public boolean safetyMode() {
      return this.safetyMode;
   }
}
