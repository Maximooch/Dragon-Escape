package me.radoje17.dragonescape;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Lang {
   private static File f = new File(DragonEscape.getInstance().getDataFolder(), "lang.yml");
   private static FileConfiguration lang;

   public static void reloadConfig() {
      if (!f.exists()) {
         DragonEscape.getInstance().saveResource("lang.yml", false);
      }

      lang = YamlConfiguration.loadConfiguration(f);
   }

   public static void reloadConfig(boolean overwrite) {
      if (overwrite) {
         if (f.exists()) {
            f.delete();
         }

         DragonEscape.getInstance().saveResource("lang.yml", false);
      }

      lang = YamlConfiguration.loadConfiguration(f);
   }

   public static void overwrite() {
      if (f.exists()) {
         f.delete();
      }

      DragonEscape.getInstance().saveResource("lang.yml", false);
      lang = YamlConfiguration.loadConfiguration(f);
   }

   public static String getMessage(String message) {
      String s = lang.getString(message);
      return s == null
         ? ChatColor.RED + "Message " + message + " could not be found."
         : ChatColor.translateAlternateColorCodes('&', s.replaceAll("%prefix%", lang.getString("prefix")));
   }

   public static List<String> getList(String message) {
      List<String> list = new ArrayList<>();
      if (lang.getStringList(message) != null) {
         lang.getStringList(message).forEach(s -> list.add(ChatColor.translateAlternateColorCodes('&', s.replaceAll("%prefix%", lang.getString("prefix")))));
      } else {
         list.add(ChatColor.RED + "List " + message + " could not be found.");
      }

      return list;
   }

   static {
      reloadConfig();
   }
}
