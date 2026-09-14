package com.airline.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static String url;
    private static String user;
    private static String password;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}

        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            url = dotenv.get("DB_URL");
            user = dotenv.get("DB_USER");
            password = dotenv.get("DB_PASSWORD");
        } catch (Exception e) {
            System.out.println("[DatabaseConnection] Notice: .env not loaded, using default settings.");
        }

        if (url == null || url.isBlank()) {
            url = "jdbc:mysql://localhost:3306/airline_reservation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        if (user == null || user.isBlank()) {
            user = "root";
        }
        if (password == null) {
            password = "";
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}