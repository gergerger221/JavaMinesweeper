import database.DatabaseManager;
import database.Highscore;

public class TestHighscores {
    public static void main(String[] args) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Clear existing highscores for clean test
        dbManager.clearAllHighscores();
        
        // Add some test highscores
        Highscore hs1 = new Highscore("Alice", "Easy", 45);
        Highscore hs2 = new Highscore("Bob", "Easy", 32);
        Highscore hs3 = new Highscore("Charlie", "Medium", 78);
        Highscore hs4 = new Highscore("Diana", "Hard", 120);
        
        dbManager.saveHighscore(hs1);
        dbManager.saveHighscore(hs2);
        dbManager.saveHighscore(hs3);
        dbManager.saveHighscore(hs4);
        
        // Test retrieval
        System.out.println("=== Easy Difficulty Highscores ===");
        for (Highscore hs : dbManager.getTopHighscores("Easy", 10)) {
            System.out.println(hs);
        }
        
        System.out.println("\n=== Medium Difficulty Highscores ===");
        for (Highscore hs : dbManager.getTopHighscores("Medium", 10)) {
            System.out.println(hs);
        }
        
        System.out.println("\n=== Hard Difficulty Highscores ===");
        for (Highscore hs : dbManager.getTopHighscores("Hard", 10)) {
            System.out.println(hs);
        }
        
        // Test highscore qualification
        System.out.println("\n=== Highscore Qualification Tests ===");
        System.out.println("Time 30 seconds qualifies for Easy: " + dbManager.isHighscore("Easy", 30, 10));
        System.out.println("Time 50 seconds qualifies for Easy: " + dbManager.isHighscore("Easy", 50, 10));
        System.out.println("Time 70 seconds qualifies for Medium: " + dbManager.isHighscore("Medium", 70, 10));
        System.out.println("Time 90 seconds qualifies for Medium: " + dbManager.isHighscore("Medium", 90, 10));
        
        System.out.println("\nTest completed successfully!");
    }
}
