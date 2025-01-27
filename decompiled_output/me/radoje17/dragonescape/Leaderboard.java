package me.radoje17.dragonescape;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import me.radoje17.dragonescape.utils.LocationUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

public class Leaderboard {
   private static Location l;
   private static List<ArmorStand> holograms = new ArrayList<>();

   public static void setLocation(Location l) throws SQLException {
      Leaderboard.l = l;
      update();
   }

   public static void update() throws SQLException {
      remove();
      int counter = 0;
      ResultSet set = MySQLConnector.prepareStatement(
            "select player_name, wins from de_stats INNER JOIN pc_players ON player_uuid = uuid ORDER BY wins DESC LIMIT 10;"
         )
         .executeQuery();
      Location l = Leaderboard.l.clone();

      while (set.next()) {
         counter++;
         l = l.subtract(0.0, 0.3, 0.0);
         setArmorStand("" + ChatColor.AQUA + counter + ". " + ChatColor.GREEN + set.getString(1) + ChatColor.YELLOW + " " + set.getInt(2), l);
      }

      l = l.subtract(0.0, 0.3, 0.0);
      setArmorStand("" + ChatColor.GRAY + ChatColor.ITALIC + "This list updates every 10 minutes", l);
      setArmorStand("" + ChatColor.RED + ChatColor.BOLD + "Top " + counter + " Wins", Leaderboard.l);
   }

   public static void remove() {
      for (ArmorStand s : holograms) {
         s.remove();
      }

      holograms.clear();
   }

   private static void setArmorStand(String text, Location l) {
      ArmorStand hologram = (ArmorStand)l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
      hologram.setGravity(false);
      hologram.setCanPickupItems(false);
      hologram.setCustomNameVisible(true);
      hologram.setVisible(false);
      hologram.setCustomName(text);
      holograms.add(hologram);
   }

   static {
      String location = DragonEscape.getConfiguration().getString("leaderboard-location");
      if (location != null) {
         l = LocationUtils.stringToLocationWorld(location);
      }
   }
}
