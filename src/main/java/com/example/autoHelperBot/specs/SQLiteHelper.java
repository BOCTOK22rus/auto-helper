package com.example.autoHelperBot.specs;

import java.sql.*;
import static com.example.autoHelperBot.specs.Constants.DB_PATH;
import org.springframework.stereotype.Component;

@Component
public class SQLiteHelper {
    private Connection connection;

    public SQLiteHelper() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            System.out.println("Соединение с базой данных установлено.");
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к базе данных: " + e.getMessage());
        }
    }

    public void insertRequests(String userName, String commandBot, String requestTime) {
        String sql = "INSERT INTO requests (user_name, command_bot, request_time) VALUES(?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            pstmt.setString(2, commandBot);
            pstmt.setString(3, requestTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении записи: " + e.getMessage());
        }
    }

    public void insertEngine(String oilChangeMileage, String mileageReplacement, String createTime) {
        String sql = "INSERT INTO engine (oil_change_mileage, mileage_replacement, create_time) VALUES(?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, oilChangeMileage);
            pstmt.setString(2, mileageReplacement);
            pstmt.setString(3, createTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении записи: " + e.getMessage());
        }
    }

    public void insertTransmission(String oilChangeMileage, String mileageReplacement, String createTime) {
        String sql = "INSERT INTO transmission (oil_change_mileage, mileage_replacement, create_time) VALUES(?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, oilChangeMileage);
            pstmt.setString(2, mileageReplacement);
            pstmt.setString(3, createTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении записи: " + e.getMessage());
        }
    }

    public void insertErrors(String code, String description, String timestamp, String createTime) {
        String sql = "INSERT INTO errors (code, description, timestamp, create_time) VALUES(?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, description);
            pstmt.setString(3, timestamp);
            pstmt.setString(4, createTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении записи: " + e.getMessage());
        }
    }

    public void insertAdmins(String name, String createTime) {
        String sql = "INSERT INTO admins (name, create_time) VALUES(?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, createTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении записи: " + e.getMessage());
        }
    }

    public String selectAllRequests() {
        String sql = "SELECT * FROM (SELECT * FROM requests ORDER BY id DESC LIMIT 5 OFFSET 1) ORDER BY id ASC";
        StringBuilder result = new StringBuilder(); // Используем StringBuilder для накопления строк

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Формируем строку для каждой записи и добавляем её в StringBuilder
                result.append("№ ").append(rs.getInt("id")).append(" ")
                        .append(rs.getString("user_name"))
                        .append(" ").append(rs.getString("command_bot"))
                        .append(" ").append(rs.getString("request_time"))
                        .append("\n");
            }
        } catch (SQLException e) {
            return "Ошибка при выборке данных";
        }

        return result.toString();
    }

    public String selectMileage(String sql) {

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getString("mileage_replacement");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}