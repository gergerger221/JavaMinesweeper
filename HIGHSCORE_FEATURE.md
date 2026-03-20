# Highscore Feature Documentation

## Overview
The Java Minesweeper game now includes a persistent highscore system that tracks the best completion times for each difficulty level.

## Features
- **Persistent Storage**: Highscores are saved to a text file in the user's home directory (`minesweeper_highscores.txt`)
- **Separate Leaderboards**: Top 10 scores for each difficulty level (Easy, Medium, Hard)
- **Player Name Entry**: When you achieve a highscore, you'll be prompted to enter your name
- **Automatic Ranking**: Scores are automatically sorted by completion time (fastest first)
- **Clean UI**: Dedicated highscores screen with tabbed interface for each difficulty

## How to Use

### Viewing Highscores
1. From the main menu, click the "Highscores" button
2. Use the tabs to switch between Easy, Medium, and Hard difficulties
3. View the top 10 scores with player names, times, and dates

### Achieving a Highscore
1. Complete any game in the fastest possible time
2. If your time qualifies for the top 10, you'll see a congratulations message
3. Enter your name (max 20 characters)
4. Your score will be automatically saved and displayed in the highscores

## Technical Details

### File Storage
- **Location**: `C:\Users\[YourUsername]\minesweeper_highscores.txt`
- **Format**: Pipe-delimited text file with fields: `id|playerName|difficulty|timeSeconds|timestamp`
- **Backup**: The file is automatically created if it doesn't exist

### Data Structure
- **Highscore Model**: Contains player name, difficulty, time in seconds, and timestamp
- **Database Manager**: Singleton pattern for file operations
- **Automatic Cleanup**: Keeps only top 50 scores per difficulty to prevent file bloat

### Classes Added
- `database/Highscore.java` - Model class for highscore entries
- `database/DatabaseManager.java` - File-based persistence layer
- `ui/HighscoresPanel.java` - UI component for displaying highscores

## Game Integration
- Highscores are checked automatically when you win a game
- The `gameOver()` method has been enhanced to handle highscore saving
- Player name input includes validation (non-empty, max 20 characters)
- Highscores are displayed in MM:SS format for easy reading

## Testing
A test class `TestHighscores.java` is included to verify the functionality:
```bash
javac -cp ".;src" TestHighscores.java
java -cp ".;src" TestHighscores
```

## Future Enhancements
Potential improvements could include:
- Export/import highscores functionality
- Statistics tracking (total games, win rate, etc.)
- Custom difficulty highscores
- Online leaderboards
- Achievement system

## Troubleshooting
- If highscores don't save, check file permissions in your home directory
- The highscores file is created automatically on first use
- All highscores can be cleared by deleting the `minesweeper_highscores.txt` file
