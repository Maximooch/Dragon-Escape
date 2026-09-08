package me.radoje17.dragonescape;

import com.sk89q.worldedit.WorldEditException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.radoje17.dragonescape.utils.ArenaUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class Arena {
   private HashMap<Location, BlockState> blocks;
   private final java.util.Map<Location, net.minecraft.server.v1_8_R3.NBTTagCompound> tileData = new HashMap<>();
   public final int COORDS;
   private String arenaName;
   private List<Location> spawnPoints;
   private List<Location> dragonPoints;
   private HashMap<Location, Boolean> liquids;
   private long arenaTime = 0L;
   private boolean waterKills;
   private boolean checkSolo;
   private boolean solo;
   private boolean ready = false;
   private boolean restoring;
   private boolean releaseAfterRestore;
   private boolean restoreFailed;
   private static final java.util.ArrayDeque<Arena> restores = new java.util.ArrayDeque<>();
   private static boolean restoreScheduled;
   private java.util.Iterator<BlockState> pendingBlocks;
   private java.util.Iterator<java.util.Map.Entry<Location, Boolean>> pendingLiquids;

   // One shared main-thread budget, not a full arena's block updates in one tick.
   private static void scheduleRestores() {
      if (restoreScheduled || restores.isEmpty()) return;
      restoreScheduled = true;
      Bukkit.getScheduler().runTask(DragonEscape.getInstance(), new Runnable() {
         @Override public void run() {
            restoreScheduled = false;
            long deadline = System.nanoTime() + 2000000L;
            int remaining = 256;
            while (!restores.isEmpty() && remaining-- > 0 && System.nanoTime() < deadline) {
               Arena arena = restores.peekFirst();
               try {
                  if (arena.pendingBlocks.hasNext()) {
                     arena.restoreBlock(arena.pendingBlocks.next());
                     arena.pendingBlocks.remove();
                  } else if (arena.pendingLiquids.hasNext()) {
                     java.util.Map.Entry<Location, Boolean> liquid = arena.pendingLiquids.next();
                     liquid.getKey().getBlock().setType(liquid.getValue() ? Material.STATIONARY_WATER : Material.STATIONARY_LAVA, false);
                     arena.pendingLiquids.remove();
                  } else {
                     restores.removeFirst();
                     arena.pendingBlocks = null;
                     arena.pendingLiquids = null;
                     arena.restoring = false;
                     if (arena.releaseAfterRestore) ArenaUtils.makeAvailable(arena);
                  }
               } catch (RuntimeException error) {
                  // Quarantine incomplete restores and fail waiting games explicitly.
                  arena.restoreFailed = true;
                  restores.removeFirst();
                  arena.pendingBlocks = null;
                  arena.pendingLiquids = null;
                  DragonEscape.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Arena restoration failed: " + arena.arenaName, error);
               }
            }
            scheduleRestores();
         }
      });
   }
   public static void clearRestores() {
      restores.clear();
      restoreScheduled = false;
   }

   public Arena(Location l) {
      this.COORDS = -1;
      this.spawnPoints = new ArrayList<>();
      this.spawnPoints.add(l);
      this.ready = true;
   }

   public Arena(Location l, long time) {
      this.COORDS = -1;
      this.spawnPoints = new ArrayList<>();
      this.spawnPoints.add(l);
      this.ready = true;
      this.arenaTime = time;
   }

   public Arena(String arenaName, boolean solo) throws WorldEditException, IOException {
      this.arenaName = arenaName;
      boolean hasSolo = ArenaUtils.hasSchematic(arenaName, true);
      boolean hasPublic = ArenaUtils.hasSchematic(arenaName, false);
      this.arenaTime = ArenaUtils.getArenaTime(arenaName);
      this.checkSolo = hasSolo == hasPublic;
      this.solo = solo;
      this.liquids = new HashMap<>();
      this.blocks = new HashMap<>();
      this.COORDS = ArenaUtils.pasteSchematic(ArenaUtils.getSchematicName(arenaName, solo));
      this.spawnPoints = ArenaUtils.getSpawnpoints(arenaName, this.COORDS);
      this.dragonPoints = ArenaUtils.getDragonpoints(arenaName, this.COORDS);
      this.waterKills = ArenaUtils.waterKills(arenaName);
      Chunk c = this.spawnPoints.get(0).getChunk();
      c.load();
   }

   public Arena restore() {
      return this.restore(true);
   }

   public Arena restore(boolean available) {
      if (this.COORDS == -1) {
         return this;
      } else {
         if (this.restoring) {
            if (available) this.releaseAfterRestore = true;
            return this;
         }
         if (available && (!this.blocks.isEmpty() || !this.liquids.isEmpty())) {
            this.restoring = true;
            this.releaseAfterRestore = true;
            ArenaUtils.makeUnavailable(this);
            this.pendingBlocks = this.blocks.values().iterator();
            this.pendingLiquids = this.liquids.entrySet().iterator();
            restores.addLast(this);
            scheduleRestores();
            return this;
         }
         for (Location location : this.blocks.keySet()) {
            this.restoreBlock(this.blocks.get(location));
         }

         for (Location l : this.liquids.keySet()) {
            if (this.liquids.get(l)) {
               l.getBlock().setType(Material.STATIONARY_WATER, false);
            } else {
               l.getBlock().setType(Material.STATIONARY_LAVA, false);
            }
         }

         this.blocks.clear();
         this.liquids.clear();
         if (available) {
            ArenaUtils.makeAvailable(this);
         }

         return this;
      }
   }

   private void restoreBlock(BlockState state) {
      if (!state.update(true, false)) throw new IllegalStateException("Block restore failed: " + state.getLocation());
      net.minecraft.server.v1_8_R3.NBTTagCompound data = this.tileData.remove(state.getLocation());
      if (data != null) {
         net.minecraft.server.v1_8_R3.World world = ((org.bukkit.craftbukkit.v1_8_R3.CraftWorld)state.getWorld()).getHandle();
         net.minecraft.server.v1_8_R3.BlockPosition position = new net.minecraft.server.v1_8_R3.BlockPosition(state.getX(), state.getY(), state.getZ());
         net.minecraft.server.v1_8_R3.TileEntity tile = world.getTileEntity(position);
         if (tile == null) throw new IllegalStateException("Tile entity missing during restoration: " + state.getLocation());
         tile.a(data);
         tile.update();
         world.notify(position);
      }
   }

   public void addBlockState(BlockState state) {
      Location location = state.getLocation();
      if (!this.blocks.containsKey(location)) {
         this.blocks.put(location, state);
         net.minecraft.server.v1_8_R3.TileEntity tile = ((org.bukkit.craftbukkit.v1_8_R3.CraftWorld)state.getWorld()).getHandle()
            .getTileEntity(new net.minecraft.server.v1_8_R3.BlockPosition(state.getX(), state.getY(), state.getZ()));
         if (tile != null) {
            net.minecraft.server.v1_8_R3.NBTTagCompound data = new net.minecraft.server.v1_8_R3.NBTTagCompound();
            tile.b(data);
            this.tileData.put(state.getLocation(), data);
         }
         if (state.getBlock().getType() == Material.LAVA || state.getBlock().getType() == Material.STATIONARY_LAVA) {
            this.liquids.put(state.getLocation(), false);
         } else if (state.getBlock().getType() == Material.WATER || state.getBlock().getType() == Material.STATIONARY_WATER) {
            this.liquids.put(state.getLocation(), true);
         }
      }
   }

   public void addBlockState(Block b) {
      if (!this.blocks.containsKey(b.getLocation())) this.addBlockState(b.getState());
   }

   public void addBlockState(Location l) {
      this.addBlockState(l.getBlock());
   }

   public String getArenaName() {
      return this.arenaName;
   }

   public List<Location> getSpawnpoints() {
      return this.spawnPoints;
   }

   public List<Location> getDragonPoints() {
      return this.dragonPoints;
   }

   public boolean isWaterKills() {
      return this.waterKills;
   }

   public boolean isReady() {
      return !this.restoreFailed && !this.restoring && (this.ready || ArenaUtils.isPasteComplete(this.COORDS));
   }

   public boolean hasLoadFailed() {
      return this.restoreFailed || (this.COORDS != -1 && ArenaUtils.hasPasteFailed(this.COORDS));
   }

   public boolean canReserveRestore() {
      return !this.restoreFailed && this.restoring && this.releaseAfterRestore && this.pendingBlocks != null;
   }
   public void reserveRestore() {
      if (!canReserveRestore()) throw new IllegalStateException("Arena is not available for reservation");
      this.releaseAfterRestore = false;
   }

   public boolean isSolo() {
      return this.solo;
   }

   public boolean isCheckSolo() {
      return this.checkSolo;
   }

   public long getArenaTime() {
      return this.arenaTime;
   }
}
