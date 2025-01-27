package me.radoje17.dragonescape.geometry;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class Cuboid {
   public Vector highBound;
   public Vector lowBound;

   public Cuboid(Vector alfa, Vector beta) {
      double lowX;
      double highX;
      if (alfa.getX() >= beta.getX()) {
         highX = alfa.getX();
         lowX = beta.getX();
      } else {
         highX = beta.getX();
         lowX = alfa.getX();
      }

      double lowY;
      double highY;
      if (alfa.getY() >= beta.getY()) {
         highY = alfa.getY();
         lowY = beta.getY();
      } else {
         highY = beta.getY();
         lowY = alfa.getY();
      }

      double lowZ;
      double highZ;
      if (alfa.getZ() >= beta.getZ()) {
         highZ = alfa.getZ();
         lowZ = beta.getZ();
      } else {
         highZ = beta.getZ();
         lowZ = alfa.getZ();
      }

      this.highBound = new Vector(highX, highY, highZ);
      this.lowBound = new Vector(lowX, lowY, lowZ);
   }

   public List<Block> getVolume(World world) {
      List<Block> list = new ArrayList<>();
      double xdiff = this.highBound.getX() - this.lowBound.getX();
      double ydiff = this.highBound.getY() - this.lowBound.getY();
      double zdiff = this.highBound.getZ() - this.lowBound.getZ();

      for (int xx = 0; (double)xx <= xdiff; xx++) {
         for (int yy = 0; (double)yy <= ydiff; yy++) {
            for (int zz = 0; (double)zz <= zdiff; zz++) {
               Location block = new Location(world, this.lowBound.getX() + (double)xx, this.lowBound.getY() + (double)yy, this.lowBound.getZ() + (double)zz);
               list.add(block.getBlock());
            }
         }
      }

      return list;
   }

   public boolean isPointInsideCuboid(Location point) {
      return point.getX() <= this.highBound.getX()
         && point.getX() >= this.lowBound.getX()
         && point.getY() <= this.highBound.getY()
         && point.getY() >= this.lowBound.getY()
         && point.getZ() <= this.highBound.getZ()
         && point.getZ() >= this.lowBound.getZ();
   }
}
