package me.radoje17.dragonescape.kits;

import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DiggerKit extends Kit {
   public DiggerKit() {
      super(KitType.Digger, Material.DIAMOND_PICKAXE, Lang.getList("digger-lore"));
      ItemStack diamondPickaxe = new ItemStack(Material.DIAMOND_PICKAXE, 6);
      ItemMeta meta = diamondPickaxe.getItemMeta();
      meta.setDisplayName(ChatColor.WHITE + "Digger");
      diamondPickaxe.setItemMeta(meta);
      this.items.put(0, diamondPickaxe);
   }

   @Override
   public void giveItems(Player p) {
      super.giveItems(p);
      p.setGameMode(GameMode.SURVIVAL);
   }

   @EventHandler
   @Override
   public void event(PlayerInteractEvent e) {
      if (KitManager.getKit(e.getPlayer()) == this) {
         Game g;
         if ((g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer())) != null) {
            if (e.getPlayer().getInventory().getItem(0) != null && e.getPlayer().getInventory().getItem(0).getType() == Material.DIAMOND_PICKAXE) {
               if (e.getPlayer().getItemInHand() == null || e.getPlayer().getItemInHand().getType() != Material.SUGAR) {
                  if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                     int amount = e.getPlayer().getInventory().getItem(0).getAmount() - 1;
                     if (amount > 0) {
                        e.getPlayer().getInventory().getItem(0).setAmount(amount);
                     } else {
                        e.getPlayer().getInventory().setItem(0, null);
                     }

                     e.getPlayer()
                        .getInventory()
                        .addItem(new ItemStack[]{new ItemStack(e.getClickedBlock().getType(), 1, (short)0, (byte)(e.getClickedBlock().getData() % 4))});
                     g.getArena().addBlockState(e.getClickedBlock());
                     e.getClickedBlock().setType(Material.AIR);
                  }
               }
            }
         }
      }
   }
}
