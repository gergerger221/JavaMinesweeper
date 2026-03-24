package database;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Highscore implements Comparable<Highscore> {
    private int id;
    private String playerName;
    private String difficulty;
    private String boardMode;
    private int lives;
    private long timeSeconds;
    private LocalDateTime timestamp;

    public Highscore() {
    }

    public Highscore(String playerName, String difficulty, String boardMode, int lives, long timeSeconds) {
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.boardMode = boardMode;
        this.lives = lives;
        this.timeSeconds = timeSeconds;
        this.timestamp = LocalDateTime.now();
    }

    public Highscore(int id, String playerName, String difficulty, String boardMode, int lives, long timeSeconds,
            LocalDateTime timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.boardMode = boardMode;
        this.lives = lives;
        this.timeSeconds = timeSeconds;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getBoardMode() {
        return boardMode;
    }

    public void setBoardMode(String boardMode) {
        this.boardMode = boardMode;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public long getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(long timeSeconds) {
        this.timeSeconds = timeSeconds;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        long minutes = timeSeconds / 60;
        long seconds = timeSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getFormattedDate() {
        if (timestamp == null) {
            return "Unknown";
        }
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    @Override
    public int compareTo(Highscore other) {
        // Sort by time (ascending - lower time is better)
        return Long.compare(this.timeSeconds, other.timeSeconds);
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %s - %s - %s - %s", playerName, difficulty, boardMode, lives,
                getFormattedTime(), getFormattedDate());
    }
}
