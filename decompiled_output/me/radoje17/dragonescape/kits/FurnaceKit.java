package me.radoje17.dragonescape.kits;

import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FurnaceKit extends Kit {
   public FurnaceKit() {
      super(KitType.Furnace, Material.FURNACE, Lang.getList("furnace-lore"));
      this.soloFriendly = false;
   }

   @EventHandler
   @Override
   public void event(PlayerInteractEvent e) {
      if (KitManager.getKit(e.getPlayer()) == this) {
         Game g;
         if ((g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer())) != null) {
            if (e.getPlayer().getItemInHand() == null || e.getPlayer().getItemInHand().getType() != Material.SUGAR) {
               Player player = e.getPlayer();
               ItemStack item = player.getItemInHand();
               Block block = e.getClickedBlock();
               if (block != null && block.getType() != Material.BARRIER && block.getType() != Material.BEACON) {
                  if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                     if (item == null || item.getType() != Material.AIR) {
                        return;
                     }

                     g.getArena().addBlockState(e.getClickedBlock());
                     player.setItemInHand(new ItemStack(block.getType(), 1, (short)0, (byte)(block.getData() % 4)));
                     block.setType(Material.AIR);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent e) {
      Game g;
      if ((g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer())) != null) {
         g.getArena().addBlockState(e.getBlockReplacedState());
         if (KitManager.getKit(e.getPlayer()) == this) {
            e.getPlayer().setItemInHand(new ItemStack(Material.BLAZE_ROD));
         }
      }
   }

   @Override
   public void giveItems(final Player p) {
      Game g = DragonEscape.getInstance().getGameManager().getGame(p);
      if (g != null) {
         for (int i = 0; i < 9; i++) {
            if (i != 0 || !g.isSolo()) {
               int vreme = i * 5 + 5;
               ItemStack blaze = new ItemStack(Material.BLAZE_POWDER, vreme);
               ItemMeta blazeMeta = blaze.getItemMeta();
               blazeMeta.setDisplayName(" ");
               blaze.setItemMeta(blazeMeta);
               p.getInventory().setItem(i, blaze);
               final int I = i;
               Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
                  @Override
                  public void run() {
                     p.getInventory().setItem(I, new ItemStack(Material.AIR, 1));
                  }
               }, 20L * (long)vreme);
            }
         }

         p.setGameMode(GameMode.SURVIVAL);
      }
   }
}
