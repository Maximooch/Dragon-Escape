package me.radoje17.dragonescape.dragon;

import java.util.ArrayList;
import java.util.List;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import me.radoje17.dragonescape.geometry.Sphere;
import me.radoje17.dragonescape.utils.ArenaUtils;
import me.radoje17.dragonescape.utils.Flypoint;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityEnderDragon;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class Dragon {
   public boolean destroyBlocks = true;
   public String name;
   public int currentPathPoint = 0;
   public double speed;
   public double distanceToPrevFlypoint;
   public Flypoint spawnpoint;
   Game g;
   public List<Flypoint> flypoint;
   private Entity bukkitArmorStand;
   private CraftArmorStand craftArmorStand;
   private EntityArmorStand realArmorStand;
   private CraftEnderDragon craftEnderDragon;
   private EntityEnderDragon realEnderDragon;
   public boolean isFlying = false;
   Location lastlocation;
   private Sphere destructionRadius;
   private double killRadius;

   public void removeDragon() {
      this.craftEnderDragon.remove();
      this.craftArmorStand.remove();
   }

   public Dragon(Game g, double speed) {
      this.g = g;
      this.speed = speed;
      this.flypoint = new ArrayList<>();
      boolean first = true;

      Location l;
      while ((l = g.getNextDragonpoint()) != null) {
         if (first) {
            this.spawnpoint = new Flypoint(l.getX(), l.getY(), l.getZ());
            first = false;
         } else {
            this.flypoint.add(new Flypoint(l.getX(), l.getY(), l.getZ()));
         }
      }

      Location spawnpointLoc = this.spawnpoint.toLocation(ArenaUtils.getWorld());
      ArenaUtils.getWorld().loadChunk(spawnpointLoc.getChunk());
      this.bukkitArmorStand = ArenaUtils.getWorld().spawnEntity(spawnpointLoc, EntityType.ARMOR_STAND);
      this.craftArmorStand = (CraftArmorStand)this.bukkitArmorStand;
      this.realArmorStand = this.craftArmorStand.getHandle();
      this.craftEnderDragon = (CraftEnderDragon)ArenaUtils.getWorld().spawnEntity(spawnpointLoc, EntityType.ENDER_DRAGON);
      this.realEnderDragon = this.craftEnderDragon.getHandle();
      this.craftEnderDragon.setCustomName(Lang.getMessage("dragon-name"));
      this.craftArmorStand.setGravity(false);
      this.craftArmorStand.setSmall(true);
      this.craftArmorStand.setVisible(false);
      this.craftArmorStand.setPassenger(this.craftEnderDragon);
      this.destructionRadius = new Sphere(DragonEscape.getConfiguration().getInt("dragon-destroy-radius"), ArenaUtils.getWorld(), g.getArena());
      this.killRadius = DragonEscape.getConfiguration().getDouble("dragon-kill-radius");
   }

   public Vector getNextPosition(Vector curr, double intensity) {
      if (this.currentPathPoint + 1 <= this.flypoint.size() - 1) {
         Vector delta = this.flypoint.get(this.currentPathPoint + 1).clone().subtract(curr);
         double diff = delta.length() - intensity;
         if (diff < 0.0) {
            this.currentPathPoint++;
            if (this.currentPathPoint == this.flypoint.size()) {
               return null;
            } else {
               Vector potentialNext = this.getNextPosition(this.flypoint.get(this.currentPathPoint), -diff);
               return potentialNext == null ? this.flypoint.get(this.currentPathPoint) : potentialNext;
            }
         } else {
            return curr.clone().add(delta.normalize().multiply(intensity));
         }
      } else {
         return null;
      }
   }

   public void startFlying() {
      this.isFlying = true;
   }

   public void stopFlying() {
      this.isFlying = false;
   }

   public Location getAngles(Vector curr, Vector next) {
      Location angles = new Location(null, 0.0, 0.0, 0.0);
      angles.setDirection(next.clone().subtract(curr).multiply(-1));
      return angles;
   }

   public void tick() {
      if (this.isFlying) {
         this.destructionRadius.at(this.getDragonPosition());
         this.destructionRadius.set(Material.AIR);
         Vector next = this.getNextPosition(this.getCurrentPosition().toVector(), this.speed);
         if (next == null) {
            this.isFlying = false;
            if (this.lastlocation != null) {
               this.setPosition(this.lastlocation);
               this.setRotation(this.lastlocation.getYaw(), this.lastlocation.getPitch());
            }

            this.g.endGame();
         } else {
            Location angles = this.getAngles(this.getCurrentPosition().toVector(), next);
            this.setPosition(next);
            this.setRotation(angles.getYaw(), angles.getPitch());
            this.lastlocation = this.craftArmorStand.getLocation();
         }

         List<Player> toDie = new ArrayList<>();

         for (Entity e : this.realArmorStand.getBukkitEntity().getNearbyEntities(this.killRadius, this.killRadius, this.killRadius)) {
            if (e instanceof Player) {
               toDie.add((Player)e);
            }
         }

         for (Player p : toDie) {
            this.g.die(p);
         }
      } else if (this.lastlocation != null) {
         this.setPosition(this.lastlocation);
         this.setRotation(this.lastlocation.getYaw(), this.lastlocation.getPitch());
      } else {
         if (this.flypoint.size() < 2) {
            return;
         }

         Flypoint f = this.flypoint.get(1);
         if (f != null) {
            Location angles = this.getAngles(this.spawnpoint, f);
            this.setPosition(this.spawnpoint);
            this.setRotation(angles.getYaw(), angles.getPitch());
         }
      }

      this.distanceToPrevFlypoint = this.getCurrentPosition().distance(this.flypoint.get(this.currentPathPoint).toLocation(ArenaUtils.getWorld()));
   }

   public void setPosition(Vector location) {
      this.realArmorStand.setPosition(location.getX(), location.getY(), location.getZ());
   }

   public void setPosition(Location location) {
      this.setPosition(location.toVector());
   }

   public void setRotation(float yaw, float pitch) {
      this.realEnderDragon.yaw = yaw;
      this.realEnderDragon.pitch = pitch;
   }

   public Location getCurrentPosition() {
      return this.craftArmorStand.getLocation();
   }

   public Location getDragonPosition() {
      return this.craftEnderDragon.getLocation();
   }

   public void handleEvent(Event event) {
   }

   public void deleteRefs() {
      this.destructionRadius = null;
      this.bukkitArmorStand = null;
      this.craftArmorStand = null;
      this.realArmorStand = null;
      this.craftEnderDragon = null;
      this.realEnderDragon = null;
   }
}
