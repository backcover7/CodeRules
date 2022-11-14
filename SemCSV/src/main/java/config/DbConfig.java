package config;

import utils.DbUtils;

import java.sql.Connection;

public class DbConfig {
    public static Connection connection;

    public DbConfig() throws ClassNotFoundException {
        connection = DbUtils.connect();
    }
}
