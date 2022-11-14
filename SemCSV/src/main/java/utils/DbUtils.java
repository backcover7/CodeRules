package utils;

import org.sqlite.SQLiteConfig;

import java.nio.file.Paths;
import java.sql.*;

public class DbUtils {
    public final static String dbname = Paths.get(FilesUtils.csv_folder, "relationships.db").toAbsolutePath().toString();
    public final static String MethodTablename = "methods";
    public final static String ClassTablename = "classes";

    private static Connection connect(String database) throws ClassNotFoundException {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + database;

            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);

            conn = DriverManager.getConnection(url, config.toProperties());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static Connection connect() throws ClassNotFoundException {
        return connect(dbname);
    }
}
