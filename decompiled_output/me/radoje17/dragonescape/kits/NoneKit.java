package me.radoje17.dragonescape.kits;

import java.util.Collections;
import me.radoje17.dragonescape.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NoneKit extends Kit {
   public NoneKit() {
      super(KitType.NONE, Material.DIAMOND_BOOTS, Collections.emptyList());
      this.breaksBlocks = false;
      this.menuItem = new ItemStack(Material.DIAMOND_BOOTS, 1);
      ItemMeta menuMeta = this.menuItem.getItemMeta();
      menuMeta.setLore(Lang.getList("none-lore"));
      menuMeta.setDisplayName(ChatColor.BLUE + "Parkour");
      this.menuItem.setItemMeta(menuMeta);
   }

   @Override
   public void event(PlayerInteractEvent e) {
   }
}
