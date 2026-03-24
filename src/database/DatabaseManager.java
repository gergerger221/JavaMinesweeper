package database;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DatabaseManager {
    private static DatabaseManager instance;

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".javaminesweeper";
    private static final String DB_PATH = APP_DIR + File.separator + "minesweeper.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;
    private static final String SQLITE_DRIVER_CLASS = "org.sqlite.JDBC";

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    private volatile String lastErrorMessage;

    private volatile boolean driverChecked;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        File dir = new File(APP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Ensure the sqlite-jdbc driver is loaded, otherwise DriverManager may not find
        // it in some IDE setups.
        ensureDriverLoaded();

        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");

                st.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL UNIQUE," +
                        "password_salt TEXT NOT NULL," +
                        "password_hash TEXT NOT NULL," +
                        "created_at TEXT NOT NULL" +
                        ")");

                st.execute("CREATE TABLE IF NOT EXISTS highscores (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INTEGER NULL," +
                        "player_name TEXT NOT NULL," +
                        "difficulty TEXT NOT NULL," +
                        "board_mode TEXT NOT NULL," +
                        "lives INTEGER NOT NULL," +
                        "time_seconds INTEGER NOT NULL," +
                        "created_at TEXT NOT NULL," +
                        "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL" +
                        ")");

                st.execute(
                        "CREATE INDEX IF NOT EXISTS idx_highscores_filters ON highscores(difficulty, board_mode, lives, time_seconds)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_highscores_created_at ON highscores(created_at)");
            }
        } catch (SQLException e) {
            System.err.println("Error initializing SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized List<PlayerSlot> getPlayerSlots() {
        lastErrorMessage = null;
        List<PlayerSlot> result = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, username FROM users ORDER BY datetime(created_at) ASC, id ASC")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(new PlayerSlot(rs.getInt("id"), rs.getString("username")));
                    }
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error retrieving player slots: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public synchronized Integer createPlayerSlot(String username) {
        lastErrorMessage = null;
        if (username == null || username.isBlank()) {
            return null;
        }

        // Internally create a user with a random password (password is never shown to the
        // player). This keeps the existing schema intact.
        byte[] pwdBytes = new byte[24];
        secureRandom.nextBytes(pwdBytes);
        String randomPassword = Base64.getEncoder().encodeToString(pwdBytes);
        boolean ok = createUser(username.trim(), randomPassword);
        if (!ok) {
            return null;
        }

        // Fetch id
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
                ps.setString(1, username.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Integer.valueOf(rs.getInt("id"));
                    }
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error creating player slot: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public synchronized boolean renamePlayerSlot(int userId, String newUsername) {
        lastErrorMessage = null;
        if (newUsername == null || newUsername.isBlank()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET username = ? WHERE id = ?")) {
                ps.setString(1, newUsername.trim());
                ps.setInt(2, userId);
                int updated = ps.executeUpdate();
                if (updated <= 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE highscores SET player_name = ? WHERE user_id = ?")) {
                ps.setString(1, newUsername.trim());
                ps.setInt(2, userId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error renaming player slot: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean deletePlayerSlot(int userId) {
        lastErrorMessage = null;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM highscores WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                int updated = ps.executeUpdate();
                if (updated <= 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error deleting player slot: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        return DriverManager.getConnection(JDBC_URL);
    }

    private synchronized void ensureDriverLoaded() {
        if (driverChecked) {
            return;
        }
        driverChecked = true;
        try {
            Class.forName(SQLITE_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            lastErrorMessage = "SQLite driver not found. Make sure sqlite-jdbc.jar is on the classpath.";
        } catch (Throwable t) {
            lastErrorMessage = t.getMessage();
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public synchronized void resetDatabase() {
        lastErrorMessage = null;
        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("DELETE FROM highscores");
                st.execute("DELETE FROM users");
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error resetting database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized boolean createUser(String username, String password) {
        lastErrorMessage = null;
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            byte[] salt = new byte[SALT_BYTES];
            secureRandom.nextBytes(salt);

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = hashPassword(password, saltB64);
            if (hashB64 == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users(username, password_salt, password_hash, created_at) VALUES(?,?,?,?)")) {
                ps.setString(1, username.trim());
                ps.setString(2, saltB64);
                ps.setString(3, hashB64);
                ps.setString(4, LocalDateTime.now().toString());
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized Integer authenticateUser(String username, String password) {
        lastErrorMessage = null;
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT id, password_salt, password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    int id = rs.getInt("id");
                    String salt = rs.getString("password_salt");
                    String expected = rs.getString("password_hash");
                    String actual = hashPassword(password, salt);
                    if (actual == null) {
                        return null;
                    }
                    if (constantTimeEquals(expected, actual)) {
                        return Integer.valueOf(id);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public synchronized String getUsernameById(int userId) {
        lastErrorMessage = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error retrieving username: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public synchronized boolean saveHighscore(Integer userId, Highscore highscore) {
        lastErrorMessage = null;
        if (highscore == null) {
            return false;
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO highscores(user_id, player_name, difficulty, board_mode, lives, time_seconds, created_at) VALUES(?,?,?,?,?,?,?)")) {
                if (userId == null) {
                    ps.setObject(1, null);
                } else {
                    ps.setInt(1, userId.intValue());
                }
                ps.setString(2, highscore.getPlayerName());
                ps.setString(3, highscore.getDifficulty());
                ps.setString(4, highscore.getBoardMode());
                ps.setInt(5, highscore.getLives());
                ps.setLong(6, highscore.getTimeSeconds());
                ps.setString(7,
                        (highscore.getTimestamp() == null ? LocalDateTime.now() : highscore.getTimestamp()).toString());
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error saving highscore: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public enum SortOrder {
        TIME_ASC, TIME_DESC, DATE_DESC
    }

    public synchronized List<Highscore> getHighscores(String difficulty, String boardMode, Integer lives, int limit,
            SortOrder sort) {
        lastErrorMessage = null;
        List<Highscore> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, player_name, difficulty, board_mode, lives, time_seconds, created_at FROM highscores WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (difficulty != null && !difficulty.isBlank() && !"All".equalsIgnoreCase(difficulty)) {
            sql.append(" AND difficulty = ?");
            params.add(difficulty);
        }
        if (boardMode != null && !boardMode.isBlank() && !"All".equalsIgnoreCase(boardMode)) {
            sql.append(" AND board_mode = ?");
            params.add(boardMode);
        }
        if (lives != null && lives.intValue() > 0) {
            sql.append(" AND lives = ?");
            params.add(lives);
        }

        if (sort == null) {
            sort = SortOrder.TIME_ASC;
        }
        if (sort == SortOrder.TIME_DESC) {
            sql.append(" ORDER BY time_seconds DESC, created_at DESC");
        } else if (sort == SortOrder.DATE_DESC) {
            sql.append(" ORDER BY created_at DESC, time_seconds ASC");
        } else {
            sql.append(" ORDER BY time_seconds ASC, created_at DESC");
        }
        sql.append(" LIMIT ?");
        params.add(Integer.valueOf(Math.max(1, limit)));

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Integer) {
                        ps.setInt(i + 1, ((Integer) p).intValue());
                    } else {
                        ps.setString(i + 1, String.valueOf(p));
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Highscore hs = new Highscore(
                                rs.getInt("id"),
                                rs.getString("player_name"),
                                rs.getString("difficulty"),
                                rs.getString("board_mode"),
                                rs.getInt("lives"),
                                rs.getLong("time_seconds"),
                                LocalDateTime.parse(rs.getString("created_at")));
                        result.add(hs);
                    }
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = e.getMessage();
            System.err.println("Error retrieving highscores: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public synchronized boolean isHighscore(String difficulty, String boardMode, int lives, long timeSeconds,
            int maxEntries) {
        List<Highscore> current = getHighscores(difficulty, boardMode, Integer.valueOf(lives), maxEntries,
                SortOrder.TIME_ASC);
        if (current.size() < maxEntries) {
            return true;
        }
        Highscore worst = current.get(current.size() - 1);
        return timeSeconds < worst.getTimeSeconds();
    }

    private String hashPassword(String password, String saltB64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltB64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }
}
