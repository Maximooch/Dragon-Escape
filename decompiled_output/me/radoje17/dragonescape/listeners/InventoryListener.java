package me.radoje17.dragonescape.listeners;

import com.sk89q.worldedit.WorldEditException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import me.radoje17.dragonescape.MySQLConnector;
import me.radoje17.dragonescape.NBTEditor;
import me.radoje17.dragonescape.PracticeGame;
import me.radoje17.dragonescape.kits.Kit;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {
   private static HashMap<Player, String> gamesToStart;
   private static HashMap<Player, String> practiceGamesToStart;

   public InventoryListener() {
      gamesToStart = new HashMap<>();
      practiceGamesToStart = new HashMap<>();
   }

   public static void addGameToStart(Player p, String map) {
      gamesToStart.put(p, map);
   }

   public static void addPracticeGameToStart(Player p, String map) {
      practiceGamesToStart.put(p, map);
   }

   @EventHandler
   public void clickVoting(InventoryClickEvent e) {
      if (e.getCurrentItem() != null) {
         Player p = (Player)e.getWhoClicked();
         Game g = DragonEscape.getInstance().getGameManager().getGame(p);
         if (g != null) {
            if (InventoryUtils.isVotingInventoryOpen(p)) {
               if (e.getSlot() == 11) {
                  g.vote(p, 0);
               } else if (e.getSlot() == 13) {
                  g.vote(p, 1);
               } else if (e.getSlot() == 15) {
                  g.vote(p, 2);
               }

               if (e.getSlot() == 28) {
                  g.voteMap(p, 0);
               } else if (e.getSlot() == 31) {
                  g.voteMap(p, 1);
               } else if (e.getSlot() == 34) {
                  g.voteMap(p, 2);
               }

               g.updateVotingInventories();
            }
         }
      }
   }

   @EventHandler
   public void clickListener(InventoryClickEvent e) throws WorldEditException, IOException, SQLException {
      if (e.getInventory().getName().contains("Leaderboard (Page #")) {
         e.setCancelled(true);
         ItemStack clicked = e.getClickedInventory().getItem(e.getSlot());
         boolean solo = ChatColor.stripColor(((String)e.getInventory().getItem(0).getItemMeta().getLore().get(0)).split(" ")[1]).equals("solo");
         if (this.isRightArrow(clicked)) {
            String arenaName = ChatColor.stripColor(e.getInventory().getItem(0).getItemMeta().getDisplayName());
            int page = Integer.parseInt(e.getInventory().getName().replace("§fLeaderboard (Page #", "").replace(")", "").replaceAll("§f", "")) + 1;
            String kit = ChatColor.stripColor(((String)e.getInventory().getItem(0).getItemMeta().getLore().get(1)).split(" ")[1]);
            e.getWhoClicked().openInventory(InventoryUtils.getMapLeaderboard((Player)e.getWhoClicked(), arenaName, solo, kit, page));
         } else if (this.isLeftArrow(clicked)) {
            String arenaName = ChatColor.stripColor(e.getInventory().getItem(0).getItemMeta().getDisplayName());
            int page = Integer.parseInt(e.getInventory().getName().replace("§fLeaderboard (Page #", "").replace(")", "").replaceAll("§f", "")) - 1;
            String kit = ChatColor.stripColor(((String)e.getInventory().getItem(0).getItemMeta().getLore().get(1)).split(" ")[1]);
            e.getWhoClicked().openInventory(InventoryUtils.getMapLeaderboard((Player)e.getWhoClicked(), arenaName, solo, kit, page));
         } else {
            if (clicked.getType() == Material.SKULL_ITEM
               && clicked.getItemMeta().getLore() != null
               && (e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT)) {
               int runID = NBTEditor.getInt(clicked, "runID");
               if (e.getClick() == ClickType.SHIFT_LEFT) {
                  if (e.getWhoClicked().isOp() && clicked.getItemMeta().getLore().contains(Lang.getMessage("approve-run"))) {
                     MySQLConnector.approveRun(runID, (Player)e.getWhoClicked(), solo);
                     List<String> newLore = new ArrayList<>();
                     newLore.add(" ");
                     newLore.add(Lang.getMessage("approved-run").replaceAll("%player%", e.getWhoClicked().getName()));
                     newLore.add(" ");
                     ItemMeta newMeta = clicked.getItemMeta();
                     newMeta.setLore(newLore);
                     clicked.setItemMeta(newMeta);
                     ((Player)e.getWhoClicked()).updateInventory();
                  }
               } else if (e.getClick() == ClickType.SHIFT_RIGHT) {
                  if (clicked.getItemMeta().getLore().contains(Lang.getMessage("report-run"))) {
                     MySQLConnector.reportRun(runID, (Player)e.getWhoClicked(), solo);
                     List<String> newLore = new ArrayList<>();
                     newLore.add(" ");
                     if (clicked.getItemMeta().getLore().contains(Lang.getMessage("no-reports"))) {
                        newLore.add(Lang.getMessage("reported-run"));
                     } else {
                        int amount = Integer.parseInt(ChatColor.stripColor(((String)clicked.getItemMeta().getLore().get(1)).replaceAll("[^\\d.]", "")));
                        newLore.add(
                           Lang.getMessage("reported-run-count").replaceAll("%amount%", "" + amount).replaceAll("%others%", amount != 1 ? "others" : "other")
                        );
                     }

                     newLore.add(" ");
                     ItemMeta newMeta = clicked.getItemMeta();
                     newMeta.setLore(newLore);
                     clicked.setItemMeta(newMeta);
                  } else if (e.getWhoClicked().isOp() && clicked.getItemMeta().getLore().contains(Lang.getMessage("remove-run"))) {
                     MySQLConnector.disableRun(runID, (Player)e.getWhoClicked(), solo);
                     String arenaName = ChatColor.stripColor(e.getInventory().getItem(0).getItemMeta().getDisplayName());
                     int page = Integer.parseInt(e.getInventory().getName().replace("§fLeaderboard (Page #", "").replace(")", "").replaceAll("§f", ""));
                     String kit = ChatColor.stripColor(((String)e.getInventory().getItem(0).getItemMeta().getLore().get(1)).split(" ")[1]);
                     e.getWhoClicked().openInventory(InventoryUtils.getMapLeaderboard((Player)e.getWhoClicked(), arenaName, solo, kit, page));
                  }
               }
            }
         }
      } else if (e.getInventory().getName().endsWith(" Solo Leaderboard") || e.getInventory().getName().endsWith(" Public Leaderboard")) {
         e.setCancelled(true);
         if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
            String[] fensi = e.getInventory().getName().split(" ");
            String arenaName = fensi[0];
            boolean solo = fensi[1].equals("Solo");
            String kit = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName().toLowerCase());

            try {
               e.getWhoClicked().openInventory(InventoryUtils.getMapLeaderboard((Player)e.getWhoClicked(), arenaName, solo, kit, 1));
            } catch (SQLException var10) {
               var10.printStackTrace();
               e.getWhoClicked().closeInventory();
               e.getWhoClicked().sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var10.getLocalizedMessage()));
            }
         }
      } else if (e.getInventory().getName().endsWith(" Leaderboard")) {
         e.setCancelled(true);
         if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
            String arenaName = e.getInventory().getName().split(" Leaderboard")[0];
            e.getWhoClicked().openInventory(InventoryUtils.getChooseKitLeaderboardInventory(arenaName, e.getSlot() == 10));
         }
      } else if (e.getInventory().getName().equals("Select kit")) {
         e.setCancelled(true);
         if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
            Kit kit = KitManager.getKit(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).toLowerCase());
            if (kit == null) {
               kit = KitManager.getKit("none");
            }

            e.getWhoClicked().closeInventory();
            KitManager.setKit((Player)e.getWhoClicked(), kit);
            if (gamesToStart.containsKey(e.getWhoClicked())) {
               if (DragonEscape.getInstance().getGameManager().hasGame((Player)e.getWhoClicked())) {
                  gamesToStart.remove(e.getWhoClicked());
                  return;
               }

               DragonEscape.getInstance().getGameManager().getSoloGame(gamesToStart.get((Player)e.getWhoClicked()), (Player)e.getWhoClicked());
               gamesToStart.remove(e.getWhoClicked());
            } else if (practiceGamesToStart.containsKey(e.getWhoClicked())) {
               if (DragonEscape.getInstance().getGameManager().hasGame((Player)e.getWhoClicked())) {
                  practiceGamesToStart.remove(e.getWhoClicked());
                  return;
               }

               new PracticeGame((Player)e.getWhoClicked(), kit, practiceGamesToStart.get(e.getWhoClicked()));
               practiceGamesToStart.remove(e.getWhoClicked());
            } else {
               e.getWhoClicked().sendMessage(Lang.getMessage("kit-selected").replaceAll("%kit%", kit.getKitName()));
            }
         }
      } else {
         if (e.getInventory().getName().equals("Dragon Escape")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) {
               return;
            }

            Material m = e.getCurrentItem().getType();
            if (m == Material.EMERALD_BLOCK) {
               String map = ChatColor.stripColor((String)e.getCurrentItem().getItemMeta().getLore().get(2)).split(": ")[1];
               e.getWhoClicked().closeInventory();

               try {
                  DragonEscape.getInstance().getGameManager().getGame(map).addPlayer((Player)e.getWhoClicked());
               } catch (IOException | WorldEditException var9) {
                  e.getWhoClicked().sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var9.getLocalizedMessage()));
                  var9.printStackTrace();
               }
            }

            if (m == Material.STAINED_CLAY) {
               e.getWhoClicked().openInventory(InventoryUtils.getInProgressGamesInventory());
            }

            if (m == Material.MAP) {
               e.getWhoClicked().openInventory(InventoryUtils.getMapsInventory(0, false, false));
            }
         }

         if (e.getInventory().getName().equals("Dragon Escape in progress")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) {
               return;
            }

            Material mx = e.getCurrentItem().getType();
            if (mx == Material.EMERALD_BLOCK) {
               e.getWhoClicked().openInventory(InventoryUtils.getActiveGamesInventory());
            }

            if (mx == Material.REDSTONE_BLOCK) {
               DragonEscape.getInstance().getGameManager().getGameByID(NBTEditor.getInt(e.getCurrentItem(), "gameID")).addPlayer((Player)e.getWhoClicked());
            }
         }

         if (e.getInventory().getName().startsWith("Create ") && e.getInventory().getName().contains("Practice")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null) {
               Material mxx = e.getCurrentItem().getType();
               int page = Integer.parseInt(e.getInventory().getName().replaceAll("[^\\d.]", ""));
               if (e.getSlot() == e.getInventory().getSize() - 1 && e.getInventory().getItem(e.getSlot()) != null) {
                  e.getWhoClicked().openInventory(InventoryUtils.getMapsInventory(page, true, true));
               } else if (e.getSlot() == 45 && e.getInventory().getItem(45) != null) {
                  e.getWhoClicked().openInventory(InventoryUtils.getMapsInventory(page - 2, true, true));
               } else if (mxx == Material.PAPER) {
                  String map = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                  addPracticeGameToStart((Player)e.getWhoClicked(), map);
                  e.getWhoClicked().openInventory(InventoryUtils.getPracticeKitSelectMenu((Player)e.getWhoClicked()));
               }
            }
         } else {
            if (e.getInventory().getName().startsWith("Create ")) {
               e.setCancelled(true);
               if (e.getCurrentItem() == null) {
                  return;
               }

               Material mxx = e.getCurrentItem().getType();
               boolean solo = e.getInventory().getName().contains("Solo");
               int page = Integer.parseInt(e.getInventory().getName().replaceAll("[^\\d.]", ""));
               if (e.getSlot() == e.getInventory().getSize() - 1 && e.getInventory().getItem(e.getSlot()) != null) {
                  e.getWhoClicked().openInventory(InventoryUtils.getMapsInventory(page, solo, false));
               } else if (e.getSlot() == 45 && e.getInventory().getItem(45) != null) {
                  e.getWhoClicked().openInventory(InventoryUtils.getMapsInventory(page - 2, solo, false));
               } else if (mxx == Material.EMERALD_BLOCK) {
                  e.getWhoClicked().openInventory(InventoryUtils.getActiveGamesInventory());
               } else if (mxx == Material.PAPER) {
                  String map = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

                  try {
                     if (solo) {
                        gamesToStart.put((Player)e.getWhoClicked(), map);
                        e.getWhoClicked().openInventory(InventoryUtils.getKitSelectMenu((Player)e.getWhoClicked(), true));
                     } else {
                        DragonEscape.getInstance().getGameManager().getGame(map).addPlayer((Player)e.getWhoClicked());
                     }
                  } catch (IOException | WorldEditException var8) {
                     e.getWhoClicked().sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var8.getLocalizedMessage()));
                     var8.printStackTrace();
                  }
               }
            }

            if (DragonEscape.getInstance().getGameManager().getGame((Player)e.getWhoClicked()) != null) {
               e.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void rightClick(PlayerInteractEvent e) {
      if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
         Game g = DragonEscape.getInstance().getGameManager().getGame(e.getPlayer());
         if (g != null && !g.isGameStarted()) {
            if (e.getItem() != null && e.getItem().getType() == Material.STICK) {
               e.getPlayer().openInventory(InventoryUtils.getKitSelectMenu(e.getPlayer(), g.isSolo()));
            }

            if (e.getItem() != null
               && e.getItem().hasItemMeta()
               && e.getItem().getItemMeta().hasDisplayName()
               && e.getItem().getItemMeta().getDisplayName().equals(Lang.getMessage("voting-book-name"))) {
               g.openVotingInventory(e.getPlayer());
            }
         }
      }
   }

   @EventHandler
   public void reportsInventoryListener(InventoryClickEvent e) throws SQLException {
      if (e.getInventory().getName().contains("Reports")) {
         e.setCancelled(true);
         if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta().getLore() == null || e.getCurrentItem().getItemMeta().getLore().size() == 0) {
            return;
         }

         boolean solo = e.getInventory().getName().contains("Solo");
         int runID = NBTEditor.getInt(e.getCurrentItem(), "runID");
         if (e.getClick() == ClickType.SHIFT_RIGHT) {
            MySQLConnector.disableRun(runID, (Player)e.getWhoClicked(), solo);
            e.getWhoClicked().openInventory(InventoryUtils.getReportsInventory(0, solo));
         } else if (e.getClick() == ClickType.SHIFT_LEFT) {
            MySQLConnector.approveRun(runID, (Player)e.getWhoClicked(), solo);
            e.getWhoClicked().openInventory(InventoryUtils.getReportsInventory(0, solo));
         }
      }
   }

   @EventHandler
   public void closeInventory(InventoryCloseEvent e) {
      if (ChatColor.stripColor(e.getInventory().getName()).equals(ChatColor.stripColor(Lang.getMessage("settings-book")))) {
         try {
            DragonEscape.getInstance()
               .getGameManager()
               .getDeSettings((Player)e.getPlayer())
               .update(
                  this.isEnabled(e.getInventory().getItem(9)),
                  this.isEnabled(e.getInventory().getItem(10)),
                  this.isEnabled(e.getInventory().getItem(11)),
                  this.isEnabled(e.getInventory().getItem(14)),
                  this.isEnabled(e.getInventory().getItem(15)),
                  false,
                  this.isEnabled(e.getInventory().getItem(17)),
                  this.isEnabled(e.getInventory().getItem(16))
               );
         } catch (SQLException var3) {
            var3.printStackTrace();
            e.getPlayer().sendMessage(Lang.getMessage("error-occurred").replaceAll("%error%", var3.getLocalizedMessage()));
         }
      }
   }

   @EventHandler
   public void click(final InventoryClickEvent e) throws SQLException {
      if (ChatColor.stripColor(e.getInventory().getName()).equals(ChatColor.stripColor(Lang.getMessage("settings-book"))) && e.getCurrentItem() != null) {
         e.setCancelled(true);
         if (e.getSlot() == 38 && DragonEscape.getInstance().getGameManager().hasGame((Player)e.getWhoClicked())) {
            Game g = DragonEscape.getInstance().getGameManager().getGame((Player)e.getWhoClicked());
            e.getWhoClicked()
               .openInventory(
                  InventoryUtils.getMapLeaderboard(
                     (Player)e.getWhoClicked(), g.getArenaName(), g.isSolo(), KitManager.getKit((Player)e.getWhoClicked()).getKitName(), 1
                  )
               );
         } else if (e.getSlot() == 42 && e.getCurrentItem() != null) {
            final Game g;
            if ((g = DragonEscape.getInstance().getGameManager().getGame((Player)e.getWhoClicked())) != null) {
               final String arenaName = g.getArenaName();
               g.removePlayer((Player)e.getWhoClicked());
               Bukkit.getScheduler().runTaskLater(DragonEscape.getInstance(), new Runnable() {
                  @Override
                  public void run() {
                     if (g instanceof PracticeGame) {
                        try {
                           DragonEscape.getInstance().getGameManager().getSoloGame(arenaName, (Player)e.getWhoClicked());
                        } catch (WorldEditException var2) {
                           var2.printStackTrace();
                        } catch (IOException var3) {
                           var3.printStackTrace();
                        }
                     } else if (g.isSolo()) {
                        if (KitManager.getKit((Player)e.getWhoClicked()).isBreaksBlocks()) {
                           KitManager.setKit((Player)e.getWhoClicked(), KitManager.getKit("leap"));
                        }

                        new PracticeGame((Player)e.getWhoClicked(), KitManager.getKit((Player)e.getWhoClicked()), arenaName);
                     }
                  }
               }, 10L);
            }
         } else {
            if (this.isEnabled(e.getCurrentItem())) {
               e.getInventory().setItem(e.getSlot(), InventoryUtils.getDisabledItem());
            } else if (this.isDisabled(e.getCurrentItem())) {
               e.getInventory().setItem(e.getSlot(), InventoryUtils.getEnabledItem());
            }
         }
      }
   }

   private boolean isEnabled(ItemStack itemStack) {
      return itemStack.getData().getData() == 10;
   }

   private boolean isDisabled(ItemStack itemStack) {
      return itemStack.getData().getData() == 8;
   }

   private boolean isRightArrow(ItemStack itemStack) {
      return itemStack.getItemMeta() != null
         && itemStack.getItemMeta().getDisplayName() != null
         && itemStack.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Next Page");
   }

   private boolean isLeftArrow(ItemStack itemStack) {
      return itemStack.getItemMeta() != null
         && itemStack.getItemMeta().getDisplayName() != null
         && itemStack.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Previous Page");
   }
}
