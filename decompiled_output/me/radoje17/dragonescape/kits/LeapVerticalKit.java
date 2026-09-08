package me.radoje17.dragonescape.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

/**
 * Mineplex Dragon Escape's four-charge PerkLeap adapted to Parcade's kit lifecycle.
 *
 * Parcade keeps its original Leap kit unchanged, so the two implementations can be
 * compared directly in solo mode. The launch vector and grounded boost here match
 * Mineplex's PerkLeap("Leap", 1, 1, 8000, 4) and UtilAction.velocity behavior.
 */
public class LeapVerticalKit extends Kit {
   private static final int MAX_USES = MineplexLeapPhysics.MAX_USES;
   private static LeapVerticalKit instance;
   private final Map<Player, Integer> uses = new WeakHashMap<>();
   private final Map<Player, Vector> pendingVelocity = new WeakHashMap<>();

   public LeapVerticalKit() {
      super(KitType.LEAPCLASSIC, Material.IRON_AXE, Lang.getList("leap-vertical-lore"));
      this.breaksBlocks = false;
      this.publicFriendly = false;
      instance = this;

      ItemStack leapAxe = createLeapAxe(MAX_USES);
      this.items.put(0, leapAxe);

      this.menuItem = new ItemStack(Material.IRON_AXE);
      ItemMeta menuMeta = this.menuItem.getItemMeta();
      // The legacy selector and leaderboards resolve kits by this display name.
      // Keep it aligned with LEAPCLASSIC; describe Mineplex behavior in the lore.
      menuMeta.setDisplayName(ChatColor.WHITE + "Leapclassic");
      List<String> lore = new ArrayList<>();
      for (String line : Lang.getList("leap-vertical-lore")) {
         lore.add(ChatColor.WHITE + line);
      }
      menuMeta.setLore(lore);
      this.menuItem.setItemMeta(menuMeta);
   }

   @Override
   public void giveItems(Player player) {
      this.pendingVelocity.remove(player);
      this.uses.put(player, MAX_USES);
      player.getInventory().setItem(0, createLeapAxe(MAX_USES));
      player.setExp(0.99F);
   }

   public static void update() {
      if (instance != null) {
         instance.cooldown = DragonEscape.getConfiguration()
            .getInt("kits.leapclassic.cooldown", MineplexLeapPhysics.DEFAULT_COOLDOWN_SECONDS);
      }
   }

   // Spigot 1.8 marks air interactions cancelled because there is no block to use.
   // Like the original Leap kit, handle these without uncancelling vanilla actions.
   @EventHandler
   @Override
   public void event(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      Game game = DragonEscape.getInstance().getGameManager().getGame(player);
      if (game == null || KitManager.getKit(player) != this) {
         return;
      }
      if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
         return;
      }
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK && MineplexLeapPhysics.isUsable(event.getClickedBlock())) {
         return;
      }
      if (player.getItemInHand() == null || !isAxe(player.getItemInHand().getType())) {
         return;
      }

      int remaining = this.uses.containsKey(player) ? this.uses.get(player) : MAX_USES;
      if (remaining <= 0) {
         player.sendMessage(Lang.getMessage("no-leaps-left"));
         return;
      }
      if (!KitManager.testCooldown(player)) {
         return;
      }

      Vector velocity = MineplexLeapPhysics.calculate(
         player.getLocation().getDirection(),
         MineplexLeapPhysics.isGrounded(player)
      );
      player.setFallDistance(0.0F);
      // Mineplex UtilAction + VelocityFix preserve the vector until packet dispatch.
      final Vector intended = velocity.clone();
      this.pendingVelocity.put(player, intended);
      org.bukkit.Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), () -> {
         if (this.pendingVelocity.get(player) == intended) {
            this.pendingVelocity.remove(player);
         }
      }, 20L);
      player.setVelocity(velocity);
      player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 8);

      KitManager.setCooldown(player, this.cooldown);
      remaining--;
      this.uses.put(player, remaining);
      player.setExp(Math.min(0.99F, (float)remaining / (float)MAX_USES));

      player.getItemInHand().setAmount(remaining);
      player.sendMessage(
         Lang.getMessage("leap-uses-left")
            .replaceAll("%uses%", Integer.toString(remaining))
            .replaceAll("%use%", remaining == 1 ? "use" : "uses")
      );
   }

   @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
   public void fixVelocity(org.bukkit.event.player.PlayerVelocityEvent event) {
      Vector intended = this.pendingVelocity.remove(event.getPlayer());
      if (intended != null && !event.isCancelled()
         && KitManager.getKit(event.getPlayer()) == this
         && DragonEscape.getInstance().getGameManager().getGame(event.getPlayer()) != null) {
         // Match Mineplex VelocityFix: update entity motion before tracker packet creation.
         event.getPlayer().setVelocity(intended);
      }
   }

   @EventHandler
   public void removeDataOnQuit(PlayerQuitEvent event) {
      this.uses.remove(event.getPlayer());
      this.pendingVelocity.remove(event.getPlayer());
   }

   private static ItemStack createLeapAxe(int amount) {
      ItemStack leapAxe = new ItemStack(Material.IRON_AXE, amount);
      ItemMeta leapMeta = leapAxe.getItemMeta();
      leapMeta.setDisplayName(ChatColor.WHITE + "Leap Axe");
      leapAxe.setItemMeta(leapMeta);
      return leapAxe;
   }

   private static boolean isAxe(Material material) {
      return material == Material.WOOD_AXE
         || material == Material.STONE_AXE
         || material == Material.IRON_AXE
         || material == Material.GOLD_AXE
         || material == Material.DIAMOND_AXE;
   }
}
