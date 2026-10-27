package com.docufind;

import com.docufind.db.DBConnection;
import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Testing Oracle DB Connection...");
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("Oracle Connection OK: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("Initializing schema...");
            DBConnection.initializeSchema();
            System.out.println("Schema initialization finished successfully.");
        } catch (Exception e) {
            System.err.println("DB Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
