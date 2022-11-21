package utils;

import lang.java.parser.converter.Mannual;
import lang.java.parser.core.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;

public class DbUtils {
    public static Connection conn;
    public final static String dbname = Paths.get(Mannual.csvDirectory, "nodes.db").toAbsolutePath().toString();
    public final static String SOURCE = "source";
    public final static String SINK = "sink";

    private final static String NAMESPACE = "namespace";
    private final static String CLASSTYPE = "classtype";
    private final static String METHOD = "methodname";
    private final static String KIND = "kind";
    private final static String NODETYPE = "nodetype";

    public void init() throws Exception {
        File nodeDb = new File(dbname);
        if (nodeDb.exists()) {
            nodeDb.delete();
        }

        connect();
        CreateNodeTable();
        ImportNodes();
    }

    private static void connect() throws ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + dbname;

            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void CreateNodeTable() {
        String sql = "CREATE TABLE node (\n"
                + "	namespace varchar,\n"
                + "	classtype varchar,\n"
                + "	methodname varchar,\n"
                + "	kind varchar,\n"
                + "	nodetype varchar"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ImportNodes() throws Exception {
        ImportNodes(Paths.get(Mannual.csvDirectory, "sources.csv").toString(), SOURCE);
        ImportNodes(Paths.get(Mannual.csvDirectory, "sinks.csv").toString(), SINK);
    }

    private static void ImportNodes(String nodePath, String nodeType) throws Exception {
        String sql = "INSERT or IGNORE INTO node (" +
                "namespace, " +
                "classtype, " +
                "methodname, " +
                "kind, " +
                "nodetype) VALUES (?, ?, ?, ?, ?)";

        BufferedReader lineReader = new BufferedReader(new FileReader(nodePath));
        String lineText;
        while ((lineText = lineReader.readLine()) != null) {
            PreparedStatement statement = conn.prepareStatement(sql);
            String[] data = lineText.split(CharUtils.colon, -1);
            statement.setString(1, data[0]);
            statement.setString(2, data[1]);
            statement.setString(3, data[2]);
            statement.setString(4, data[3]);
            statement.setString(5, nodeType);

            statement.executeUpdate();
            statement.close();
        }
        lineReader.close();
    }

    public static Node QueryAnnotationType(String namespace, String classtype) throws SQLException {
        String sql = "SELECT kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND kind LIKE 'annotation%'";
        return QueryDeclaringType(sql, namespace, classtype);
    }

    public static Node QueryConstructor(String namespace, String classtype) throws SQLException {
        String sql = "SELECT kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND methodname = '<init>'";
        Node node = QueryDeclaringType(sql, namespace, classtype);
        node.setMethod("<init>");
        return node;
    }

    private static Node QueryDeclaringType(String sql, String namespace, String classtype) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, namespace);
        statement.setString(2, classtype);

        ResultSet resultSet = statement.executeQuery();

        Node node = new Node();
        node.setNamespace(namespace);
        node.setClasstype(classtype);

        while(resultSet.next()) {
            node.setKind(resultSet.getString(KIND));
            node.setNodetype(resultSet.getString(NODETYPE));
            statement.close();
        }
        return node;
    }

    public static Node QueryMethod(String namespace, String classtype, String methodname) throws SQLException {
        // TODO: methodname like ... But this needs to store the all executablereference and then query on them.
        //  This will also convert some nodes to simplied version even with namespace & classtype
        String sql = "SELECT methodname, kind, nodetype FROM node WHERE namespace = ? AND classtype = ?";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, namespace);
        statement.setString(2, classtype);

        ResultSet resultSet = statement.executeQuery();

        Node node = new Node();
        node.setNamespace(namespace);
        node.setClasstype(classtype);
        node.setMethod(methodname);

        while(resultSet.next()) {
            String method = resultSet.getString(METHOD);
            if (method.startsWith(CharUtils.leftbracket) && CharUtils.RegexMatch(method, methodname)) {
                node.setKind(resultSet.getString(KIND));
                node.setNodetype(resultSet.getString(NODETYPE));
            } else if (method.equals(methodname)) {
                node.setKind(resultSet.getString(KIND));
                node.setNodetype(resultSet.getString(NODETYPE));
            }
        }
        return node;
    }
}
