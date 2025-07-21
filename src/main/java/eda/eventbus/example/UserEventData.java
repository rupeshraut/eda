package eda.eventbus.example;

/**
 * Example event data for user events
 */
public class UserEventData {
    private final String userId;
    private final String username;
    private final String email;
    private final String action;
    
    public UserEventData(String userId, String username, String email, String action) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.action = action;
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getAction() { return action; }
    
    @Override
    public String toString() {
        return "UserEventData{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}