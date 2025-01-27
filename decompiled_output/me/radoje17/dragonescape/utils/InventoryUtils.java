package me.radoje17.dragonescape.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import me.radoje17.dragonescape.DeSettings;
import me.radoje17.dragonescape.DragonEscape;
import me.radoje17.dragonescape.Game;
import me.radoje17.dragonescape.Lang;
import me.radoje17.dragonescape.MySQLConnector;
import me.radoje17.dragonescape.NBTEditor;
import me.radoje17.dragonescape.PlayerCountHologram;
import me.radoje17.dragonescape.PracticeGame;
import me.radoje17.dragonescape.kits.Kit;
import me.radoje17.dragonescape.kits.KitManager;
import me.radoje17.dragonescape.kits.KitType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class InventoryUtils {
   private static Inventory activeGamesInventory = Bukkit.createInventory(null, 54, "Dragon Escape");
   private static Inventory inProgressGamesInventory = Bukkit.createInventory(null, 54, "Dragon Escape in progress");
   private static ItemStack restartItem = new ItemStack(Material.SUGAR);
   private static ItemStack quitItem = new ItemStack(Material.MAGMA_CREAM, 1);
   private static ItemStack enabled = new ItemStack(Material.INK_SACK, 1, (short)10);
   private static ItemStack disabled = new ItemStack(Material.INK_SACK, 1, (short)8);
   private static ItemStack rightArrow = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
   private static ItemStack leftArrow = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
   private static ItemStack settingsBook = new ItemStack(Material.BOOK, 1);

   public static ItemStack getSettingsBook() {
      return settingsBook;
   }

   public static Inventory getMapsInventory(int page, boolean solo, boolean practice) {
      List<String> maps = ArenaUtils.getArenasSorted();
      Inventory inv = Bukkit.createInventory(null, 54, "Create " + (practice ? "Practice" : (solo ? "Solo" : "Public")) + " Game (Page #" + (page + 1) + ")");
      int end = 36;
      int i = 0;
      ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)3);
      ItemMeta glassMeta = glass.getItemMeta();
      glassMeta.setDisplayName(" ");
      glass.setItemMeta(glassMeta);
      if (page == 0) {
         i = 18;
         end = 45;

         for (int j = 9; j < 18; j++) {
            inv.setItem(j, glass);
         }
      } else {
         for (int j = 36; j < 45; j++) {
            inv.setItem(j, glass);
         }
      }

      for (int count = page * 27; i < end && maps.size() > count; count++) {
         String map = maps.get(count);
         ItemStack mapItem = new ItemStack(Material.PAPER, 1);
         ItemMeta mapItemMeta = mapItem.getItemMeta();
         mapItemMeta.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD + ChatColor.UNDERLINE + ArenaUtils.getArenaName(map));
         List<String> lore = new ArrayList<>();
         lore.add("");
         lore.add(ChatColor.YELLOW + "Slots: " + ChatColor.WHITE + ArenaUtils.getSpawnpoints(map, 0).size());
         lore.add(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + ArenaUtils.getAuthor(map));
         mapItemMeta.setLore(lore);
         mapItem.setItemMeta(mapItemMeta);
         if (ArenaUtils.isSpecial(map)) {
            mapItem.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
            mapItem = NBTEditor.set(mapItem, 1, "HideFlags");
         }

         inv.setItem(i, mapItem);
         i++;
      }

      if (maps.size() > (page + 1) * 36) {
         inv.setItem(53, rightArrow);
      }

      if (page > 0) {
         inv.setItem(45, leftArrow);
      }

      return inv;
   }

   public static Inventory getSettingsInventory(Player p) throws SQLException {
      Inventory inv = Bukkit.createInventory(null, 54, Lang.getMessage("settings-book"));
      DeSettings settings = DragonEscape.getInstance().getGameManager().getDeSettings(p);
      ItemStack joinMessages = new ItemStack(Material.EMPTY_MAP, 1);
      ItemMeta joinMessageMeta = joinMessages.getItemMeta();
      joinMessageMeta.setDisplayName(Lang.getMessage("toggle-join-messages"));
      joinMessageMeta.setLore(Lang.getList("toggle-join-messages-lore"));
      joinMessages.setItemMeta(joinMessageMeta);
      inv.setItem(0, joinMessages);
      inv.setItem(9, settings.sendJoinMessages() ? enabled : disabled);
      ItemStack rewardMessages = new ItemStack(Material.EMPTY_MAP, 1);
      ItemMeta rewardMessagesMeta = rewardMessages.getItemMeta();
      rewardMessagesMeta.setDisplayName(Lang.getMessage("toggle-reward-messages"));
      rewardMessagesMeta.setLore(Lang.getList("toggle-reward-messages-lore"));
      rewardMessages.setItemMeta(rewardMessagesMeta);
      inv.setItem(1, rewardMessages);
      inv.setItem(10, settings.sendRewardMessages() ? enabled : disabled);
      ItemStack resetMessages = new ItemStack(Material.EMPTY_MAP, 1);
      ItemMeta resetMessagesLore = resetMessages.getItemMeta();
      resetMessagesLore.setDisplayName(Lang.getMessage("toggle-reset-messages"));
      resetMessagesLore.setLore(Lang.getList("toggle-reset-messages-lore"));
      resetMessages.setItemMeta(resetMessagesLore);
      inv.setItem(2, resetMessages);
      inv.setItem(11, settings.sendResetMessages() ? enabled : disabled);
      ItemStack displayTimer = new ItemStack(Material.WATCH, 1);
      ItemMeta displayTimerLore = displayTimer.getItemMeta();
      displayTimerLore.setDisplayName(Lang.getMessage("display-timer"));
      displayTimerLore.setLore(Lang.getList("toggle-display-timer-lore"));
      displayTimer.setItemMeta(displayTimerLore);
      inv.setItem(5, displayTimer);
      inv.setItem(14, settings.displayTimer() ? enabled : disabled);
      ItemStack playerVisibility = new ItemStack(Material.SLIME_BALL, 1);
      ItemMeta playerVisibilityMeta = playerVisibility.getItemMeta();
      playerVisibilityMeta.setDisplayName(Lang.getMessage("player-visibility"));
      playerVisibilityMeta.setLore(Lang.getList("toggle-player-visibility-lore"));
      playerVisibility.setItemMeta(playerVisibilityMeta);
      inv.setItem(6, playerVisibility);
      inv.setItem(15, settings.showPlayers() ? enabled : disabled);
      ItemStack autoRejoin = new ItemStack(Material.IRON_TRAPDOOR, 1);
      ItemMeta autoRejoinMeta = autoRejoin.getItemMeta();
      autoRejoinMeta.setDisplayName(Lang.getMessage("auto-rejoin"));
      autoRejoinMeta.setLore(Lang.getList("auto-rejoin-lore"));
      autoRejoin.setItemMeta(autoRejoinMeta);
      inv.setItem(7, autoRejoin);
      inv.setItem(16, settings.autoRejoin() ? enabled : disabled);
      ItemStack safetyMode = new ItemStack(Material.BLAZE_POWDER, 1);
      ItemMeta safetyModeLore = safetyMode.getItemMeta();
      safetyModeLore.setDisplayName(Lang.getMessage("safety-mode"));
      safetyModeLore.setLore(Lang.getList("toggle-safety-mode-lore"));
      safetyMode.setItemMeta(safetyModeLore);
      inv.setItem(8, safetyMode);
      inv.setItem(17, settings.safetyMode() ? enabled : disabled);
      Game g = null;
      if (DragonEscape.getInstance().getGameManager().hasGame(p)) {
         g = DragonEscape.getInstance().getGameManager().getGame(p);
         ItemStack leaderboard = new ItemStack(Material.BOOK, 1);
         ItemMeta leaderboardMeta = leaderboard.getItemMeta();
         leaderboardMeta.setDisplayName(Lang.getMessage("leaderboard-item"));
         leaderboard.setItemMeta(leaderboardMeta);
         inv.setItem(38, leaderboard);
      }

      if (g != null && (g.isSolo() || g instanceof PracticeGame)) {
         ItemStack switchToDifferentMode = new ItemStack(Material.SIGN, 1);
         ItemMeta switchMeta = switchToDifferentMode.getItemMeta();
         if (g.isSolo()) {
            switchMeta.setDisplayName(Lang.getMessage("switch-to-practice"));
         } else {
            switchMeta.setDisplayName(Lang.getMessage("switch-to-solo"));
         }

         switchToDifferentMode.setItemMeta(switchMeta);
         inv.setItem(42, switchToDifferentMode);
      }

      return inv;
   }

   public static ItemStack getEnabledItem() {
      return enabled;
   }

   public static ItemStack getDisabledItem() {
      return disabled;
   }

   public static Inventory getKitSelectMenu(Player p, boolean solo) {
      Kit selectedKit = KitManager.getKit(p);
      Inventory inv = Bukkit.createInventory(null, 27, "Select kit");
      int[] slots = new int[]{12, 11, 15, 13, 10, 16, 14};
      Iterator i = Arrays.stream(slots).iterator();

      for (String s : KitManager.getKits().keySet()) {
         Kit k = KitManager.getKits().get(s);
         if ((k.isPublicFriendly() || solo) && (k.isSoloFriendly() || !solo)) {
            ItemStack is = k.getMenuItem();
            if (!ChatColor.stripColor(is.getItemMeta().getDisplayName()).equalsIgnoreCase("none")) {
               ItemStack copy = new ItemStack(is);
               ItemMeta copyMeta = copy.getItemMeta();
               List<String> lore = copyMeta.getLore();
               if (lore == null) {
                  lore = new ArrayList<>();
               }

               if (selectedKit == null
                  || !selectedKit.getKitName().equalsIgnoreCase(ChatColor.stripColor(copyMeta.getDisplayName()))
                     && (!"Parkour".equals(ChatColor.stripColor(copyMeta.getDisplayName())) || selectedKit.getType() != KitType.NONE)) {
                  lore.add("" + ChatColor.UNDERLINE + ChatColor.BOLD + ChatColor.GRAY + "Click to select this kit");
               } else {
                  lore.add(ChatColor.GREEN + "Selected");
               }

               copyMeta.setLore(lore);
               copy.setItemMeta(copyMeta);
               inv.setItem((Integer)i.next(), copy);
            }
         }
      }

      return inv;
   }

   public static Inventory getPracticeKitSelectMenu(Player p) {
      Kit selectedKit = KitManager.getKit(p);
      Inventory inv = Bukkit.createInventory(null, 27, "Select kit");
      int[] slots = new int[]{12, 11, 15, 13, 10, 16, 14};
      Iterator i = Arrays.stream(slots).iterator();

      for (String s : KitManager.getKits().keySet()) {
         Kit k = KitManager.getKits().get(s);
         if (k.isPracticeFriendly()) {
            ItemStack is = k.getMenuItem();
            if (!ChatColor.stripColor(is.getItemMeta().getDisplayName()).equalsIgnoreCase("none")) {
               ItemStack copy = new ItemStack(is);
               ItemMeta copyMeta = copy.getItemMeta();
               List<String> lore = copyMeta.getLore();
               if (lore == null) {
                  lore = new ArrayList<>();
               }

               if (selectedKit == null
                  || !selectedKit.getKitName().equalsIgnoreCase(ChatColor.stripColor(copyMeta.getDisplayName()))
                     && (!"Parkour".equals(ChatColor.stripColor(copyMeta.getDisplayName())) || selectedKit.getType() != KitType.NONE)) {
                  lore.add("" + ChatColor.UNDERLINE + ChatColor.BOLD + ChatColor.GRAY + "Click to select this kit");
               } else {
                  lore.add(ChatColor.GREEN + "Selected");
               }

               copyMeta.setLore(lore);
               copy.setItemMeta(copyMeta);
               inv.setItem((Integer)i.next(), copy);
            }
         }
      }

      return inv;
   }

   public static Inventory voteInventory(Player p, Game g) {
      Inventory inv = Bukkit.createInventory(null, 27, Lang.getMessage("speed-vote-inventory"));
      HashMap<Player, Integer> votes = g.getVotes();
      int easyVotes = Collections.frequency(votes.values(), 0);
      int mediumVotes = Collections.frequency(votes.values(), 1);
      int hardVotes = Collections.frequency(votes.values(), 2);
      ItemStack easy = new ItemStack(Material.WOOL, 1, (short)5);
      ItemMeta easyMeta = easy.getItemMeta();
      List<String> easyLore = Lang.getList("easy-lore");
      easyLore.add(easyVotes == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + easyVotes));
      if (votes.containsKey(p) && votes.get(p) == 0) {
         easyLore.add(Lang.getMessage("you-voted"));
      }

      easyMeta.setLore(easyLore);
      easyMeta.setDisplayName(Lang.getMessage("easy"));
      easy.setItemMeta(easyMeta);
      inv.setItem(11, easy);
      ItemStack medium = new ItemStack(Material.WOOL, 1, (short)1);
      ItemMeta mediumMeta = medium.getItemMeta();
      List<String> mediumLore = Lang.getList("medium-lore");
      mediumLore.add(mediumVotes == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + mediumVotes));
      if (votes.containsKey(p) && votes.get(p) == 1) {
         mediumLore.add(Lang.getMessage("you-voted"));
      }

      mediumMeta.setLore(mediumLore);
      mediumMeta.setDisplayName(Lang.getMessage("medium"));
      medium.setItemMeta(mediumMeta);
      inv.setItem(13, medium);
      ItemStack hard = new ItemStack(Material.WOOL, 1, (short)14);
      ItemMeta hardMeta = hard.getItemMeta();
      List<String> hardLore = Lang.getList("hard-lore");
      hardLore.add(hardVotes == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + hardVotes));
      if (votes.containsKey(p) && votes.get(p) == 2) {
         hardLore.add(Lang.getMessage("you-voted"));
      }

      hardMeta.setLore(hardLore);
      hardMeta.setDisplayName(Lang.getMessage("hard"));
      hard.setItemMeta(hardMeta);
      inv.setItem(15, hard);
      return inv;
   }

   public static boolean isVotingInventoryOpen(Player p) {
      return p.getOpenInventory().getTopInventory() != null && p.getOpenInventory().getTopInventory().getName().equals(Lang.getMessage("speed-vote-inventory"))
         || p.getOpenInventory().getTopInventory().getName().equals(Lang.getMessage("maps-vote-inventory"));
   }

   public static Inventory mapsVoteInventory(Player p, Game g) {
      Inventory inv = Bukkit.createInventory(null, 45, Lang.getMessage("speed-vote-inventory"));
      HashMap<Player, Integer> votes = g.getVotes();
      int easyVotes = Collections.frequency(votes.values(), 0);
      int mediumVotes = Collections.frequency(votes.values(), 1);
      int hardVotes = Collections.frequency(votes.values(), 2);
      ItemStack easy = new ItemStack(Material.WOOL, 1, (short)5);
      ItemMeta easyMeta = easy.getItemMeta();
      List<String> easyLore = Lang.getList("easy-lore");
      easyLore.add(easyVotes == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + easyVotes));
      if (votes.containsKey(p) && votes.get(p) == 0) {
         easyLore.add(Lang.getMessage("you-voted"));
      }

      easyMeta.setLore(easyLore);
      easyMeta.setDisplayName(Lang.getMessage("easy"));
      easy.setItemMeta(easyMeta);
      inv.setItem(11, easy);
      ItemStack medium = new ItemStack(Material.WOOL, 1, (short)1);
      ItemMeta mediumMeta = medium.getItemMeta();
      List<String> mediumLore = Lang.getList("medium-lore");
      mediumLore.add(mediumVotes == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + mediumVotes));
      if (votes.containsKey(p) && votes.get(p) == 1) {
         mediumLore.add(Lang.getMessage("you-voted"));
      }

      mediumMeta.setLore(mediumLore);
      mediumMeta.setDisplayName(Lang.getMessage("medium"));
      medium.setItemMeta(mediumMeta);
      inv.setItem(13, medium);
      ItemStack hard = new ItemStack(Material.WOOL, 1, (short)14);
      ItemMeta hardMeta = hard.getItemMeta();
      List<String> hardLore = Lang.getList("hard-lore");
      hardLore.add(hardVotes == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + hardVotes));
      if (votes.containsKey(p) && votes.get(p) == 2) {
         hardLore.add(Lang.getMessage("you-voted"));
      }

      hardMeta.setLore(hardLore);
      hardMeta.setDisplayName(Lang.getMessage("hard"));
      hard.setItemMeta(hardMeta);
      inv.setItem(15, hard);
      HashMap<Player, Integer> mapVotesMap = g.getMapVotes();
      int[] mapVotes = new int[]{
         Collections.frequency(mapVotesMap.values(), 0), Collections.frequency(mapVotesMap.values(), 1), Collections.frequency(mapVotesMap.values(), 2)
      };

      for (int i = 0; i < 3; i++) {
         ItemStack item = new ItemStack(Material.PAPER, 1);
         ItemMeta itemMeta = hard.getItemMeta();
         List<String> itemLore = new ArrayList<>();
         int glasovi = mapVotes[i];
         itemLore.add(glasovi == 1 ? Lang.getMessage("vote") : Lang.getMessage("votes").replaceAll("%count%", "" + glasovi));
         if (g.getMapVotes().containsKey(p) && g.getMapVotes().get(p) == i) {
            itemLore.add(Lang.getMessage("you-voted"));
         }

         itemMeta.setLore(itemLore);
         itemMeta.setDisplayName(ChatColor.WHITE + ArenaUtils.getArenaName(g.getArenaChoice()[i]));
         item.setItemMeta(itemMeta);
         inv.setItem(28 + i * 3, item);
      }

      return inv;
   }

   public static Inventory getActiveGamesInventory() {
      return activeGamesInventory;
   }

   public static Inventory getInProgressGamesInventory() {
      return inProgressGamesInventory;
   }

   public static void updateInventories() {
      updateGamesInProgressInventory();
      updateActiveGamesInventory();
      PlayerCountHologram.update();
   }

   private static void updateGamesInProgressInventory() {
      inProgressGamesInventory.clear();
      int available = 0;
      int i = 10;

      for (Game g : DragonEscape.getInstance().getGameManager().getSortedGames()) {
         if (!g.isActive()) {
            available++;
         } else {
            ItemStack gameItem = new ItemStack(Material.REDSTONE_BLOCK, g.getPlayers().size());
            gameItem = NBTEditor.set(gameItem, g.getGameID(), "gameID");
            ItemMeta gameItemMeta = gameItem.getItemMeta();
            gameItemMeta.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD + ChatColor.UNDERLINE + "Dragon Escape " + g.getGameID());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "Game: " + ChatColor.WHITE + "Dragon Escape");
            lore.add(ChatColor.YELLOW + "Map: " + ChatColor.WHITE + g.getArenaName());
            lore.add(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + g.getPlayers().size() + "/" + g.slots());
            lore.add("");
            lore.add(ChatColor.RED + "Game is in progress");
            if (g.slots() > g.getPlayers().size()) {
               lore.add("" + ChatColor.WHITE + ChatColor.BOLD + ChatColor.UNDERLINE + "Click to Spectate");
            } else {
               lore.add("" + ChatColor.RED + ChatColor.UNDERLINE + "Game is full");
            }

            gameItemMeta.setLore(lore);
            gameItem.setItemMeta(gameItemMeta);
            inProgressGamesInventory.setItem(i, gameItem);
            i++;
            if (i == 17 || i == 26) {
               i += 2;
            }

            if (i > 34) {
               break;
            }
         }
      }

      ItemStack activeGames = new ItemStack(Material.EMERALD_BLOCK, 1);
      ItemMeta activeGamesMeta = activeGames.getItemMeta();
      activeGamesMeta.setDisplayName("" + ChatColor.GREEN + available + " game" + (available != 1 ? "s" : "") + " available");
      List<String> lorex = new ArrayList<>();
      lorex.add(" ");
      lorex.add("" + ChatColor.WHITE + ChatColor.BOLD + ChatColor.UNDERLINE + "Click to See");
      activeGamesMeta.setLore(lorex);
      activeGames.setItemMeta(activeGamesMeta);
      inProgressGamesInventory.setItem(49, activeGames);
   }

   public static Inventory getChooseModeLeaderboardInventory(String arenaName) {
      arenaName = ArenaUtils.getArenaName(arenaName) + " Leaderboard";
      Inventory inv = Bukkit.createInventory(null, 27, arenaName);
      ItemStack soloItem = new ItemStack(Material.MAP, 1);
      ItemMeta soloMeta = soloItem.getItemMeta();
      soloMeta.setDisplayName(ChatColor.WHITE + "Solo Leaderboard");
      soloItem.setItemMeta(soloMeta);
      ItemStack publicItem = new ItemStack(Material.MAP, 1);
      ItemMeta publicMeta = publicItem.getItemMeta();
      publicMeta.setDisplayName(ChatColor.WHITE + "Public Leaderboard");
      publicItem.setItemMeta(publicMeta);
      inv.setItem(10, soloItem);
      inv.setItem(16, publicItem);
      return inv;
   }

   public static Inventory getChooseKitLeaderboardInventory(String arenaName, boolean solo) {
      Inventory inv = Bukkit.createInventory(null, 27, arenaName + " " + (solo ? "Solo" : "Public") + " Leaderboard");

      for (String s : KitManager.getKits().keySet()) {
         Kit k = KitManager.getKits().get(s);
         if ((!solo || k.isSoloFriendly()) && (solo || k.isPublicFriendly())) {
            ItemStack is = k.getMenuItem();
            inv.addItem(new ItemStack[]{is});
         }
      }

      return inv;
   }

   public static Inventory getMapLeaderboard(Player p, String arenaName, boolean solo, String kit, int page) throws SQLException {
      arenaName = ChatColor.stripColor(arenaName.toLowerCase());
      Inventory inv = Bukkit.createInventory(null, 54, "§fLeaderboard (Page #" + page + ")");
      ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1);
      ItemMeta meta = glass.getItemMeta();
      meta.setDisplayName(" ");
      glass.setItemMeta(meta);
      ItemStack map = new ItemStack(Material.MAP);
      ItemMeta mapMeta = map.getItemMeta();
      mapMeta.setDisplayName("§f" + ArenaUtils.getArenaName(arenaName));
      List<String> mapLore = new ArrayList<>();
      mapLore.add(ChatColor.GRAY + "Mode: " + ChatColor.GREEN + (solo ? "solo" : "public"));
      mapLore.add(ChatColor.GRAY + "Kit: " + ChatColor.YELLOW + kit);
      mapMeta.setLore(mapLore);
      map.setItemMeta(mapMeta);
      inv.setItem(0, map);
      inv.setItem(1, glass);
      inv.setItem(3, glass);
      inv.setItem(5, glass);
      inv.setItem(7, glass);
      inv.setItem(8, glass);
      inv.setItem(9, glass);
      inv.setItem(10, glass);
      inv.setItem(12, glass);
      inv.setItem(14, glass);
      inv.setItem(16, glass);
      inv.setItem(17, glass);
      inv.setItem(18, glass);
      inv.setItem(19, glass);
      inv.setItem(21, glass);
      inv.setItem(23, glass);
      inv.setItem(25, glass);
      inv.setItem(26, glass);
      inv.setItem(27, glass);
      inv.setItem(28, glass);
      inv.setItem(30, glass);
      inv.setItem(32, glass);
      inv.setItem(34, glass);
      inv.setItem(35, glass);
      inv.setItem(36, glass);
      inv.setItem(37, glass);
      inv.setItem(39, glass);
      inv.setItem(41, glass);
      inv.setItem(43, glass);
      inv.setItem(44, glass);
      inv.setItem(45, glass);
      inv.setItem(46, glass);
      inv.setItem(48, glass);
      inv.setItem(50, glass);
      inv.setItem(52, glass);
      inv.setItem(53, glass);
      if (page != 1) {
         inv.setItem(45, getLeftArrow());
      } else {
         ItemStack goldenGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)4);
         goldenGlass.setItemMeta(meta);
         inv.setItem(1, goldenGlass);
         ItemStack silverGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)7);
         silverGlass.setItemMeta(meta);
         inv.setItem(10, silverGlass);
         ItemStack bronzeGlass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)12);
         bronzeGlass.setItemMeta(meta);
         inv.setItem(19, bronzeGlass);
      }

      String tableName = "de_times";
      if (solo) {
         tableName = tableName + "_solo";
      }

      String query = "select (realName(uuid)) AS playerName, MIN(time) AS time, runID, approved, (realName(disabledBy)) as disabledBy, (SELECT COUNT(*) FROM de_reports"
         + (solo ? "_solo" : "")
         + " WHERE runID = de_times"
         + (solo ? "_solo" : "")
         + ".runID) AS reportCount from "
         + tableName
         + " WHERE disabled = 0 AND arenaName='"
         + arenaName
         + "' GROUP BY playerName ORDER BY time, date ASC LIMIT "
         + (page - 1) * 18
         + ", 19;";
      if (kit != null) {
         query = "select (realName(uuid)) AS playerName, MIN(time) AS time, runID, approved, (realName(disabledBy)) as disabledBy, (SELECT COUNT(*) FROM de_reports"
            + (solo ? "_solo" : "")
            + " WHERE runID = de_times"
            + (solo ? "_solo" : "")
            + ".runID) AS reportCount from "
            + tableName
            + " WHERE disabled = 0 AND arenaName='"
            + arenaName
            + "' AND kit='"
            + kit
            + "' GROUP BY playerName ORDER BY time, date ASC LIMIT "
            + (page - 1) * 18
            + ", 19;";
      }

      ResultSet set = MySQLConnector.prepareStatement(query).executeQuery();
      ArrayList<Integer> headOrder = new ArrayList<>();
      headOrder.add(2);
      headOrder.add(11);
      headOrder.add(20);
      headOrder.add(29);
      headOrder.add(38);
      headOrder.add(47);
      headOrder.add(4);
      headOrder.add(13);
      headOrder.add(22);
      headOrder.add(31);
      headOrder.add(40);
      headOrder.add(49);
      headOrder.add(6);
      headOrder.add(15);
      headOrder.add(24);
      headOrder.add(33);
      headOrder.add(42);
      headOrder.add(51);
      int red = 0;
      int dodato = 0;
      List<Integer> reports = MySQLConnector.getReports(p, solo);

      while (dodato < 18 && set.next()) {
         dodato++;
         String player = set.getString(1);
         long time = set.getLong(2);
         ItemStack glava = new ItemStack(Material.SKULL_ITEM, 1, (short)SkullType.PLAYER.ordinal());
         SkullMeta glavaMeta = (SkullMeta)glava.getItemMeta();
         if (dodato == 1) {
            glavaMeta.setDisplayName("§f" + player + " §e[" + TimeUtils.formatTime(time) + "]");
         } else if (dodato == 2) {
            glavaMeta.setDisplayName("§f" + player + " §7[" + TimeUtils.formatTime(time) + "]");
         } else if (dodato == 3) {
            glavaMeta.setDisplayName("§f" + player + " §6[" + TimeUtils.formatTime(time) + "]");
         } else {
            glavaMeta.setDisplayName("§f" + player + " §b[" + TimeUtils.formatTime(time) + "]");
         }

         glavaMeta.setOwner(player);
         List<String> lore = new ArrayList<>();
         lore.add(" ");
         if (p.isOp()) {
            if (set.getInt(4) == 1) {
               lore.add(Lang.getMessage("approved-run").replaceAll("%player%", set.getString(5)));
            } else {
               if (set.getInt(6) > 0) {
                  lore.add(
                     Lang.getMessage("report-run-count")
                        .replaceAll("%amount%", "" + set.getInt(6))
                        .replaceAll("%reports%", set.getInt(6) != 1 ? "reports" : "report")
                  );
               }

               lore.add(Lang.getMessage("approve-run"));
               lore.add(Lang.getMessage("remove-run"));
            }
         } else if (set.getInt(4) == 1) {
            lore.add(Lang.getMessage("approved-run").replaceAll("%player%", set.getString(5)));
         } else if (reports.contains(set.getInt(3))) {
            if (set.getInt(6) > 1) {
               lore.add(
                  Lang.getMessage("reported-run-count")
                     .replaceAll("%amount%", "" + (set.getInt(6) - 1))
                     .replaceAll("%others%", set.getInt(6) - 1 != 1 ? "others" : "other")
               );
            } else {
               lore.add(Lang.getMessage("reported-run"));
            }
         } else {
            if (set.getInt(6) > 0) {
               lore.add(
                  Lang.getMessage("report-run-count")
                     .replaceAll("%amount%", "" + set.getInt(6))
                     .replaceAll("%reports%", set.getInt(6) > 1 ? "reports" : "report")
               );
            } else {
               lore.add(Lang.getMessage("no-reports"));
            }

            lore.add(Lang.getMessage("report-run"));
         }

         lore.add(" ");
         glavaMeta.setLore(lore);
         glava.setItemMeta(glavaMeta);
         inv.setItem(headOrder.get(0), NBTEditor.set(glava, set.getInt(3), "runID"));
         headOrder.remove(0);
      }

      if (set.next()) {
         inv.setItem(53, getRightArrow());
      }

      ItemStack noArena = new ItemStack(Material.BARRIER, 1);
      ItemMeta noArenaMeta = noArena.getItemMeta();
      noArenaMeta.setDisplayName(" ");
      noArena.setItemMeta(noArenaMeta);

      for (int i = 0; i < 18 - dodato; i++) {
         inv.setItem(inv.firstEmpty(), noArena);
      }

      return inv;
   }

   private static void updateActiveGamesInventory() {
      activeGamesInventory.clear();
      int inProgress = 0;
      int i = 28;
      int j = 0;

      for (Game g : DragonEscape.getInstance().getGameManager().getSortedGames()) {
         j++;
         if (g.slots() != 1) {
            if (g.isActive()) {
               inProgress++;
            } else {
               ItemStack gameItem = new ItemStack(Material.EMERALD_BLOCK, g.getPlayers().size());
               ItemMeta gameItemMeta = gameItem.getItemMeta();
               gameItemMeta.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD + ChatColor.UNDERLINE + "Dragon Escape " + g.getGameID());
               List<String> lore = new ArrayList<>();
               lore.add("");
               lore.add(ChatColor.YELLOW + "Game: " + ChatColor.WHITE + "Dragon Escape");
               lore.add(ChatColor.YELLOW + "Map: " + ChatColor.WHITE + g.getArenaName());
               lore.add(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + g.getPlayers().size() + "/" + g.slots());
               lore.add("");
               if (g.isTimerStarted()) {
                  lore.add(ChatColor.YELLOW + "Starting in " + g.getTimer() + " seconds");
               } else {
                  lore.add(ChatColor.YELLOW + "Waiting for players");
               }

               if (g.slots() >= g.getPlayers().size()) {
                  lore.add("" + ChatColor.WHITE + ChatColor.BOLD + ChatColor.UNDERLINE + "Click to Join");
               } else {
                  lore.add("" + ChatColor.RED + ChatColor.UNDERLINE + "Game is full");
                  gameItem.setType(Material.REDSTONE_BLOCK);
               }

               gameItemMeta.setLore(lore);
               gameItem.setItemMeta(gameItemMeta);
               activeGamesInventory.setItem(i, gameItem);
               if (++i == 35) {
                  i += 2;
               }

               if (i > 43) {
                  break;
               }
            }
         }
      }

      ItemStack inProgressGames = new ItemStack(Material.STAINED_CLAY, 1, (short)14);
      ItemMeta inProgressMeta = inProgressGames.getItemMeta();
      inProgressMeta.setDisplayName("" + ChatColor.RED + inProgress + " game" + (inProgress != 1 ? "s" : "") + " in progress");
      List<String> lorex = new ArrayList<>();
      lorex.add(" ");
      lorex.add("" + ChatColor.WHITE + ChatColor.BOLD + ChatColor.UNDERLINE + "Click to See");
      inProgressMeta.setLore(lorex);
      inProgressGames.setItemMeta(inProgressMeta);
      activeGamesInventory.setItem(14, inProgressGames);
      ItemStack selectMap = new ItemStack(Material.MAP, 1);
      ItemMeta selectMapMeta = selectMap.getItemMeta();
      selectMapMeta.setDisplayName(ChatColor.WHITE + "Create a game");
      lorex.clear();
      lorex.add(" ");
      lorex.add("" + ChatColor.WHITE + ChatColor.BOLD + ChatColor.UNDERLINE + "Click to Create a Game");
      selectMapMeta.setLore(lorex);
      selectMap.setItemMeta(selectMapMeta);
      activeGamesInventory.setItem(12, selectMap);
   }

   public static Inventory getReportsInventory(int page, boolean solo) {
      Inventory inv = null;

      try {
         String reportsTable;
         String runsTable;
         if (solo) {
            reportsTable = "de_reports_solo";
            runsTable = "de_times_solo";
         } else {
            reportsTable = "de_reports";
            runsTable = "de_times";
         }

         ResultSet set = MySQLConnector.prepareStatement(
               "SELECT "
                  + reportsTable
                  + ".runID, COUNT(*) AS reportCount, realName("
                  + runsTable
                  + ".uuid) AS player, time, arenaName, kit, "
                  + runsTable
                  + ".date FROM "
                  + reportsTable
                  + " INNER JOIN "
                  + runsTable
                  + " ON "
                  + reportsTable
                  + ".runID = "
                  + runsTable
                  + ".runID WHERE "
                  + reportsTable
                  + ".disabled = 0 AND "
                  + runsTable
                  + ".disabled = 0 AND approved = 0 GROUP BY runID ORDER BY reportCount desc LIMIT "
                  + page * 45
                  + ",46;"
            )
            .executeQuery();
         int size = 0;
         if (set.last()) {
            size = set.getRow();
            set.beforeFirst();
         }

         int rows = Math.min(6, size / 9 + (size % 9 != 0 ? 1 : 0) + 1);
         inv = Bukkit.createInventory(null, rows * 9, (solo ? "Solo" : "Public") + " Reports (Page #" + (page + 1) + ")");

         for (int i = 0; set.next() && i < (rows - 1) * 9; i++) {
            int personalBestID = set.getInt(1);
            int count = set.getInt(2);
            String name = set.getString(3);
            long time = set.getLong(4);
            String arenaName = set.getString(5);
            String kit = set.getString(6);
            long date = set.getLong(7);
            ItemStack reported = new ItemStack(Material.LEATHER_BOOTS, 1);
            ItemMeta reportedItemMeta = reported.getItemMeta();
            reportedItemMeta.setDisplayName(ChatColor.YELLOW + TimeUtils.formatTime(time));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "by " + ChatColor.YELLOW + name);
            lore.add(ChatColor.GRAY + "on " + ChatColor.BLUE + arenaName);
            lore.add(ChatColor.GRAY + "kit " + ChatColor.YELLOW + kit);
            lore.add(" ");
            lore.add(ChatColor.GRAY + TimeUtils.formatDateTime(date));
            lore.add(" ");
            lore.add(ChatColor.GREEN + "Shift Left Click to approve this run");
            lore.add(ChatColor.RED + "Shift Right Click to remove this run");
            lore.add("" + ChatColor.YELLOW + count + ChatColor.GRAY + (count == 1 ? " report" : " reports"));
            reportedItemMeta.setLore(lore);
            reported.setItemMeta(reportedItemMeta);
            reported = NBTEditor.set(reported, personalBestID, "runID");
            inv.addItem(new ItemStack[]{reported});
         }

         ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1);
         ItemMeta glassMeta = glass.getItemMeta();
         glassMeta.setDisplayName(" ");
         glass.setItemMeta(glassMeta);

         for (int i = (rows - 1) * 9; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
         }

         ItemStack barrier = new ItemStack(Material.BARRIER, 1);
         ItemMeta barrierMeta = barrier.getItemMeta();
         barrierMeta.setDisplayName(" ");
         barrier.setItemMeta(barrierMeta);

         while (inv.firstEmpty() != -1) {
            inv.setItem(inv.firstEmpty(), barrier);
         }
      } catch (SQLException var21) {
         var21.printStackTrace();
      }

      return inv;
   }

   public static ItemStack getRestartItem() {
      return restartItem;
   }

   public static ItemStack getQuitItem() {
      return quitItem;
   }

   private static ItemStack getLeftArrow() {
      return leftArrow;
   }

   private static ItemStack getRightArrow() {
      return rightArrow;
   }

   static {
      ItemMeta enabledMeta = enabled.getItemMeta();
      enabledMeta.setDisplayName(Lang.getMessage("enabled"));
      enabled.setItemMeta(enabledMeta);
      ItemMeta disabledMeta = disabled.getItemMeta();
      disabledMeta.setDisplayName(Lang.getMessage("disabled"));
      disabled.setItemMeta(disabledMeta);
      ItemMeta restartMeta = restartItem.getItemMeta();
      restartMeta.setDisplayName(ChatColor.AQUA + "Restart");
      restartItem.setItemMeta(restartMeta);
      ItemMeta quitItemMeta = quitItem.getItemMeta();
      quitItemMeta.setDisplayName(ChatColor.RED + "Quit");
      quitItem.setItemMeta(quitItemMeta);
      SkullMeta rightArrowMeta = (SkullMeta)rightArrow.getItemMeta();
      rightArrowMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
      rightArrowMeta.setOwner("MHF_ArrowRight");
      rightArrow.setItemMeta(rightArrowMeta);
      SkullMeta leftArrowMeta = (SkullMeta)leftArrow.getItemMeta();
      leftArrowMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
      leftArrowMeta.setOwner("MHF_ArrowLeft");
      leftArrow.setItemMeta(leftArrowMeta);
      ItemMeta settingsBookMeta = settingsBook.getItemMeta();
      settingsBookMeta.setDisplayName(Lang.getMessage("settings-book"));
      settingsBook.setItemMeta(settingsBookMeta);
      updateInventories();
   }
}
