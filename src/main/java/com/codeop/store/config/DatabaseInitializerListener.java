package com.codeop.store.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

public class DatabaseInitializerListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment environment = event.getEnvironment();
        String adminUrl = environment.getProperty("app.db.admin.url");
        String adminUser = environment.getProperty("app.db.admin.username");
        String adminPassword = environment.getProperty("app.db.admin.password");
        String databaseName = environment.getProperty("app.db.name");

        if (isBlank(adminUrl) || isBlank(adminUser) || isBlank(databaseName)) {
            return;
        }

        try (Connection connection = DriverManager.getConnection(adminUrl, adminUser, adminPassword)) {
            if (databaseExists(connection, databaseName)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE \"" + databaseName + "\"");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to ensure database exists: " + databaseName, ex);
        }
    }

    private boolean databaseExists(Connection connection, String dbName) throws Exception {
        String sql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
