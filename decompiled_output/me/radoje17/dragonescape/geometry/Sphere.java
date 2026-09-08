package me.radoje17.dragonescape.geometry;

import java.util.ArrayList;
import java.util.List;
import me.radoje17.dragonescape.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Sphere {
   private World world;
   private int x, y, z;
   private final int[][] offsets;
   private final Arena arena;

   public Sphere(int radius, World world, Arena arena) {
      this.world = world;
      this.arena = arena;
      List<int[]> points = new ArrayList<>();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (dx * dx + dy * dy + dz * dz <= radius * radius) points.add(new int[]{dx, dy, dz});
            }
         }
      }
      offsets = points.toArray(new int[points.size()][]);
   }
   public Sphere at(Location location) {
      world = location.getWorld();
      x = location.getBlockX(); y = location.getBlockY(); z = location.getBlockZ();
      return this;
   }
   public void set(Material material) {
      for (int[] offset : offsets) {
         int targetY = y + offset[1];
         if (targetY < 0 || targetY >= world.getMaxHeight()) continue;
         Block block = world.getBlockAt(x + offset[0], targetY, z + offset[2]);
         if (block.getType() != material) {
            arena.addBlockState(block);
            block.setType(material, false);
         }
      }
   }
}
