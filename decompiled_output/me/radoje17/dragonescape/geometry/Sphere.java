package me.radoje17.dragonescape.geometry;

import java.util.ArrayList;
import java.util.List;
import me.radoje17.dragonescape.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class Sphere {
   private World world;
   private int x = 0;
   private int y = 0;
   private int z = 0;
   private List<Block> cubicSet;
   private List<Block> sphericalSet = new ArrayList<>();
   private int radius;
   private Arena arena;

   public Sphere(int radius, World world, Arena arena) {
      this.world = world;
      this.radius = radius;
      this.arena = arena;
      Cuboid region = new Cuboid(new Vector(radius, radius, radius), new Vector(-radius, -radius, -radius));
      this.cubicSet = region.getVolume(this.world);

      for (Block block : this.cubicSet) {
         int a = block.getX() * block.getX();
         int b = block.getY() * block.getY();
         int c = block.getZ() * block.getZ();
         if (a + b + c <= this.radius * this.radius) {
            this.sphericalSet.add(block);
         }
      }
   }

   public Sphere at(Location location) {
      this.world = location.getWorld();
      this.x = (int)Math.floor(location.getX());
      this.y = (int)Math.floor(location.getY());
      this.z = (int)Math.floor(location.getZ());
      return this;
   }

   public void set(Material mat) {
      for (Block block : this.sphericalSet) {
         Block b = this.world.getBlockAt(this.x + block.getX(), this.y + block.getY(), this.z + block.getZ());
         if (b.getType() != mat) {
            this.arena.addBlockState(b);
            b.setType(mat);
         }
      }
   }
}
