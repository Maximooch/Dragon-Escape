package me.radoje17.dragonescape.kits;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * The Dragon Escape leap vector used by Mineplex's PerkLeap/UtilAction stack.
 *
 * The calculation is kept independent from Bukkit so representative vectors can
 * be verified without booting a Minecraft server. Ground detection intentionally
 * mirrors Mineplex's 1.8 bounding-box sampling rather than Player#isOnGround.
 */
public final class MineplexLeapPhysics {
   public static final double POWER = 1.0;
   public static final double Y_ADD = 0.2;
   public static final double Y_MAX = 1.0;
   public static final double GROUNDED_Y_BOOST = 0.2;
   public static final int MAX_USES = 4;
   public static final int DEFAULT_COOLDOWN_SECONDS = 8;

   private static final Set<Material> PASSABLE = EnumSet.of(
      Material.AIR,
      Material.SAPLING,
      Material.WATER,
      Material.STATIONARY_WATER,
      Material.LAVA,
      Material.STATIONARY_LAVA,
      Material.BED_BLOCK,
      Material.POWERED_RAIL,
      Material.DETECTOR_RAIL,
      Material.WEB,
      Material.LONG_GRASS,
      Material.DEAD_BUSH,
      Material.YELLOW_FLOWER,
      Material.RED_ROSE,
      Material.BROWN_MUSHROOM,
      Material.RED_MUSHROOM,
      Material.TORCH,
      Material.FIRE,
      Material.REDSTONE_WIRE,
      Material.CROPS,
      Material.SIGN_POST,
      Material.WOODEN_DOOR,
      Material.LADDER,
      Material.RAILS,
      Material.WALL_SIGN,
      Material.LEVER,
      Material.STONE_PLATE,
      Material.IRON_DOOR_BLOCK,
      Material.WOOD_PLATE,
      Material.REDSTONE_TORCH_OFF,
      Material.REDSTONE_TORCH_ON,
      Material.STONE_BUTTON,
      Material.SNOW,
      Material.SUGAR_CANE_BLOCK,
      Material.FENCE,
      Material.PORTAL,
      Material.CAKE_BLOCK,
      Material.DIODE_BLOCK_OFF,
      Material.DIODE_BLOCK_ON,
      Material.TRAP_DOOR,
      Material.IRON_FENCE,
      Material.THIN_GLASS,
      Material.PUMPKIN_STEM,
      Material.MELON_STEM,
      Material.VINE,
      Material.FENCE_GATE,
      Material.WATER_LILY,
      Material.NETHER_WARTS,
      Material.ENCHANTMENT_TABLE,
      Material.BREWING_STAND,
      Material.CAULDRON,
      Material.ENDER_PORTAL,
      Material.ENDER_PORTAL_FRAME,
      Material.DAYLIGHT_DETECTOR,
      Material.STAINED_GLASS_PANE,
      Material.IRON_TRAPDOOR,
      Material.DAYLIGHT_DETECTOR_INVERTED,
      Material.BARRIER,
      Material.BIRCH_FENCE_GATE,
      Material.JUNGLE_FENCE_GATE,
      Material.DARK_OAK_FENCE_GATE,
      Material.ACACIA_FENCE_GATE,
      Material.SPRUCE_FENCE,
      Material.BIRCH_FENCE,
      Material.JUNGLE_FENCE,
      Material.DARK_OAK_FENCE,
      Material.ACACIA_FENCE,
      Material.SPRUCE_DOOR,
      Material.BIRCH_DOOR,
      Material.JUNGLE_DOOR,
      Material.ACACIA_DOOR,
      Material.DARK_OAK_DOOR
   );

   private static final Set<Material> USABLE = EnumSet.of(
      Material.DISPENSER,
      Material.BED_BLOCK,
      Material.PISTON_BASE,
      Material.BOOKSHELF,
      Material.CHEST,
      Material.WORKBENCH,
      Material.FURNACE,
      Material.BURNING_FURNACE,
      Material.WOODEN_DOOR,
      Material.LEVER,
      Material.IRON_DOOR_BLOCK,
      Material.STONE_BUTTON,
      Material.FENCE,
      Material.DIODE_BLOCK_OFF,
      Material.DIODE_BLOCK_ON,
      Material.TRAP_DOOR,
      Material.FENCE_GATE,
      Material.NETHER_FENCE,
      Material.ENCHANTMENT_TABLE,
      Material.BREWING_STAND,
      Material.ENDER_CHEST,
      Material.ANVIL,
      Material.TRAPPED_CHEST,
      Material.HOPPER,
      Material.DROPPER,
      Material.BIRCH_FENCE_GATE,
      Material.JUNGLE_FENCE_GATE,
      Material.DARK_OAK_FENCE_GATE,
      Material.ACACIA_FENCE_GATE,
      Material.SPRUCE_FENCE_GATE,
      Material.SPRUCE_DOOR,
      Material.BIRCH_DOOR,
      Material.JUNGLE_DOOR,
      Material.ACACIA_DOOR,
      Material.DARK_OAK_DOOR
   );

   private MineplexLeapPhysics() {
   }

   public static Vector calculate(Vector direction, boolean grounded) {
      double[] result = calculate(direction.getX(), direction.getY(), direction.getZ(), grounded);
      return new Vector(result[0], result[1], result[2]);
   }

   static double[] calculate(double x, double y, double z, boolean grounded) {
      double length = Math.sqrt(x * x + y * y + z * z);
      if (length == 0.0 || Double.isNaN(length)) {
         return new double[]{0.0, 0.0, 0.0};
      }

      x = x / length * POWER;
      y = y / length * POWER + Y_ADD;
      z = z / length * POWER;

      if (y > Y_MAX) {
         y = Y_MAX;
      }
      if (grounded) {
         y += GROUNDED_Y_BOOST;
      }

      return new double[]{x, y, z};
   }

   public static boolean isGrounded(Player player) {
      AxisAlignedBB box = ((CraftPlayer)player).getHandle().getBoundingBox();
      World world = player.getWorld();
      int minX = floor(box.a);
      int maxX = floor(box.d);
      int y = floor(player.getLocation().getY() - 0.1);
      int minZ = floor(box.c);
      int maxZ = floor(box.f);

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            Block block = world.getBlockAt(x, y, z);
            if (!PASSABLE.contains(block.getType())) {
               return true;
            }
         }
      }
      return false;
   }

   public static boolean isUsable(Block block) {
      return block != null && USABLE.contains(block.getType());
   }

   private static int floor(double value) {
      int integer = (int)value;
      return value < integer ? integer - 1 : integer;
   }
}
