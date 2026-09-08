package me.radoje17.dragonescape;

import com.sk89q.worldedit.WorldEditException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import me.radoje17.dragonescape.commands.DragonEscapeAdminCommand;
import me.radoje17.dragonescape.commands.DragonEscapeCommand;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.listeners.InventoryListener;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.LocationUtils;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public final class DragonEscape extends JavaPlugin {
   private static DragonEscape instance;
   private GameManager gameManager;
   private static File file;
   private static FileConfiguration data;
   private static boolean cubicsEnabled = false;

   public void onEnable() {
      instance = this;
      file = new File(this.getDataFolder() + "/config.yml");
      if (!file.exists()) {
         getInstance().saveResource("config.yml", false);
      }

      data = YamlConfiguration.loadConfiguration(file);
      instance = this;
      this.gameManager = new GameManager();
      DragonEscapeAdminCommand adminCommand = new DragonEscapeAdminCommand();
      Bukkit.getPluginManager().registerEvents(this.gameManager, this);
      Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
      Bukkit.getPluginManager().registerEvents(adminCommand, this);
      new ArenaUtils();
      DragonEscapeCommand command = new DragonEscapeCommand();
      this.getCommand("dragonescape").setExecutor(command);
      this.getCommand("dragonescape").setTabCompleter(command);
      this.getCommand("dragonescapeadmin").setExecutor(adminCommand);
      Bukkit.getPluginManager().registerEvents(new KitManager(), this);
      new MySQLConnector();
      cubicsEnabled = Bukkit.getPluginManager().getPlugin("Cubics") != null;
      this.gameManager.createPublicLobby();
      Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
         @Override
         public void run() {
            try {
               Leaderboard.update();
            } catch (SQLException var2) {
               var2.printStackTrace();
            }
         }
      }, 0L, 12000L);

      // Arenas are created on demand and registered in ArenaUtils for reuse.
   }

   public void onDisable() {
      ArenaUtils.stopPastes();
      Arena.clearRestores();
      Leaderboard.remove();
      PlayerCountHologram.remove();
      DragonEscapeAdminCommand.removeAllConfigurings();

      try {
         World world = Bukkit.getWorld("dragonescape");
         Location spawn = getGlobalLobby();

         for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(spawn);
            p.setGameMode(GameMode.ADVENTURE);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
         }

         for (Player p : this.gameManager.getPlayers().keySet()) {
            this.gameManager.restoreInventory(p);
         }

         Bukkit.unloadWorld(world, false);
         File f = new File(Bukkit.getWorldContainer() + "/dragonescape");
         if (f.exists() && f.isDirectory()) {
            FileUtils.deleteDirectory(f);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      for (Player p : Bukkit.getOnlinePlayers()) {
         p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
      }
   }

   public static DragonEscape getInstance() {
      return instance;
   }

   public GameManager getGameManager() {
      return this.gameManager;
   }

   public static Location getLobby() {
      String s = data.getString("lobby");
      return s == null ? ((World)Bukkit.getWorlds().get(0)).getSpawnLocation() : LocationUtils.stringToLocationWorld(s);
   }

   public static void setLobby(Location l) {
      data.set("lobby", LocationUtils.locationToStringWorld(l));

      try {
         data.save(file);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }

   public static void setGlobalLobby(Location l) {
      data.set("global-lobby", LocationUtils.locationToStringWorld(l));

      try {
         data.save(file);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }

   public static Location getGlobalLobby() {
      String s = data.getString("global-lobby");
      return s == null ? ((World)Bukkit.getWorlds().get(0)).getSpawnLocation() : LocationUtils.stringToLocationWorld(data.getString("global-lobby"));
   }

   public static FileConfiguration getConfiguration() {
      return data;
   }

   public static boolean isCubicsEnabled() {
      return cubicsEnabled;
   }

   public static void sendActionBar(Player p, String message) {
      IChatBaseComponent chatBaseComponent = ChatSerializer.a("{\"text\": \"" + message + "\"}");
      PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(chatBaseComponent, (byte)2);
      ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packetPlayOutChat);
   }

   public static void sendTimer(Player p, String message) {
      IChatBaseComponent chatBaseComponent = ChatSerializer.a("{\"text\": \"" + message + "\"}");
      PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(chatBaseComponent, (byte)2);
      ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packetPlayOutChat);
   }

   public static void sendTitle(Player p, String message, ChatColor color) {
      IChatBaseComponent chatTitle = ChatSerializer.a("{\"text\": \"" + message + "\",color:" + color.name().toLowerCase() + "}");
      PacketPlayOutTitle title = new PacketPlayOutTitle(EnumTitleAction.TITLE, chatTitle);
      PacketPlayOutTitle length = new PacketPlayOutTitle(5, 20, 0);
      ((CraftPlayer)p).getHandle().playerConnection.sendPacket(title);
      ((CraftPlayer)p).getHandle().playerConnection.sendPacket(length);
   }

   public void saveConfig() {
      try {
         data.save(file);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }
}
