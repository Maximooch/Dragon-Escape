package me.radoje17.dragonescape;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.radoje17.dragonescape.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

public class PlayerCountHologram {
   private static Location l;
   private static List<ArmorStand> holograms = new ArrayList<>();

   public static void setLocation(Location l) throws SQLException {
      PlayerCountHologram.l = l;
      update();
   }

   public static void update() {
      if (PlayerCountHologram.l != null) {
         remove();
         Iterator<String> list = Lang.getList("player-count-hologram").iterator();
         Location l = PlayerCountHologram.l.clone();
         int playerCount = DragonEscape.getInstance().getGameManager().getPlayerCount();

         for (String player = playerCount == 1 ? "player" : "players"; list.hasNext(); l = l.subtract(0.0, 0.3, 0.0)) {
            setArmorStand(list.next().replaceAll("%amount%", "" + playerCount).replaceAll("%player%", player), l);
         }
      }
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
      String location = DragonEscape.getConfiguration().getString("player-count-location");
      if (location != null) {
         l = LocationUtils.stringToLocationWorld(location);
      }
   }
}
