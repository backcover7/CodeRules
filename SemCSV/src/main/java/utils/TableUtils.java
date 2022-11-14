package utils;

import config.DbConfig;

import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;

public class TableUtils {
    public static final String CLASSID = "class_id";
    public static final String METHODID = "method_id";
    public static final String NAMESPACE = "namespace";
    public static final String CLASSTYPE = "classtype";
    public static final String TYPEMODIFIER = "type_modifer";
    public static final String SUPERCLASS = "superclass";
    public static final String INTERFACE = "interface";
    public static final String METHODNAME = "methodname";
    public static final String METHODMODIFER = "method_modifier";
    public static final String ARGSIZE = "argument_size";
    public static final String ARGTYPE = "argument_type";
    public static final String RETTYPE = "return_type";
    public final static String Category = "category";

    private static void CreateClassTable() {
        String sql = "CREATE TABLE classes (\n"
                + "	class_id INTEGER PRIMARY KEY,\n"
                + "	namespace varchar,\n"
                + "	classtype varchar,\n"
                + "	type_modifer varchar,\n"
                + "	superclass varchar,\n"
                + "	interface varchar,\n"
                + "	category varchar, \n"
                + " UNIQUE(namespace, classtype)\n"
                + ");";

        try (Statement stmt = DbConfig.connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void CreateMethodTable() {
        String sql = "CREATE TABLE methods (\n"
                + "	method_id INTEGER PRIMARY KEY,\n"
                + "	class_id INTEGER,\n"
                + "	methodname varchar,\n"
                + "	method_modifier varchar,\n"
                + "	argument_size varchar,\n"
                + "	argument_type varchar,\n"
                + "	return_type varchar,\n"
                + "	kind varchar,\n"
                + "	category varchar,\n"
//                + "	FOREIGN KEY (class_id) REFERENCES classes (class_id)\n"
                + " UNIQUE(class_id, methodname, method_modifier, argument_size, argument_type, return_type)\n"
                + ");";

        try (Statement stmt = DbConfig.connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int QueryClassId(String namespace, String classtype) throws SQLException {
        String sql = "SELECT class_id FROM " + DbUtils.ClassTablename + " WHERE namespace = ? and classtype = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setString(1, namespace);
        statement.setString(2, classtype);

        ResultSet resultSet = statement.executeQuery();
        int class_id = resultSet.getInt("class_id");

        statement.close();

        return class_id;
    }

    private static void ImportClassCSV(String CsvPath) throws Exception {
        ImportClassCSV(CsvPath, DbUtils.ClassTablename);
    }

    private static void ImportClassCSV(String CsvPath, String table) throws Exception {
        String sql = "INSERT or IGNORE INTO " + table + " (" +
                "namespace, " +
                "classtype, " +
                "type_modifer, " +
                "superclass, " +
                "interface, " +
                "category) VALUES (?, ?, ?, ?, ?, ?)";

        BufferedReader lineReader = new BufferedReader(new FileReader(CsvPath));
        String lineText;
        while ((lineText = lineReader.readLine()) != null) {
            PreparedStatement statement = DbConfig.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            String[] data = lineText.split(StringsUtils.colon, -1);
            statement.setString(1, data[0]);
            statement.setString(2, data[1]);
            statement.setString(3, data[2]);
            statement.setString(4, data[3]);
            statement.setString(5, data[4]);
            statement.setString(6, data[11]);

            statement.executeUpdate();
            statement.close();
        }
        lineReader.close();
    }

    private static void ImportMethodCSV(String CsvPath) throws Exception {
        ImportMethodCSV(CsvPath, DbUtils.MethodTablename);
    }

    private static void ImportMethodCSV(String CsvPath, String table) throws Exception {
        String sql = "INSERT or IGNORE INTO " + table + " (" +
                "class_id, " +
                "methodname, " +
                "method_modifier, " +
                "argument_size, " +
                "argument_type, " +
                "return_type, " +
                "kind, " +
                "category) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        BufferedReader lineReader = new BufferedReader(new FileReader(CsvPath));
        String lineText;
        while ((lineText = lineReader.readLine()) != null) {
            PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
            String[] data = lineText.split(StringsUtils.colon, -1);

            int class_id = QueryClassId(data[0], data[1]);

            statement.setInt(1, class_id);
            statement.setString(2, data[5]);
            statement.setString(3, data[6]);
            statement.setString(4, data[7]);
            statement.setString(5, data[8]);
            statement.setString(6, data[9]);
            statement.setString(7, data[10]);
            statement.setString(8, data[11]);

            statement.executeUpdate();
            statement.close();
        }

        lineReader.close();
    }

    public static void InitDB() throws Exception {
        ArrayList<String> CsvPathList = FilesUtils.getExtensionFiles(
                Paths.get(FilesUtils.method_csv).toAbsolutePath().toString(), StringsUtils.CsvExtension, true);

        CreateClassTable();
        for (String CsvPath : CsvPathList) {
            System.out.println("[+] Importing class csv file " + Paths.get(CsvPath).getFileName().toString());
            ImportClassCSV(CsvPath);
        }
        System.out.println("[*] Finish importing all class csv files!");

        CreateMethodTable();
        for (String CsvPath : CsvPathList) {
            System.out.println("[+] Importing method csv file " + Paths.get(CsvPath).getFileName().toString());
            ImportMethodCSV(CsvPath);
        }
        System.out.println("[*] Finish importing all method csv files!");
    }
}
