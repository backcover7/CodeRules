package com.saucer.sast.utils;

import com.saucer.sast.lang.java.parser.core.RuleNode;
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
    public final static String dbname = Paths.get(FileUtils.OutputDirectory, "nodes.db").toAbsolutePath().toString();

    private final static String NAMESPACE = "namespace";
    private final static String CLASSTYPE = "classtype";
    private final static String METHOD = "methodname";
    private final static String KIND = "kind";
    private final static String NODETYPE = "nodetype";

    public final static String PRENAMESPACE = "prenamespace";
    public final static String PRECLASSTYPE = "preclasstype";
    public final static String PREMETHODNAME = "premethodname";
    public final static String PRELINENUM = "prelinenum";
    public final static String PRESIGNATURE = "presignature";
    public final static String PREGADGETSOURCE = "pregadgetsource";
    public final static String SUCCNAMESPACE = "succnamespace";
    public final static String SUCCCLASSTYPE = "succclasstype";
    public final static String SUCCMETHODNAME = "succmethodname";
    public final static String SUCCCODE = "succcode";
    public final static String SUCCLINENUM = "succlinenum";
    public final static String SUCCSIGNATURE = "succsignature";
    public final static String PARENTCODE = "parentcode";
    public final static String FILEPATH = "filepath";
    public final static String EDGETYPE = "edgetype";
    public final static String DATATRACE = "datatrace";

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

    public static void connect() throws ClassNotFoundException {
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
        ImportNodes(Paths.get(FileUtils.RulesDirectory, "sources.csv").toString(), RuleNode.SOURCENODE);
        ImportNodes(Paths.get(FileUtils.RulesDirectory, "sinks.csv").toString(), RuleNode.SINKNODE);
        ImportNodes(Paths.get(FileUtils.RulesDirectory, "gadget.csv").toString(), RuleNode.GADGETSOURCENODE);
        ImportNodes(Paths.get(FileUtils.RulesDirectory, "negative.csv").toString(), RuleNode.NEGATIVENODE);
    }

    private static void ImportNodes(String nodePath, String nodeType) throws Exception {
        String sql = "INSERT or IGNORE INTO node (" +
                "namespace, " +
                "classtype, " +
                "methodname, " +
                "kind, " +
                "nodetype) VALUES (?, ?, ?, ?, ?)";

        BufferedReader lineReader = new BufferedReader(new FileReader(nodePath));
        lineReader.lines().forEach(lineText -> {
            try {
                PreparedStatement statement = conn.prepareStatement(sql);
                String[] data = lineText.split(CharUtils.colon, -1);
                statement.setString(1, data[0]);
                statement.setString(2, data[1]);
                statement.setString(3, data[2]);
                statement.setString(4, data[3]);
                statement.setString(5, nodeType);

                statement.executeUpdate();
                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        lineReader.close();
    }

    private static void CreateCallGraphTable() {
        String sql = "CREATE TABLE callgraph (\n"
                + "	prenamespace varchar,\n"
                + "	preclasstype varchar,\n"
                + "	premethodname varchar,\n"
                + "	prelinenum varchar,\n"
                + "	presignature integer,\n"
                + "	pregadgetsource varchar,\n"
                + "	succnamespace varchar,\n"
                + "	succclasstype varchar,\n"
                + "	succmethodname varchar,\n"
                + "	succcode varchar,\n"
                + "	succlinenum integer,\n"
                + "	succsignature varchar,\n"
                + "	parentcode varchar,\n"
                + "	filepath varchar,\n"
                + "	edgetype varchar,\n"
                + "	datatrace varchar"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void SaveCallGaraph2Db(CallGraphNode callGraphNode) {
        String sql = "INSERT or IGNORE INTO callgraph (" +
                "prenamespace, " +
                "preclasstype, " +
                "premethodname, " +
                "prelinenum, " +
                "presignature, " +
                "pregadgetsource, " +
                "succnamespace, " +
                "succclasstype, " +
                "succmethodname, " +
                "succcode, " +
                "succlinenum, " +
                "succsignature, " +
                "parentcode, " +
                "filepath, " +
                "edgetype, " +
                "datatrace) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);

            statement.setString(1, callGraphNode.getPreNamespace());
            statement.setString(2, callGraphNode.getPreClasstype());
            statement.setString(3, callGraphNode.getPreMethodName());
            statement.setString(4, callGraphNode.getPreLineNum());
            statement.setString(5, callGraphNode.getPreSignature());
            statement.setString(6, String.valueOf(callGraphNode.isPreGadgetSource()));
            statement.setString(7, callGraphNode.getSuccNamespace());
            statement.setString(8, callGraphNode.getSuccClasstype());
            statement.setString(9, callGraphNode.getSuccMethodName());
            statement.setString(10, callGraphNode.getSuccCode());
            statement.setString(11, callGraphNode.getSuccLineNum());
            statement.setString(12, callGraphNode.getSuccSignature());
            statement.setString(13, callGraphNode.getParentCode());
            statement.setString(14, callGraphNode.getFilePath());
            statement.setString(15, callGraphNode.getEdgeType());
            statement.setString(16, callGraphNode.getDatatrace());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static RuleNode QueryAnnotationTypeNode(String namespace, String classtype) {
        String sql = "SELECT kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND kind LIKE 'annotation%'";
        return QueryDeclaringType(sql, namespace, classtype);
    }

    public static RuleNode QueryConstructorNode(String namespace, String classtype) {
        String sql = "SELECT kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND methodname = '<init>'";
        RuleNode ruleNode = QueryDeclaringType(sql, namespace, classtype);
        ruleNode.setMethod("<init>");
        return ruleNode;
    }

    private static RuleNode QueryDeclaringType(String sql, String namespace, String classtype) {
        RuleNode ruleNode = new RuleNode();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, namespace);
            statement.setString(2, classtype);

            ResultSet resultSet = statement.executeQuery();

            ruleNode.setNamespace(namespace);
            ruleNode.setClasstype(classtype);

            while(resultSet.next()) {
                ruleNode.setKind(resultSet.getString(KIND));
                ruleNode.setNodetype(resultSet.getString(NODETYPE));
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ruleNode;
    }

    public static RuleNode QueryInvocationMethodNode(String namespace, String classtype, String methodname) {
        String sql = "SELECT methodname, kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND nodetype != "
                + "\"" + RuleNode.GADGETSOURCENODE + "\"";
        return QueryMethodNode(sql, namespace, classtype, methodname);
    }

    public static RuleNode QueryCtExecutableMethodNode(String namespace, String classtype, String methodname) {
        String sql = "SELECT methodname, kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND nodetype = "
                + "\"" + RuleNode.GADGETSOURCENODE + "\"";
        return QueryMethodNode(sql, namespace, classtype, methodname);
    }

    public static RuleNode QueryNegativeNode(String namespace, String classtype, String methodname) {
        String sql = "SELECT methodname, kind, nodetype FROM node WHERE namespace = ? AND classtype = ? AND nodetype = "
                + "\"" + RuleNode.NEGATIVENODE + "\"";
        return QueryMethodNode(sql, namespace, classtype, methodname);
    }

    private static RuleNode QueryMethodNode(String sql, String namespace, String classtype, String methodname) {
        RuleNode ruleNode = new RuleNode();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, namespace);
            statement.setString(2, classtype);

            ResultSet resultSet = statement.executeQuery();

            ruleNode.setNamespace(namespace);
            ruleNode.setClasstype(classtype);
            ruleNode.setMethod(methodname);

            while(resultSet.next()) {
                String method = resultSet.getString(METHOD);
                String kind = resultSet.getString(KIND);
                String nodeType = resultSet.getString(NODETYPE);

                ruleNode.setKind(kind);
                if (method.startsWith(CharUtils.leftbracket) && CharUtils.RegexMatch(method, methodname)) {
                    ruleNode.setNodetype(nodeType);
                } else if (method.equals(methodname)) {
                    ruleNode.setNodetype(nodeType);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ruleNode;
    }

    public static ArrayList<HashMap<String, String>> QuerySourceCallGraph() throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE edgetype = \"" + CallGraphNode.SourceFlowType + "\"";
        PreparedStatement statement = conn.prepareStatement(sql);
        return QueryCallGraph(statement);
    }

    public static ArrayList<HashMap<String, String>> QuerySuccCallGraph
            (String namespace, String classtype, String methodname, String signature) throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE prenamespace = ? AND preclasstype = ? AND premethodname = ? AND presignature = ?";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, namespace);
        statement.setString(2, classtype);
        statement.setString(3, methodname);
        statement.setString(4, signature);

        return QueryCallGraph(statement);
    }

    public static ArrayList<HashMap<String, String>> QueryGadgetSourceNodeCallGraph() throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE pregadgetsource = \"true\" group by prenamespace, preclasstype, premethodname, presignature";
        PreparedStatement statement = conn.prepareStatement(sql);
        return QueryCallGraph(statement);
    }

    public static ArrayList<HashMap<String, String>> QuerySetterGetterConstructorCallGraph() throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE presignature not like \"%()\" AND premethodname LIKE \"set%\" OR premethodname LIKE \"get%\" OR " +
                "premethodname = \"<init>\" group by prenamespace, preclasstype, premethodname";
        PreparedStatement statement = conn.prepareStatement(sql);
        return QueryCallGraph(statement);
    }

    private static ArrayList<HashMap<String, String>> QueryCallGraph(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();

        ArrayList<HashMap<String, String>> sources = new ArrayList<>();
        while(resultSet.next()) {
            HashMap<String, String> source = new HashMap<>();

            source.put(PRENAMESPACE, resultSet.getString(PRENAMESPACE));
            source.put(PRECLASSTYPE, resultSet.getString(PRECLASSTYPE));
            source.put(PREMETHODNAME, resultSet.getString(PREMETHODNAME));
            source.put(PRELINENUM, resultSet.getString(PRELINENUM));
            source.put(PRESIGNATURE, resultSet.getString(PRESIGNATURE));
            source.put(PREGADGETSOURCE, String.valueOf(resultSet.getInt(PREGADGETSOURCE)));
            source.put(SUCCNAMESPACE, resultSet.getString(SUCCNAMESPACE));
            source.put(SUCCCLASSTYPE, resultSet.getString(SUCCCLASSTYPE));
            source.put(SUCCMETHODNAME, resultSet.getString(SUCCMETHODNAME));
            source.put(SUCCCODE, resultSet.getString(SUCCCODE));
            source.put(SUCCLINENUM, resultSet.getString(SUCCLINENUM));
            source.put(SUCCSIGNATURE, resultSet.getString(SUCCSIGNATURE));
            source.put(PARENTCODE, resultSet.getString(PARENTCODE));
            source.put(FILEPATH, resultSet.getString(FILEPATH));
            source.put(EDGETYPE, resultSet.getString(EDGETYPE));
            source.put(DATATRACE, resultSet.getString(DATATRACE));

            sources.add(source);
        }
        return sources;
    }

    public static void UpdateSinkFlowEdge(HashMap<String, String> invocation) throws SQLException {
        String sql = "UPDATE callgraph SET edgetype = " + "\"" + CallGraphNode.SinkFlowType + "\" " +
                "WHERE prenamespace = ? AND preclasstype = ? AND premethodname = ? AND prelinenum = ? " +
                "AND presignature = ? AND pregadgetsource = ? AND succnamespace = ? AND succclasstype = ? " +
                "AND succmethodname = ? AND succcode = ? AND succlinenum = ? AND succsignature = ?" +
                "AND parentcode = ? AND filepath = ? AND edgetype = ? AND datatrace = ?";

        PreparedStatement statement = conn.prepareStatement(sql);

        statement.setString(1, invocation.get(DbUtils.PRENAMESPACE));
        statement.setString(2, invocation.get(DbUtils.PRECLASSTYPE));
        statement.setString(3, invocation.get(DbUtils.PREMETHODNAME));
        statement.setString(4, invocation.get(DbUtils.PRELINENUM));
        statement.setString(5, invocation.get(DbUtils.PRESIGNATURE));
        statement.setString(6, invocation.get(DbUtils.PREGADGETSOURCE));
        statement.setString(7, invocation.get(DbUtils.SUCCNAMESPACE));
        statement.setString(8, invocation.get(DbUtils.SUCCCLASSTYPE));
        statement.setString(9, invocation.get(DbUtils.SUCCMETHODNAME));
        statement.setString(10, invocation.get(DbUtils.SUCCCODE));
        statement.setString(11, invocation.get(DbUtils.SUCCLINENUM));
        statement.setString(12, invocation.get(DbUtils.SUCCSIGNATURE));
        statement.setString(13, invocation.get(DbUtils.PARENTCODE));
        statement.setString(14, invocation.get(DbUtils.FILEPATH));
        statement.setString(15, invocation.get(DbUtils.EDGETYPE));
        statement.setString(16, invocation.get(DbUtils.DATATRACE));

        statement.executeUpdate();
        statement.close();
    }

    public static ArrayList<RuleNode> QuerySourceNodeFlowRuleNode() throws SQLException {
        return QueryNodeFlowRuleNode(CallGraphNode.SourceFlowType);

    }
    public static ArrayList<RuleNode> QuerySinkNodeFlowRuleNode() throws SQLException {
        return QueryNodeFlowRuleNode(CallGraphNode.SinkNodeType);
    }

    public static ArrayList<RuleNode> QueryNodeFlowRuleNode(String NodeFlowType) throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE edgetype = " + "\"" + NodeFlowType + "\"";

        PreparedStatement statement = conn.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();

        ArrayList<RuleNode> SinkNodes = new ArrayList<>();

        while(resultSet.next()) {
            String namespace = resultSet.getString(SUCCNAMESPACE);
            String classtype = resultSet.getString(SUCCCLASSTYPE);
            String methodname = resultSet.getString(SUCCMETHODNAME);

            RuleNode node = QueryInvocationMethodNode(namespace, classtype, methodname);
            node.setFile(resultSet.getString(FILEPATH));
            node.setLine(resultSet.getString(SUCCLINENUM));
            node.setCode(resultSet.getString(SUCCCODE));
            node.setMethodcode(resultSet.getString(PARENTCODE));

            SinkNodes.add(node);
        }
        return SinkNodes;
    }

    public static ArrayList<HashMap<String, Object>> QuerySinkGadgetNodeFlowRuleNode() throws SQLException {
        String sql = "SELECT * FROM callgraph WHERE edgetype = " + "\"" + CallGraphNode.SinkGadgetNodeFlowType + "\"";

        PreparedStatement statement = conn.prepareStatement(sql);

        ArrayList<HashMap<String, String>> callgraphNodes = QueryCallGraph(statement);
        ArrayList<HashMap<String, Object>> sinkGadgets = new ArrayList<>();
        for (HashMap<String, String> callgraphNode : callgraphNodes) {
            HashMap<String, Object> sinkGadget = new HashMap<>();
            RuleNode source = QueryInvocationMethodNode(
                    callgraphNode.get(DbUtils.PRENAMESPACE),
                    callgraphNode.get(DbUtils.PRECLASSTYPE),
                    callgraphNode.get(DbUtils.PREMETHODNAME));
            source.setSignature(callgraphNode.get(DbUtils.PRESIGNATURE));
            source.setLine(callgraphNode.get(DbUtils.PRELINENUM));
            source.setSignature(callgraphNode.get(DbUtils.PRESIGNATURE));


            RuleNode sink = QueryInvocationMethodNode(
                    callgraphNode.get(DbUtils.SUCCNAMESPACE),
                    callgraphNode.get(DbUtils.SUCCCLASSTYPE),
                    callgraphNode.get(DbUtils.SUCCMETHODNAME));
            sink.setSignature(callgraphNode.get(DbUtils.SUCCSIGNATURE));
            sink.setLine(callgraphNode.get(DbUtils.SUCCLINENUM));
            sink.setCode(callgraphNode.get(DbUtils.SUCCCODE));
            sink.setFile(callgraphNode.get(DbUtils.FILEPATH));

            sinkGadget.put(CallGraphNode.SinkGadgetNodeFlowSource, source);
            sinkGadget.put(CallGraphNode.SinkGadgetNodeFlowSink, sink);
            sinkGadget.put(DATATRACE, callgraphNode.get(DATATRACE));
            sinkGadgets.add(sinkGadget);
        }

        return sinkGadgets;
    }
}
