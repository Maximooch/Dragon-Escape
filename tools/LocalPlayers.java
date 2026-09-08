import java.io.File;
import java.sql.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Supplies the player directory formerly maintained by the Parcade network. */
public final class LocalPlayers extends JavaPlugin implements Listener {
    private String url, user, password;
    public void onEnable() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File("plugins/DragonEscape/mysql.yml"));
        url = "jdbc:mysql://" + cfg.getString("mysql.hostname") + ":" + cfg.getInt("mysql.port")
            + "/" + cfg.getString("mysql.database") + "?useSSL=false&connectTimeout=5000&socketTimeout=5000";
        user = cfg.getString("mysql.username");
        password = cfg.getString("mysql.password");
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS pc_players (player_uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(16) NOT NULL)");
            s.executeUpdate("ALTER TABLE pc_players ADD COLUMN IF NOT EXISTS player_cc VARCHAR(2), ADD COLUMN IF NOT EXISTS discord_uuid VARCHAR(36), ADD COLUMN IF NOT EXISTS discord_code VARCHAR(16)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS cubics_data (uuid VARCHAR(36) PRIMARY KEY, cubics INT NOT NULL DEFAULT 2000, parkourXp INT NOT NULL DEFAULT 0)");
            s.executeUpdate("INSERT IGNORE INTO cubics_data(uuid) SELECT player_uuid FROM pc_players");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS de_stats (uuid VARCHAR(36) PRIMARY KEY, wins INT DEFAULT 0, 2nd INT DEFAULT 0, 3rd INT DEFAULT 0, losses INT DEFAULT 0, deaths INT DEFAULT 0, soloGames INT DEFAULT 0, blocksBroken INT DEFAULT 0, blocksPlaced INT DEFAULT 0)");
            for (String setting : new String[]{"joinMessages", "rewardMessages", "resetMessages", "displayTimer", "playerVisibility", "soundEffects", "safetyMode", "autoRejoin"}) {
                int enabled = setting.equals("safetyMode") || setting.equals("autoRejoin") ? 0 : 1;
                s.executeUpdate("ALTER TABLE de_stats ADD COLUMN IF NOT EXISTS " + setting + " TINYINT NOT NULL DEFAULT " + enabled);
            }
            s.executeUpdate("INSERT IGNORE INTO de_stats(uuid) SELECT player_uuid FROM pc_players");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS de_arenaStats (uuid VARCHAR(36), arenaName VARCHAR(64), joins INT DEFAULT 0, finishes INT DEFAULT 0, deaths INT DEFAULT 0, leaves INT DEFAULT 0, resets INT DEFAULT 0, playTime INT DEFAULT 0, PRIMARY KEY(uuid, arenaName))");
            for (String table : new String[]{"de_times", "de_times_solo"}) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (uuid VARCHAR(36), arenaName VARCHAR(64), time BIGINT, kit VARCHAR(16) DEFAULT 'leap', date BIGINT DEFAULT -1, approved TINYINT DEFAULT 0, disabled TINYINT DEFAULT 0, disabledBy VARCHAR(36), disabledDate BIGINT)");
                s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS runID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, MODIFY arenaName VARCHAR(64), MODIFY approved TINYINT DEFAULT 0, MODIFY disabled TINYINT DEFAULT 0");
            }
            for (String table : new String[]{"de_reports", "de_reports_solo"}) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (reportID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), runID INT NOT NULL, date BIGINT, disabled TINYINT DEFAULT 0, disabledBy VARCHAR(36), disabledDate BIGINT)");
                s.executeUpdate("ALTER TABLE " + table + " MODIFY disabled TINYINT DEFAULT 0");
            }
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("Local player directory and DragonEscape schema ready.");
        } catch (SQLException e) {
            getLogger().severe("Local database initialization failed: " + e.getMessage());
            getServer().shutdown();
        }
    }
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void login(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO pc_players(player_uuid,player_name) VALUES (?,?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name)")) {
            s.setString(1, event.getUniqueId().toString());
            s.setString(2, event.getName());
            s.executeUpdate();
            try (PreparedStatement currency = c.prepareStatement("INSERT IGNORE INTO cubics_data(uuid) VALUES (?)")) {
                currency.setString(1, event.getUniqueId().toString());
                currency.executeUpdate();
            }
            try (PreparedStatement stats = c.prepareStatement("INSERT IGNORE INTO de_stats(uuid) VALUES (?)")) {
                stats.setString(1, event.getUniqueId().toString());
                stats.executeUpdate();
            }
        } catch (SQLException e) {
            getLogger().warning("Cannot save local player: " + e.getMessage());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Local database unavailable; please try again.");
        }
    }
}
