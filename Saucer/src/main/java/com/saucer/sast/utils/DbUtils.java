package com.saucer.sast.utils;

import com.saucer.sast.lang.java.parser.core.Node;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DbUtils {
    public static Connection conn;

    public final static String CSVDirectory = Paths.get(FileUtils.csv, "nodes").toString();
    public final static String dbname = Paths.get(CSVDirectory, "nodes.db").toAbsolutePath().toString();

    public final static String SOURCE = "source";
    public final static String SINK = "sink";
    public final static String GADGET = "gadget";

    private final static String NAMESPACE = "namespace";
    private final static String CLASSTYPE = "classtype";
    private final static String METHOD = "methodname";
    private final static String KIND = "kind";
    private final static String NODETYPE = "nodetype";

    public final static String PRENAMESPACE = "prenamespace";
    public final static String PRECLASSTYPE = "preclasstype";
    public final static String PREMETHODNAME = "premethodname";
    public final static String PREPARAMSIZE = "preparamsize";
    public final static String SUCCNAMESPACE = "succnamespace";
    public final static String SUCCCLASSTYPE = "succclasstype";
    public final static String SUCCMETHODNAME = "succmethodname";
    public final static String FILEPATH = "filepath";
    public final static String SUCCCODE = "succcode";
    public final static String EDGETYPE = "edgetype";

    public void init() throws Exception {
        File nodeDb = new File(dbname);
        if (nodeDb.exists()) {
            nodeDb.delete();
        }

        connect();

        // Source, sink, gadget node rule tables
        CreateNodeTable();
        ImportNodes();

        // Call graph edges table
        CreateCallGraphTable();
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
        ImportNodes(Paths.get(CSVDirectory, "sources.csv").toString(), SOURCE);
        ImportNodes(Paths.get(CSVDirectory, "sinks.csv").toString(), SINK);
        ImportNodes(Paths.get(CSVDirectory, "gadget.csv").toString(), GADGET);
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

    private static void CreateCallGraphTable() {
        String sql = "CREATE TABLE callgraph (\n"
                + "	prenamespace varchar,\n"
                + "	preclasstype varchar,\n"
                + "	premethodname varchar,\n"
                + "	preparamsize integer,\n"
                + "	succnamespace varchar,\n"
                + "	succclasstype varchar,\n"
                + "	succmethodname varchar,\n"
                + "	succcode varchar,\n"
                + "	filepath varchar,\n"
                + "	edgetype varchar"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void SaveCG2Db(CallGraphNode callGraphNode) {
        ImportCallGraph(callGraphNode);
    }

    private static void ImportCallGraph(CallGraphNode callGraphNode) {
        String sql = "INSERT or IGNORE INTO callgraph (" +
                "prenamespace, " +
                "preclasstype, " +
                "premethodname, " +
                "preparamsize, " +
                "succnamespace, " +
                "succclasstype, " +
                "succmethodname, " +
                "succcode, " +
                "filepath, " +
                "edgetype) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);

            statement.setString(1, callGraphNode.getPreNamespace());
            statement.setString(2, callGraphNode.getPreClasstype());
            statement.setString(3, callGraphNode.getPreMethod());
            statement.setInt(4, callGraphNode.getPreParamSize());
            statement.setString(5, callGraphNode.getSuccNamespace());
            statement.setString(6, callGraphNode.getSuccClasstype());
            statement.setString(7, callGraphNode.getSuccMethod());
            statement.setString(8, callGraphNode.getSuccCode());
            statement.setString(9, callGraphNode.getFilePath());
            statement.setString(10, callGraphNode.getEdgeType());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            String kind = resultSet.getString(KIND);
            String nodeType = resultSet.getString(NODETYPE);

            if (method.startsWith(CharUtils.leftbracket) && CharUtils.RegexMatch(method, methodname)) {
                node.setKind(kind);
                node.setNodetype(nodeType);
            } else if (method.equals(methodname)) {
                node.setKind(kind);
                node.setNodetype(nodeType);
            }
        }
        return node;
    }

    public static ArrayList<HashMap<String, String>> QuerySourceNode() throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE edgetype = \"" + Node.SourceNodeType + "\"";
        PreparedStatement statement = conn.prepareStatement(sql);
        return QueryCallGraph(statement);
    }

    public static ArrayList<HashMap<String, String>> QuerySuccNode(String namespace, String classtype, String methodname) throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE prenamespace = ? AND preclasstype = ? AND premethodname = ?";
        PreparedStatement statement = conn.prepareStatement(sql);

        statement.setString(1, namespace);
        statement.setString(2, classtype);
        statement.setString(3, methodname);

        return QueryCallGraph(statement);
    }

    public static ArrayList<HashMap<String, String>> QueryCallGraph(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();

        ArrayList<HashMap<String, String>> sources = new ArrayList<>();
        while(resultSet.next()) {
            HashMap<String, String> source = new HashMap<>();

            source.put(PRENAMESPACE, resultSet.getString(PRENAMESPACE));
            source.put(PRECLASSTYPE, resultSet.getString(PRECLASSTYPE));
            source.put(PREMETHODNAME, resultSet.getString(PREMETHODNAME));
            source.put(PREPARAMSIZE, String.valueOf(resultSet.getInt(PREPARAMSIZE)));
            source.put(SUCCNAMESPACE, resultSet.getString(SUCCNAMESPACE));
            source.put(SUCCCLASSTYPE, resultSet.getString(SUCCCLASSTYPE));
            source.put(SUCCMETHODNAME, resultSet.getString(SUCCMETHODNAME));
            source.put(SUCCCODE, resultSet.getString(SUCCCODE));
            source.put(FILEPATH, resultSet.getString(FILEPATH));
            source.put(EDGETYPE, resultSet.getString(EDGETYPE));

            sources.add(source);
        }
        return sources;
    }
}
