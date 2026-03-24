package database;

public class PlayerSlot {
    private final int id;
    private final String username;

    public PlayerSlot(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
