package org.Ave.jakisProjekt.database;

import org.Ave.jakisProjekt.main.PlayerData;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Database {
    private Connection conn;

    public void connect(String path) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users (uuid TEXT PRIMARY KEY, xp INTEGER, prestige INTEGER, speed_level INTEGER)");
        }
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignored) {}
    }

    public PlayerData loadPlayerSync(UUID uuid) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new PlayerData(rs.getInt("xp"), rs.getInt("prestige"), rs.getInt("speed_level"));
        } catch (SQLException e) { e.printStackTrace(); }
        return new PlayerData(0, 0, 0);
    }

    public void savePlayerSync(UUID uuid, PlayerData d) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO users(uuid,xp,prestige,speed_level) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET xp=?, prestige=?, speed_level=?")) {
            ps.setString(1, uuid.toString()); ps.setInt(2, d.xp); ps.setInt(3, d.prestige); ps.setInt(4, d.speedLevel);
            ps.setInt(5, d.xp); ps.setInt(6, d.prestige); ps.setInt(7, d.speedLevel);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}