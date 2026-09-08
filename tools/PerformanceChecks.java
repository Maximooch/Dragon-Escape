import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.blocks.BaseBlock;
import me.radoje17.dragonescape.*;
import me.radoje17.dragonescape.utils.ArenaUtils;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class PerformanceChecks extends JavaPlugin implements Runnable {
 private Arena arena;
 private int phase, ticks, second, failed;
 private long last, maxTick;
 private Clipboard expected;
 private Block chest;
 private int checks;
 public void onEnable() {
  Bukkit.getScheduler().runTaskLater(this, () -> {
   try {
    check(ArenaUtils.getCoords()==0,"startup performs no untracked pre-pastes");
    GameManager gm=DragonEscape.getInstance().getGameManager();
    Game g=new Game(); gm.addGameInProgress(4242,g);
    Field id=Game.class.getDeclaredField("gameID"); id.setAccessible(true); id.setInt(g,4242);
    gm.removeGameFromLists(g); check(gm.getGameByID(4242)==null,"active-game removal uses integer ID");
    ChunkUnloadEvent event=new ChunkUnloadEvent(Bukkit.getWorld("DE").getChunkAt(0,0));
    gm.chunkUnload(event); check(!event.isCancelled(),"inactive solo-world chunks may unload");
    try(FileInputStream input=new FileInputStream("plugins/DragonEscape/schematics/SkylandsFixed.schematic")) {
     expected=ClipboardFormat.SCHEMATIC.getReader(input).read(new BukkitWorld(ArenaUtils.getWorld()).getWorldData());
    }
    arena=ArenaUtils.getArena("skylands",false);
    ArenaUtils.makeUnavailable(arena);
    second=ArenaUtils.pasteSchematic("SkylandsFixed");
    check(!arena.isReady(),"new arena is not ready before paste completion");
    File bad=new File("plugins/DragonEscape/schematics/test-corrupt.schematic");
    try(FileWriter out=new FileWriter(bad)){out.write("invalid");}
    failed=ArenaUtils.pasteSchematic("test-corrupt");
    last=System.nanoTime();
    Bukkit.getScheduler().runTaskTimer(this,this,1,1);
   } catch(Throwable error){fail(error);}
  },20L);
 }
 private void check(boolean value,String message) {
  if(!value)throw new AssertionError(message);
  checks++; getLogger().info("PASS "+message);
 }
 private void fail(Throwable error) {
  getLogger().log(java.util.logging.Level.SEVERE,"PERFORMANCE TEST FAILED",error);
  Bukkit.shutdown();
 }
 public void run() {
  long now=System.nanoTime();maxTick=Math.max(maxTick,now-last);last=now;
  if(++ticks>2400){fail(new AssertionError("test timed out phase="+phase));return;}
  try {
   if(phase==0 && arena.isReady() && ArenaUtils.isPasteComplete(second) && ArenaUtils.hasPasteFailed(failed)) {
    int samples=0;
    getLogger().info("Expected bounds="+expected.getRegion()+" origin="+expected.getOrigin());
    for(Vector position:expected.getRegion()) {
     BaseBlock block=expected.getBlock(position);
     if(block.getId()==0)continue;
     Vector dst=position.subtract(expected.getOrigin()).add(new Vector(arena.COORDS,80,arena.COORDS));
     if(dst.getBlockY()<0 || dst.getBlockY()>=256)continue;
     Block actual=ArenaUtils.getWorld().getBlockAt(dst.getBlockX(),dst.getBlockY(),dst.getBlockZ());
     if(actual.getTypeId()!=block.getId() || actual.getData()!=block.getData())throw new AssertionError("paste mismatch at "+dst+" expected="+block+" actual="+actual.getType()+":"+actual.getData());
     if(++samples>=1000)break;
    }
    check(samples>0,"paste completed with matching schematic block IDs/data ("+samples+" samples)");
    check(ArenaUtils.hasPasteFailed(failed),"corrupt schematic marked failed, never ready");
    World world=ArenaUtils.getWorld();
    chest=world.getBlockAt(arena.COORDS,200,arena.COORDS);
    chest.setType(Material.CHEST,false);((Chest)chest.getState()).getBlockInventory().setItem(0,new ItemStack(Material.DIAMOND,3));
    arena.addBlockState(chest);chest.setType(Material.AIR,false);
    for(int x=1;x<=600;x++){
     Block block=world.getBlockAt(arena.COORDS+x,200,arena.COORDS);block.setType(Material.STONE,false);arena.addBlockState(block);block.setType(Material.AIR,false);
    }
    arena.restore();check(!arena.isReady(),"restoring arena withheld from pool");phase=1;
   } else if(phase==1 && arena.isReady()) {
    check(chest.getType()==Material.CHEST,"restoration preserves chest block");
    ItemStack content=((Chest)chest.getState()).getBlockInventory().getItem(0);
    check(content!=null && content.getType()==Material.DIAMOND && content.getAmount()==3,"restoration preserves tile inventory");
    for(int x=1;x<=600;x++)if(ArenaUtils.getWorld().getBlockAt(arena.COORDS+x,200,arena.COORDS).getType()!=Material.STONE)throw new AssertionError("restore missed block "+x);
    check(true,"budgeted restoration restores all 600 blocks");
    Arena reused=ArenaUtils.getArena("skylands",false);check(reused==arena,"completed arena reused rather than re-pasted");
    ArenaUtils.makeUnavailable(reused);
    check(ArenaUtils.getCoords()==7500,"no allocation while reusing arena");
    arena.addBlockState(chest); chest.setType(Material.AIR,false);
    arena.restore();
    Arena reserved = ArenaUtils.getArena("SKYLANDS",false);
    check(reserved == arena && !reserved.isReady(), "restoring copy reserved without a new paste");
    check(!reserved.canReserveRestore(), "reserved restoring copy cannot be claimed twice");
    check(ArenaUtils.getCoords()==7500,"reservation allocates no schematic copy");
    phase=2;
   } else if(phase==2 && arena.isReady()) {
    Field pool= ArenaUtils.class.getDeclaredField("availableArenas"); pool.setAccessible(true);
    Map<String,List<Arena>> available=(Map<String,List<Arena>>)pool.get(null);
    check(!available.get("skylands").contains(arena),"reserved copy stays exclusively owned after restore");
    check(chest.getType()==Material.CHEST,"reserved copy fully restored before ready");
    Field failure = Arena.class.getDeclaredField("restoreFailed"); failure.setAccessible(true);
    failure.setBoolean(arena,true);
    check(arena.hasLoadFailed() && !arena.isReady() && !arena.canReserveRestore(), "failed restoration is quarantined and reported to waiting games");
    failure.setBoolean(arena,false);
    Game publicGame = new Game("skylands");
    Field gameId = Game.class.getDeclaredField("gameID"); gameId.setAccessible(true);
    int id = gameId.getInt(publicGame);
    GameManager manager = DragonEscape.getInstance().getGameManager();
    manager.addGameInProgress(id, publicGame);
    publicGame.endGame();
    int tasks = Bukkit.getScheduler().getPendingTasks().size();
    publicGame.endGame();
    check(tasks == Bukkit.getScheduler().getPendingTasks().size(), "ending a public game twice does not schedule duplicate cleanup");
    check(manager.getGameByID(id) == null, "ended public game no longer ticks");
    getLogger().info("PERFORMANCE TESTS PASSED: "+checks+"; ticks="+ticks+" max observed tick interval ms="+(maxTick/1000000.0));
    Bukkit.shutdown();phase=2;
   }
  }catch(Throwable error){fail(error);}
 }
}
