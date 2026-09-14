package com.airline.util;

/**
 * Singleton class to manage the authenticated user's session throughout the application.
 */
public class UserSession {

    private static UserSession instance;

    private int userId;
    private String fullName;
    private String email;
    private String role;
    private String nicPassport;

    private UserSession(int userId, String fullName, String email, String role, String nicPassport) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role != null ? role : "Passenger";
        this.nicPassport = nicPassport;
    }

    public static synchronized UserSession initSession(int userId, String fullName, String email, String role, String nicPassport) {
        instance = new UserSession(userId, fullName, email, role, nicPassport);
        return instance;
    }

    public static synchronized UserSession getInstance() {
        return instance;
    }

    public static synchronized void cleanUserSession() {
        instance = null;
    }

    public static boolean isLoggedIn() {
        return instance != null;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(this.role);
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNicPassport() {
        return nicPassport;
    }

    public void setNicPassport(String nicPassport) {
        this.nicPassport = nicPassport;
    }
}
