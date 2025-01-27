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
   private HashMap<String, BlockState> blocks;
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
      Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
            Arena.this.ready = true;
         }
      }, 300L);
   }

   public Arena restore() {
      return this.restore(true);
   }

   public Arena restore(boolean available) {
      if (this.COORDS == -1) {
         return this;
      } else {
         for (String location : this.blocks.keySet()) {
            this.blocks.get(location).update(true);
         }

         for (Location l : this.liquids.keySet()) {
            if (this.liquids.get(l)) {
               l.getBlock().setType(Material.STATIONARY_WATER);
            } else {
               l.getBlock().setType(Material.STATIONARY_LAVA);
            }
         }

         this.blocks.clear();
         if (available) {
            ArenaUtils.makeAvailable(this);
         }

         return this;
      }
   }

   public void addBlockState(BlockState state) {
      String location = state.getLocation().toString();
      if (!this.blocks.containsKey(location)) {
         this.blocks.put(location, state);
         if (state.getBlock().getType() == Material.LAVA || state.getBlock().getType() == Material.STATIONARY_LAVA) {
            this.liquids.put(state.getLocation(), false);
         } else if (state.getBlock().getType() == Material.WATER || state.getBlock().getType() == Material.STATIONARY_WATER) {
            this.liquids.put(state.getLocation(), true);
         }
      }
   }

   public void addBlockState(Block b) {
      this.addBlockState(b.getState());
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
      return this.ready;
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
