package me.radoje17.dragonescape.utils;

import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import me.radoje17.dragonescape.Arena;
import me.radoje17.dragonescape.DragonEscape;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ArenaUtils {
   private static final java.util.concurrent.ConcurrentHashMap<Integer, Integer> pasteStates = new java.util.concurrent.ConcurrentHashMap<>();
   private static final java.util.ArrayDeque<Runnable> pendingPastes = new java.util.ArrayDeque<>();
   private static volatile boolean stopping;
   private static boolean pasteRunning;
   private static volatile EditSession activePaste;
   public static boolean isPasteComplete(int coords) { return Integer.valueOf(1).equals(pasteStates.get(coords)); }
   public static boolean hasPasteFailed(int coords) { return Integer.valueOf(-1).equals(pasteStates.get(coords)); }
   public static void stopPastes() { stopping = true; pendingPastes.clear(); EditSession edit = activePaste; if (edit != null) edit.cancel(); }
   private static void nextPaste() {
      if (stopping || pasteRunning || pendingPastes.isEmpty()) return;
      pasteRunning = true;
      Bukkit.getScheduler().runTaskAsynchronously(DragonEscape.getInstance(), pendingPastes.removeFirst());
   }
   private static File f;
   private static FileConfiguration arenas;
   private static int coords;
   private static World world;
   private static HashMap<Integer, Arena> arenaIds;
   private static HashMap<String, List<Arena>> arenasInUse;
   private static HashMap<String, List<Arena>> availableArenas;

   public ArenaUtils() {
      stopping = false;
      pasteRunning = false;
      pasteStates.clear();
      pendingPastes.clear();
      arenaIds = new HashMap<>();
      arenasInUse = new HashMap<>();
      availableArenas = new HashMap<>();
      f = new File(DragonEscape.getInstance().getDataFolder() + "/arenas.yml");
      arenas = YamlConfiguration.loadConfiguration(f);
      coords = 0;
      WorldCreator voidWorldCreator = new WorldCreator("dragonescape");
      voidWorldCreator.type(WorldType.FLAT);
      voidWorldCreator.generatorSettings("2;0;1;");
      world = voidWorldCreator.createWorld();
      world.setGameRuleValue("doDaylightCycle", "false");
      world.setGameRuleValue("keepInventory", "true");
      world.setGameRuleValue("randomTickSpeed", "0");
      world.setKeepSpawnInMemory(false);
      world.setGameRuleValue("doFireTick", "false");
      world.setGameRuleValue("mobGriefing", "false");
      world.setMonsterSpawnLimit(0);
      world.setAnimalSpawnLimit(0);
      File schematicsFolder = new File(DragonEscape.getInstance().getDataFolder() + "/schematics");
      schematicsFolder.mkdirs();
      Bukkit.getScheduler().runTaskTimer(DragonEscape.getInstance(), new Runnable() {
         @Override
         public void run() {
         }
      }, 20L, 20L);
   }

   public static Arena getArena(String arenaName, boolean solo) throws WorldEditException, IOException {
      arenaName = arenaName.toLowerCase(java.util.Locale.ROOT);
      if (availableArenas.containsKey(arenaName)) {
         for (Arena a : availableArenas.get(arenaName)) {
            if (a.isReady() && (!a.isCheckSolo() || a.isSolo() == solo)) {
               makeUnavailable(a);
               return a.restore(false);
            }
         }
      }

      // Reserve an idle arena being restored rather than paste an unnecessary copy.
      List<Arena> restoring = arenasInUse.get(arenaName);
      if (restoring != null) {
         for (Arena candidate : restoring) {
            if (candidate.canReserveRestore() && (!candidate.isCheckSolo() || candidate.isSolo() == solo)) {
               candidate.reserveRestore();
               return candidate;
            }
         }
      }
      return addArena(new Arena(arenaName, solo), false);
   }

   public static String getAuthor(String arena) {
      String s = arenas.getString(arena.toLowerCase() + ".author");
      return s == null ? "unknown" : s;
   }

   public static Arena addArena(Arena a, boolean available) {
      HashMap<String, List<Arena>> map = available ? availableArenas : arenasInUse;
      List<Arena> arenas = map.get(a.getArenaName());
      if (arenas == null) {
         arenas = new ArrayList<>();
      }

      if (!arenas.contains(a)) {
         arenas.add(a);
      }

      map.put(a.getArenaName(), arenas);
      arenaIds.put(a.COORDS / 2500, a);
      return a;
   }

   public static void makeAvailable(Arena a) {
      List<Arena> arenas = arenasInUse.get(a.getArenaName());
      if (arenas != null && arenas.contains(a)) {
         arenas.remove(a);
         arenasInUse.put(a.getArenaName(), arenas);
      }

      addArena(a, true);
   }

   public static void makeUnavailable(Arena a) {
      List<Arena> arenas = availableArenas.get(a.getArenaName());
      if (arenas != null && arenas.contains(a)) {
         arenas.remove(a);
         availableArenas.put(a.getArenaName(), arenas);
      }

      addArena(a, false);
   }

   private static void saveData() {
      try {
         arenas.save(f);
      } catch (IOException var1) {
         var1.printStackTrace();
      }
   }

   public static boolean isArena(String arenaName) {
      return arenas.isConfigurationSection(arenaName.toLowerCase());
   }

   public static String getSchematicName(String arenaName) {
      return !isArena(arenaName) ? "" : arenas.getString(arenaName.toLowerCase() + ".schematic");
   }

   public static boolean hasSchematic(String arenaName, boolean solo) {
      return !isArena(arenaName) ? false : arenas.isString(arenaName.toLowerCase() + "." + (solo ? "solo-" : "") + "schematic");
   }

   public static String getSchematicName(String arenaName, boolean solo) {
      if (!isArena(arenaName)) {
         return "";
      } else {
         return !hasSchematic(arenaName, solo)
            ? arenas.getString(arenaName.toLowerCase() + "." + (!solo ? "solo-" : "") + "schematic")
            : arenas.getString(arenaName.toLowerCase() + "." + (solo ? "solo-" : "") + "schematic");
      }
   }

   public static void setSchematicName(String arenaName, String schematicName, boolean solo) {
      if (isArena(arenaName)) {
         arenas.set(arenaName.toLowerCase() + "." + (solo ? "solo-" : "") + "schematic", schematicName + ".schematic");
      }
   }

   public static String getArenaName(String arenaName) {
      return !isArena(arenaName.toLowerCase()) ? arenaName : arenas.getString(arenaName.toLowerCase() + ".name");
   }

   public static void setArenaTime(String arenaName, long time) {
      if (isArena(arenaName)) {
         arenas.set(arenaName.toLowerCase() + ".time", time);
         saveData();
      }
   }

   public static long getArenaTime(String arenaName) {
      return arenas.getLong(arenaName.toLowerCase() + ".time");
   }

   public static void fix() {
      for (String arenaName : getArenas()) {
         boolean radi = false;
         List<Location> spawnpoints = new ArrayList<>();

         for (Location l : getSpawnpoints(arenaName, 0)) {
            if (l.getX() > 2000.0) {
               l.setX(l.getX() - 2500.0);
               radi = true;
            }

            if (l.getZ() > 2000.0) {
               l.setZ(l.getZ() - 2500.0);
               radi = true;
            }

            spawnpoints.add(l);
         }

         if (radi) {
            arenas.set(arenaName + ".spawnpoints", null);

            for (Location l : spawnpoints) {
               addSpawnpoint(arenaName, l);
            }
         }
      }

      for (String arenaName : getArenas()) {
         boolean radi = false;
         List<Location> dragonpoints = new ArrayList<>();

         for (Location l : getDragonpoints(arenaName, 0)) {
            if (l.getX() > 2000.0) {
               l.setX(l.getX() - 2500.0);
               radi = true;
            }

            if (l.getZ() > 2000.0) {
               l.setZ(l.getZ() - 2500.0);
               radi = true;
            }

            dragonpoints.add(l);
         }

         if (radi) {
            arenas.set(arenaName + ".dragonpoints", null);

            for (Location l : dragonpoints) {
               addDragonpoint(arenaName, l);
            }
         }
      }
   }

   public static void setSpecial(String arenaName, boolean special) {
      if (isArena(arenaName)) {
         arenas.set(arenaName.toLowerCase() + ".special", special);
         saveData();
      }
   }

   public static boolean isSpecial(String arenaName) {
      return arenas.getBoolean(arenaName.toLowerCase() + ".special");
   }

   public static void setSpawnPoint(String arenaName, Location l) {
      if (isArena(arenaName)) {
         arenas.set(arenaName.toLowerCase() + ".location", LocationUtils.locationToStringWorld(l));
         saveData();
      }
   }

   public static Location getSpawnPoint(String arenaName) {
      if (!isArena(arenaName)) {
         return null;
      } else {
         String s = arenas.getString(arenaName.toLowerCase() + ".location");
         return s == null ? null : LocationUtils.stringToLocationWorld(s);
      }
   }

   public static boolean hasSpawnPoint(String arenaName) {
      return getSpawnPoint(arenaName) != null;
   }

   public static void createArena(String arenaName, String schematicName) throws WorldEditException, IOException {
      pasteSchematic(schematicName);
      arenas.set(arenaName.toLowerCase() + ".name", arenaName);
      arenas.set(arenaName.toLowerCase() + ".schematic", schematicName);
      saveData();
   }

   public static void addSpawnpoint(String arenaName, Location l) {
      List<String> locations = new ArrayList<>(getSpawnpointsString(arenaName));
      locations.add(LocationUtils.locationToString(l));
      arenas.set(arenaName.toLowerCase() + ".spawnpoints", locations);
      saveData();
   }

   public static void removeSpawnpoint(String arenaName, int id) throws IndexOutOfBoundsException {
      List<String> locations = new ArrayList<>(getSpawnpointsString(arenaName));
      locations.remove(id);
      arenas.set(arenaName.toLowerCase() + ".spawnpoints", locations);
      saveData();
   }

   public static void addDragonpoint(String arenaName, Location l) {
      List<String> locations = new ArrayList<>(getDragonpointsString(arenaName));
      locations.add(LocationUtils.locationToStringNoFace(l));
      arenas.set(arenaName.toLowerCase() + ".dragonpoints", locations);
      saveData();
   }

   public static void removeDragonpoint(String arenaName, int id) throws IndexOutOfBoundsException {
      List<String> locations = new ArrayList<>(getDragonpointsString(arenaName));
      locations.remove(id);
      arenas.set(arenaName.toLowerCase() + ".dragonpoints", locations);
      saveData();
   }

   public static List<Location> getSpawnpoints(String arenaName, int coords) {
      if (!arenas.isList(arenaName.toLowerCase() + ".spawnpoints")) {
         return Collections.emptyList();
      } else {
         List<Location> locations = new ArrayList<>();

         for (String s : arenas.getStringList(arenaName.toLowerCase() + ".spawnpoints")) {
            locations.add(LocationUtils.stringToLocation(s, coords));
         }

         return locations;
      }
   }

   public static List<Location> getDragonpoints(String arenaName, int coords) {
      if (!arenas.isList(arenaName.toLowerCase() + ".dragonpoints")) {
         return Collections.emptyList();
      } else {
         List<Location> locations = new ArrayList<>();

         for (String s : arenas.getStringList(arenaName.toLowerCase() + ".dragonpoints")) {
            locations.add(LocationUtils.stringToLocation(s, coords));
         }

         return locations;
      }
   }

   private static List<String> getSpawnpointsString(String arenaName) {
      return !arenas.isList(arenaName.toLowerCase() + ".spawnpoints")
         ? Collections.emptyList()
         : arenas.getStringList(arenaName.toLowerCase() + ".spawnpoints");
   }

   private static List<String> getDragonpointsString(String arenaName) {
      return !arenas.isList(arenaName.toLowerCase() + ".dragonpoints")
         ? Collections.emptyList()
         : arenas.getStringList(arenaName.toLowerCase() + ".dragonpoints");
   }

   public static boolean waterKills(String arenaName) {
      return arenas.getBoolean(arenaName.toLowerCase() + ".water-kills");
   }

   public static Arena getArena(Block b) {
      int x = b.getLocation().getBlockX();
      return arenaIds.get(x / 2500);
   }

   public static void setWaterKills(String arenaName, boolean value) {
      arenas.set(arenaName.toLowerCase() + ".water-kills", value);
      saveData();
   }

   public static void removeArena(String arenaName) {
      if (isArena(arenaName)) {
         arenas.set(arenaName, null);
         saveData();
      }
   }

   public static int pasteSchematic(String schematicName) throws WorldEditException, IOException {
      final File file = new File(DragonEscape.getInstance().getDataFolder(), "schematics/" + schematicName + ".schematic");
      if (!file.isFile()) throw new IOException("Schematic not found: " + file);
      if (stopping) throw new IOException("Server is stopping");
      final int allocated = coords;
      coords += 2500;
      final com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
      final com.sk89q.worldedit.world.registry.WorldData worldData = weWorld.getWorldData();
      pasteStates.put(allocated, 0);
      pendingPastes.addLast(new Runnable() {
         @Override public void run() {
            ClipboardHolder holder = null;
            EditSession extent = null;
            try {
               if (stopping) return;
               Clipboard clipboard;
               try (FileInputStream input = new FileInputStream(file)) {
                  clipboard = ClipboardFormat.SCHEMATIC.getReader(input).read(worldData);
               }
               holder = new ClipboardHolder(clipboard, worldData);
               extent = new EditSessionBuilder(weWorld).fastmode(true).allowedRegionsEverywhere().limitUnlimited().changeSetNull().build();
               activePaste = extent;
               if (stopping) { extent.cancel(); return; }
               Operation operation = holder.createPaste(extent, worldData).to(new Vector(allocated, 80, allocated))
                  .ignoreEntities(true).ignoreAirBlocks(true).build();
               Operations.complete(operation);
               extent.flushQueue(); // This legacy FAWE implementation waits off-thread for placement.
               if (!stopping) pasteStates.put(allocated, 1);
            } catch (Exception error) {
               pasteStates.put(allocated, -1);
               if (extent != null) extent.cancel();
               DragonEscape.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Failed to paste " + file.getName(), error);
            } finally {
               activePaste = null;
               if (holder != null) holder.close();
               if (!stopping && DragonEscape.getInstance().isEnabled()) {
                  Bukkit.getScheduler().runTask(DragonEscape.getInstance(), new Runnable() {
                     @Override public void run() { pasteRunning = false; nextPaste(); }
                  });
               }
            }
         }
      });
      nextPaste();
      return allocated;
   }

   public static List<String> getArenasSorted() {
      List<String> arenas = new ArrayList<>();
      if (ArenaUtils.arenas.getKeys(false) != null) {
         for (String s : ArenaUtils.arenas.getKeys(false)) {
            arenas.add(s);
         }
      }

      arenas.sort(new Comparator<String>() {
         public int compare(String o1, String o2) {
            boolean special1 = ArenaUtils.isSpecial(o1);
            boolean special2 = ArenaUtils.isSpecial(o2);
            if (!special1 && special2) {
               return 1;
            } else {
               return special1 && !special2 ? -1 : o1.compareTo(o2);
            }
         }
      });
      return arenas;
   }

   public static List<String> getArenas() {
      List<String> arenas = new ArrayList<>();
      if (ArenaUtils.arenas.getKeys(false) != null) {
         for (String s : ArenaUtils.arenas.getKeys(false)) {
            arenas.add(s);
         }
      }

      Collections.sort(arenas);
      return arenas;
   }

   public static String getRandomArena() {
      List<String> arenas = getArenas();
      return arenas.get(new Random().nextInt(arenas.size()));
   }

   public static String[] getRandomArenas() {
      List<String> arenas = getArenas();
      int counter = 0;
      String[] arenaString = new String[3];
      Random r = new Random();
      boolean end = false;

      while (counter < 3) {
         String curr = arenas.get(r.nextInt(arenas.size()));

         for (int i = 0; i < counter; i++) {
            if (arenaString[i].equals(curr)) {
               end = true;
            }
         }

         if (!end) {
            arenaString[counter] = curr;
            counter++;
         } else {
            end = false;
         }
      }

      return arenaString;
   }

   public static int getCoords() {
      return coords;
   }

   public static World getWorld() {
      return world;
   }

   public static String getParkourName(String arenaName) {
      arenaName = arenaName.toLowerCase();
      return !arenas.isString(arenaName + ".parkour-name") ? arenaName : arenas.getString(arenaName + ".parkour-name");
   }

   public static void setParkourName(String arenaName, String parkourName) {
      arenas.set(arenaName + ".parkour-name", parkourName);
      saveData();
   }
}
