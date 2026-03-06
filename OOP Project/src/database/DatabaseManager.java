package database;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseManager {
    private static final String DB_NAME = "minesweeper_highscores.txt";
    private static final String DB_PATH = System.getProperty("user.home") + File.separator + DB_NAME;
    
    private static DatabaseManager instance;
    private int nextId = 1;
    
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
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
                System.out.println("Database file created at: " + DB_PATH);
            } catch (IOException e) {
                System.err.println("Error creating database file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Database file found at: " + DB_PATH);
            // Find the next available ID
            List<Highscore> existing = getAllHighscores();
            for (Highscore hs : existing) {
                if (hs.getId() >= nextId) {
                    nextId = hs.getId() + 1;
                }
            }
        }
    }

    public boolean saveHighscore(Highscore highscore) {
        try {
            List<Highscore> highscores = getAllHighscores();
            highscore.setId(nextId++);
            highscores.add(highscore);
            
            // Sort by time (ascending - lower time is better)
            Collections.sort(highscores);
            
            // Keep only top 50 per difficulty to prevent file from growing too large
            List<Highscore> filteredHighscores = new ArrayList<>();
            for (String difficulty : new String[]{"Easy", "Medium", "Hard"}) {
                int count = 0;
                for (Highscore hs : highscores) {
                    if (hs.getDifficulty().equals(difficulty) && count < 50) {
                        filteredHighscores.add(hs);
                        count++;
                    }
                }
            }
            
            return saveHighscoresToFile(filteredHighscores);
            
        } catch (Exception e) {
            System.err.println("Error saving highscore: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Highscore> getTopHighscores(String difficulty, int limit) {
        try {
            List<Highscore> allHighscores = getAllHighscores();
            List<Highscore> filteredHighscores = new ArrayList<>();
            
            for (Highscore hs : allHighscores) {
                if (hs.getDifficulty().equals(difficulty)) {
                    filteredHighscores.add(hs);
                }
            }
            
            // Sort by time (ascending - lower time is better)
            Collections.sort(filteredHighscores);
            
            // Return only the requested number of highscores
            List<Highscore> result = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, filteredHighscores.size()); i++) {
                result.add(filteredHighscores.get(i));
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error retrieving highscores: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean isHighscore(String difficulty, long timeSeconds, int maxEntries) {
        System.out.println("DEBUG: isHighscore called - difficulty: '" + difficulty + "', time: " + timeSeconds + ", maxEntries: " + maxEntries);
        List<Highscore> currentHighscores = getTopHighscores(difficulty, maxEntries);
        System.out.println("DEBUG: Current highscores count: " + currentHighscores.size());
        
        // If we have fewer than max entries, any time qualifies
        if (currentHighscores.size() < maxEntries) {
            System.out.println("DEBUG: Qualifies - fewer than max entries");
            return true;
        }
        
        // Check if time is better than worst time in current list
        Highscore worstHighscore = currentHighscores.get(currentHighscores.size() - 1);
        boolean qualifies = timeSeconds < worstHighscore.getTimeSeconds();
        System.out.println("DEBUG: Worst time: " + worstHighscore.getTimeSeconds() + ", Current time: " + timeSeconds + ", Qualifies: " + qualifies);
        return qualifies;
    }

    private List<Highscore> getAllHighscores() {
        List<Highscore> highscores = new ArrayList<>();
        File dbFile = new File(DB_PATH);
        
        if (!dbFile.exists()) {
            return highscores;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5) {
                        Highscore highscore = new Highscore(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            parts[2],
                            Long.parseLong(parts[3]),
                            LocalDateTime.parse(parts[4])
                        );
                        highscores.add(highscore);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing highscore line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading highscores file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return highscores;
    }

    private boolean saveHighscoresToFile(List<Highscore> highscores) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DB_PATH))) {
            for (Highscore hs : highscores) {
                writer.write(String.format("%d|%s|%s|%d|%s%n",
                    hs.getId(),
                    hs.getPlayerName(),
                    hs.getDifficulty(),
                    hs.getTimeSeconds(),
                    hs.getTimestamp().toString()
                ));
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing highscores file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // For testing purposes
    public int getHighscoreCount(String difficulty) {
        List<Highscore> highscores = getTopHighscores(difficulty, 100);
        return highscores.size();
    }

    public void clearAllHighscores() {
        try {
            File dbFile = new File(DB_PATH);
            if (dbFile.exists()) {
                dbFile.delete();
                initializeDatabase();
                System.out.println("All highscores cleared");
            }
        } catch (Exception e) {
            System.err.println("Error clearing highscores: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
