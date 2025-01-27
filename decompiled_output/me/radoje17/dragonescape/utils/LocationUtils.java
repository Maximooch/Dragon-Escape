package me.radoje17.dragonescape.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationUtils {
   public static String locationToStringNoFace(Location l) {
      return l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
   }

   public static String locationToString(Location l) {
      return (double)l.getBlockX() + 0.5 + ";" + l.getY() + ";" + ((double)l.getBlockZ() + 0.5) + ";" + l.getYaw() + ";" + l.getPitch();
   }

   public static Location stringToLocation(String s) {
      String[] list = s.split(";");
      return list.length <= 3
         ? new Location(ArenaUtils.getWorld(), Double.parseDouble(list[0]), Double.parseDouble(list[1]), Double.parseDouble(list[2]))
         : new Location(
            ArenaUtils.getWorld(),
            Double.parseDouble(list[0]),
            Double.parseDouble(list[1]),
            Double.parseDouble(list[2]),
            Float.parseFloat(list[3]),
            Float.parseFloat(list[4])
         );
   }

   public static Location stringToLocation(String s, int add) {
      String[] list = s.split(";");
      return list.length <= 3
         ? new Location(
            ArenaUtils.getWorld(), Double.parseDouble(list[0]) + (double)add, Double.parseDouble(list[1]), Double.parseDouble(list[2]) + (double)add
         )
         : new Location(
            ArenaUtils.getWorld(),
            Double.parseDouble(list[0]) + (double)add,
            Double.parseDouble(list[1]),
            Double.parseDouble(list[2]) + (double)add,
            Float.parseFloat(list[3]),
            Float.parseFloat(list[4])
         );
   }

   public static String locationToStringWorld(Location l) {
      return l.getWorld().getName()
         + ";"
         + ((double)l.getBlockX() + 0.5)
         + ";"
         + l.getY()
         + ";"
         + ((double)l.getBlockZ() + 0.5)
         + ";"
         + l.getYaw()
         + ";"
         + l.getPitch();
   }

   public static String locationToStringWorldNoFace(Location l) {
      return l.getWorld().getName() + ";" + ((double)l.getBlockX() + 0.5) + ";" + l.getY() + ";" + ((double)l.getBlockZ() + 0.5);
   }

   public static Location stringToLocationWorld(String s) {
      String[] list = s.split(";");
      return list.length <= 4
         ? new Location(Bukkit.getWorld(list[0]), Double.parseDouble(list[1]), Double.parseDouble(list[2]), Double.parseDouble(list[3]))
         : new Location(
            Bukkit.getWorld(list[0]),
            Double.parseDouble(list[1]),
            Double.parseDouble(list[2]),
            Double.parseDouble(list[3]),
            Float.parseFloat(list[4]),
            Float.parseFloat(list[5])
         );
   }
}
