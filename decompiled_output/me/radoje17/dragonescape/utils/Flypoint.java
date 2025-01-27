package me.radoje17.dragonescape.utils;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.util.Vector;

public class Flypoint extends Vector {
   public Flypoint(double x, double y, double z) {
      super(x, y, z);
   }

   public Map<String, Object> serialize() {
      Map<String, Object> mapSerializer = new HashMap<>();
      mapSerializer.put("x", this.getX());
      mapSerializer.put("y", this.getY());
      mapSerializer.put("z", this.getZ());
      return mapSerializer;
   }

   public static Flypoint deserialize(Map<String, Object> serializedSpawnpoint) {
      double x = (Double)serializedSpawnpoint.get("x");
      double y = (Double)serializedSpawnpoint.get("y");
      double z = (Double)serializedSpawnpoint.get("z");
      return new Flypoint(x, y, z);
   }
}
