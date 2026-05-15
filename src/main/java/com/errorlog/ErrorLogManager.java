package com.errorlog;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ErrorLogManager {

    // ─────────────────────────────────────────────────
    // ADD a log entry → INSERT into PostgreSQL
    // ─────────────────────────────────────────────────
    public void addLog(String rawLog) throws SQLException {
        // Validate input first
        if (rawLog == null || rawLog.trim().isEmpty()) {
            throw new IllegalArgumentException("Log entry cannot be empty.");
        }
        if (!rawLog.contains(":")) {
            throw new IllegalArgumentException(
                "Invalid format. Expected 'TYPE: message' — got: " + rawLog
            );
        }

        int colonIndex = rawLog.indexOf(":");
        String type    = rawLog.substring(0, colonIndex).trim().toUpperCase();
        String message = rawLog.substring(colonIndex + 1).trim();

        if (type.isEmpty()) {
            throw new IllegalArgumentException("Error type cannot be empty.");
        }

        // PreparedStatement prevents SQL Injection attacks
        // Never use string concatenation to build SQL queries
        String sql = "INSERT INTO server_logs (error_type, message) VALUES (?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, message);
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────
    // GET all logs → SELECT from PostgreSQL
    // ─────────────────────────────────────────────────
    public ArrayList<ServerError> getAllErrors() throws SQLException {
        return fetchLogs("SELECT error_id, error_type, message FROM server_logs ORDER BY error_id ASC");
    }

    // ─────────────────────────────────────────────────
    // GET only critical logs → SELECT with WHERE clause
    // ─────────────────────────────────────────────────
    public ArrayList<ServerError> getCriticalErrors() throws SQLException {
        return fetchLogs(
            "SELECT error_id, error_type, message FROM server_logs " +
            "WHERE UPPER(error_type) = 'CRITICAL' ORDER BY error_id ASC"
        );
    }

    // ─────────────────────────────────────────────────
    // SORT — fetch all then sort in memory using Comparator
    // (Comparator requirement from assignment is preserved)
    // ─────────────────────────────────────────────────
    public ArrayList<ServerError> getSortedErrors(Comparator<ServerError> comparator)
            throws SQLException {
        ArrayList<ServerError> list = getAllErrors();
        Collections.sort(list, comparator);
        return list;
    }

    // ─────────────────────────────────────────────────
    // COUNT by type → for statistics panel
    // ─────────────────────────────────────────────────
    public int countByType(String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM server_logs WHERE UPPER(error_type) = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ─────────────────────────────────────────────────
    // COUNT all logs → for statistics panel
    // ─────────────────────────────────────────────────
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM server_logs";
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ─────────────────────────────────────────────────
    // CLEAR all logs → DELETE from PostgreSQL
    // ─────────────────────────────────────────────────
    public void clearAll() throws SQLException {
        String sql = "DELETE FROM server_logs";
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    // ─────────────────────────────────────────────────
    // BUILD formatted report using StringBuffer
    // (StringBuffer requirement from assignment preserved)
    // ─────────────────────────────────────────────────
    public String buildReport(ArrayList<ServerError> list) {
        StringBuffer sb = new StringBuffer();
        sb.append("========================================\n");
        sb.append("         SERVER ERROR REPORT            \n");
        sb.append("========================================\n");

        if (list.isEmpty()) {
            sb.append("  No errors to display.\n");
        } else {
            for (ServerError error : list) {
                sb.append("ID      : ").append(error.getErrorId()).append("\n");
                sb.append("Type    : ").append(error.getErrorType()).append("\n");
                sb.append("Message : ").append(error.getMessage()).append("\n");
                sb.append("----------------------------------------\n");
            }
        }

        sb.append("Total: ").append(list.size()).append(" error(s)\n");
        sb.append("========================================\n");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────
    // PRIVATE HELPER — runs a SELECT and maps rows
    // to ServerError objects
    // ─────────────────────────────────────────────────
    private ArrayList<ServerError> fetchLogs(String sql) throws SQLException {
        ArrayList<ServerError> list = new ArrayList<>();

        try (Statement stmt  = DBConnection.getConnection().createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int    id      = rs.getInt("error_id");
                String type    = rs.getString("error_type");
                String message = rs.getString("message");

                // Reconstruct raw log string so ServerError can parse it
                ServerError error = new ServerError(id, type + ": " + message);
                list.add(error);
            }
        }

        return list;
    }
}