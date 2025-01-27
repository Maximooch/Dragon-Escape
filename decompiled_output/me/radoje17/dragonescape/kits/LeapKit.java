package me.radoje17.dragonescape.kits;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class LeapKit extends Kit {
   double intensity = 1.1;
   double intensity2 = 0.2;

   public LeapKit() {
      super(KitType.LEAP, Material.IRON_AXE, Lang.getList("leap-lore"));
      this.breaksBlocks = false;
      ItemStack leapAxe = new ItemStack(Material.IRON_AXE);
      ItemMeta leapMeta = leapAxe.getItemMeta();
      leapMeta.setDisplayName(ChatColor.WHITE + "Leap axe");
      leapAxe.setItemMeta(leapMeta);
      this.items.put(0, leapAxe);
      if (this.section != null) {
         double temp = this.section.getDouble("intensity");
         if (temp != 0.0) {
            this.intensity = temp;
         }

         temp = this.section.getDouble("intensity-2");
         if (temp != 0.0) {
            this.intensity2 = temp;
         }
      }
   }

   @EventHandler
   @Override
   public void event(PlayerInteractEvent e) {
      if (KitManager.getKit(e.getPlayer()) == this) {
         Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
         if (g != null && g.isGameStarted()) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
               if (e.getPlayer().getItemInHand() != null && e.getPlayer().getItemInHand().getType() == Material.IRON_AXE) {
                  if (KitManager.testCooldown(e.getPlayer())) {
                     Player player = e.getPlayer();
                     KitManager.setCooldown(player, this.cooldown);
                     Vector vec = player.getLocation().getDirection();
                     vec.normalize();
                     vec.multiply(this.intensity);
                     vec.setY(vec.getY() + this.intensity2);
                     if (vec.getY() > 1.2) {
                        vec.setY(1.2);
                     }

                     player.setFallDistance(0.0F);
                     player.setVelocity(vec);
                     player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
                  }
               }
            }
         }
      }
   }
}
