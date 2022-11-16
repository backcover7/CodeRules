package utils;

import java.nio.file.Paths;
import java.sql.*;

public class DbUtils {
    public final static String dbname = Paths.get(FileUtils.csv, "nodes.db").toAbsolutePath().toString();
    public final static String SourceTablename = "sources";
    public final static String SinkTablename = "sinks";

    private static Connection connect(String database) throws ClassNotFoundException {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + database;

            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static Connection connect() throws ClassNotFoundException {
        return connect(dbname);
    }
}
