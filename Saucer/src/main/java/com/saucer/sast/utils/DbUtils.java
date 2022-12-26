package com.saucer.sast.utils;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.parser.core.MethodHierarchy;
import com.saucer.sast.lang.java.parser.nodes.*;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.*;
import java.lang.Exception;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.saucer.sast.utils.CharUtils.*;

public class DbUtils {
    public static Connection conn;
    public final static String dbname = Paths.get(FileUtils.OutputDirectory, "saucer.db").toAbsolutePath().normalize().toString();
    public final static String rulesTable = "rules";
    public final static String methodsTable = "methods";
    public final static String invocationsTable = "invocations";
    public final static String fieldsTable = "fields";
    public final static String callgraphsTable = "callgraphs";

    public static void init() throws Exception {
        File nodeDb = new File(dbname);
        if (nodeDb.exists()) {
            nodeDb.delete();
        }

        connect();

        // Source, sink, gadget node rule tables
        CreateRulesTable();
        ImportRules();

        // Call graph edges table
        CreateMethodsTable();
        CreateInvocationsTable();
        CreateCallgraphsTable();
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

    private static void CreateRulesTable() {
        String sql = new StringBuilder()
                .append("CREATE TABLE " + rulesTable + " (\n")
                .append(ClassNode.NAMESPACE).append(" varchar,\n")
                .append(ClassNode.CLASSTYPE).append(" varchar,\n")
                .append(MethodNode.METHOD).append(" varchar,\n")   // Might be CharUtils.empty if annotation
                .append(RuleNode.KIND).append(" varchar,\n")       // Might be CharUtils.empty if negative
                .append(RuleNode.CATEGORY).append(" varchar,\n")
                .append(RuleNode.RULE).append(" varchar);").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ImportRules() {
        ImportRules(Paths.get(FileUtils.RulesDirectory, "sources.csv").toString(), RuleNode.SOURCE);
        ImportRules(Paths.get(FileUtils.RulesDirectory, "sinks.csv").toString(), RuleNode.SINK);
        ImportRules(Paths.get(FileUtils.RulesDirectory, "gadget.csv").toString(), RuleNode.GADGET);
        ImportRules(Paths.get(FileUtils.RulesDirectory, "negative.csv").toString(), RuleNode.NEGATIVE);
    }

    private static void ImportRules(String rulePath, String category) {
        String sql = new StringBuilder()
                .append("INSERT INTO ").append(rulesTable).append(" (")
                .append(ClassNode.NAMESPACE).append(", ")
                .append(ClassNode.CLASSTYPE).append(", ")
                .append(MethodNode.METHOD).append(", ")
                .append(RuleNode.KIND).append(", ")
                .append(RuleNode.CATEGORY).append(", ")
                .append(RuleNode.RULE).append(") VALUES (?, ?, ?, ?, ?, ?)").toString();

        try {
            BufferedReader lineReader = new BufferedReader(new FileReader(rulePath));
            lineReader.lines().forEach(lineText -> {
                try {
                    PreparedStatement statement = conn.prepareStatement(sql);
                    String[] data = lineText.split(CharUtils.colon, -1);
                    statement.setString(1, data[0]);
                    statement.setString(2, data[1]);
                    statement.setString(3, CharUtils.replaceEmpty2Null(data[2]));
                    statement.setString(4, CharUtils.replaceEmpty2Null(data[3]));
                    statement.setString(5, category);
                    statement.setString(6, lineText);

                    statement.executeUpdate();
                    statement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            lineReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void CreateMethodsTable() {
        // TODO: sink annotation
        String sql = new StringBuilder()
                .append("CREATE TABLE ").append(methodsTable).append(" (\n")
                .append(CallGraphNode.METHODID).append(" integer PRIMARY KEY,\n")
                .append(ClassNode.NAMESPACE).append(" varchar,\n")
                .append(ClassNode.CLASSTYPE).append(" varchar,\n")
                .append(MethodNode.METHOD).append(" varchar,\n")
                .append(MethodNode.RETURNTYPE).append(" varchar,\n")
                .append(MethodNode.SIGNATURE).append(" varchar,\n")
                .append(MethodNode.SOURCECODE).append(" varchar,\n")
                .append(MethodNode.METHODLOCATION).append(" varchar,\n")
                .append(MethodNode.ISANNOTATION).append(" integer,\n")   // 1 is true, 0 is false
                .append(MethodNode.ISCONSTRUCTOR).append(" integer,\n")
                .append(MethodNode.ISMETHOD).append(" integer,\n")
                .append(MethodNode.ISWEBANNOTATIONSOURCE).append(" integer,\n")
                .append(MethodNode.ISWEBINVOCATIONSOURCE).append(" integer,\n")
                .append(MethodNode.ISNATIVEGADGETSOURCE).append(" integer,\n")
                .append(MethodNode.ISJSONGADGETSOURCE).append(" integer,\n")
                .append(MethodNode.ISSINKINVOCATION).append(" integer,\n")
                .append(MethodNode.ISSOURCEPROPAGATOR).append(" integer,\n")
                .append(MethodNode.ISSINKPROPAGATOR).append(" integer,\n")
                .append("UNIQUE (").append(MethodNode.RETURNTYPE).append(",").append(MethodNode.SIGNATURE).append(",").append(MethodNode.SOURCECODE).append(",").append(MethodNode.METHODLOCATION).append(")").append(");").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int ImportMethodNode(MethodNode methodNode) {
        String sql = new StringBuilder()
                .append("INSERT or IGNORE INTO ").append(methodsTable).append(" (")
                .append(ClassNode.NAMESPACE).append(", ")
                .append(ClassNode.CLASSTYPE).append(", ")
                .append(MethodNode.METHOD).append(", ")
                .append(MethodNode.RETURNTYPE).append(", ")
                .append(MethodNode.SIGNATURE).append(", ")
                .append(MethodNode.SOURCECODE).append(", ")
                .append(MethodNode.METHODLOCATION).append(", ")
                .append(MethodNode.ISANNOTATION).append(", ")
                .append(MethodNode.ISCONSTRUCTOR).append(", ")
                .append(MethodNode.ISMETHOD).append(", ")
                .append(MethodNode.ISWEBANNOTATIONSOURCE).append(", ")
                .append(MethodNode.ISWEBINVOCATIONSOURCE).append(", ")
                .append(MethodNode.ISNATIVEGADGETSOURCE).append(", ")
                .append(MethodNode.ISJSONGADGETSOURCE).append(", ")
                .append(MethodNode.ISSINKINVOCATION).append(", ")
                .append(MethodNode.ISSOURCEPROPAGATOR).append(", ")
                .append(MethodNode.ISSINKPROPAGATOR).append(") ")
                .append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").toString();

        ClassNode classNode = methodNode.getFullClasstype();

        PreparedStatement statement;
        int id = -1;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, classNode.getNamespace());
            statement.setString(2, classNode.getName());
            statement.setString(3, methodNode.getName());
            statement.setString(4, methodNode.getReturntype());
            statement.setString(5, methodNode.getSignature());
            statement.setString(6, methodNode.getSourceCode());
            statement.setString(7, Object2Json(methodNode.getMethodLocation()));
            statement.setInt(8, Boolean2Integer(methodNode.isAnnotationFlag()));
            statement.setInt(9, Boolean2Integer(methodNode.isConstructorFlag()));
            statement.setInt(10, Boolean2Integer(methodNode.isMethodFlag()));
            statement.setInt(11, Boolean2Integer(methodNode.isWebAnnotationSource()));
            statement.setInt(12, Boolean2Integer(methodNode.isWebInvocationSource()));
            statement.setInt(13, Boolean2Integer(methodNode.isNativeGadgetSource()));
            statement.setInt(14, Boolean2Integer(methodNode.isJsonGadgetSource()));
            statement.setInt(15, Boolean2Integer(methodNode.isSinkInvocation()));
            statement.setInt(16, Boolean2Integer(methodNode.isSourcePropagator()));
            statement.setInt(17, Boolean2Integer(methodNode.isSinkPropagator()));

            statement.executeUpdate();
            id = QueryExistingMethodNodeRowId(methodNode);

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    private static int QueryExistingMethodNodeRowId(MethodNode methodNode) {
        int id = -1;
        try {
            String sql = new StringBuilder().append("SELECT ").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(MethodNode.RETURNTYPE).append(" = ? AND ").append(MethodNode.SIGNATURE).append(" = ? AND ").append(MethodNode.SOURCECODE).append(" = ? AND ").append(MethodNode.METHODLOCATION).append(" = ?").toString();
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, methodNode.getReturntype());
            statement.setString(2, methodNode.getSignature());
            statement.setString(3, methodNode.getSourceCode());
            statement.setString(4, Object2Json(methodNode.getMethodLocation()));

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    private static void CreateInvocationsTable() {
        String sql = new StringBuilder().append("CREATE TABLE ").append(invocationsTable).append(" (\n")
                .append(CallGraphNode.INVOCATIONID).append(" integer primary key,\n")
                .append(InvocationNode.INVOCATIOMETHODID).append(" integer,\n")
                .append(ClassNode.NAMESPACE).append(" varchar,\n")
                .append(ClassNode.CLASSTYPE).append(" varchar,\n")
                .append(MethodNode.METHOD).append(" varchar,\n")
                .append(MethodNode.RETURNTYPE).append(" varchar,\n")
                .append(MethodNode.SIGNATURE).append(" varchar,\n")
                .append(MethodNode.SOURCECODE).append(" varchar,\n")
                .append(RuleNode.CATEGORY).append(" varchar,\n")
                .append(RuleNode.KIND).append(" varchar,\n")
                .append(RuleNode.RULE).append(" varchar,\n")
                .append(InvocationNode.SNIPPET).append(" varchar,\n")
                .append(InvocationNode.INVOCATIONLOCATION).append(" varchar,\n")
                .append("UNIQUE (").append(InvocationNode.INVOCATIOMETHODID).append(",").append(InvocationNode.INVOCATIONLOCATION).append("));").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int ImportInvocationNode(InvocationNode invocationNode) {
        String sql = new StringBuilder().append("INSERT or IGNORE INTO ").append(invocationsTable).append(" (")
                .append(InvocationNode.INVOCATIOMETHODID).append(", ")
                .append(ClassNode.NAMESPACE).append(", ")
                .append(ClassNode.CLASSTYPE).append(", ")
                .append(MethodNode.METHOD).append(", ")
                .append(MethodNode.RETURNTYPE).append(", ")
                .append(MethodNode.SIGNATURE).append(", ")
                .append(MethodNode.SOURCECODE).append(", ")
                .append(RuleNode.CATEGORY).append(", ")
                .append(RuleNode.KIND).append(", ")
                .append(RuleNode.RULE).append(", ")
                .append(InvocationNode.SNIPPET).append(", ")
                .append(InvocationNode.INVOCATIONLOCATION).append(") ")
                .append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").toString();

        MethodNode methodNode = invocationNode.getMethodNode();
        ClassNode classNode = methodNode.getFullClasstype();
        RuleNode ruleNode = invocationNode.getRuleNode();

        int id = -1;
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, invocationNode.getInvocationMethodID());
            statement.setString(2, classNode.getNamespace());
            statement.setString(3, classNode.getName());
            statement.setString(4, methodNode.getName());
            statement.setString(5, methodNode.getReturntype());
            statement.setString(6, methodNode.getSignature());
            statement.setString(7, methodNode.getSourceCode());
            statement.setString(8, ruleNode == null ? null : ruleNode.getCategory());
            statement.setString(9, ruleNode == null ? null : ruleNode.getKind());
            statement.setString(10, ruleNode == null ? null : ruleNode.getRule());
            statement.setString(11, invocationNode.getSnippet());
            statement.setString(12, Object2Json(invocationNode.getInvocationLocation()));

            statement.executeUpdate();
            id = QueryExistingInvocationNodeRowId(invocationNode);

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }


    // TODO one invocationMethodID might have several invocationIDs in invocations table.
    private static int QueryExistingInvocationNodeRowId(InvocationNode invocationNode) {
        int id = -1;
        try {
            String sql = new StringBuilder().append("SELECT ").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(InvocationNode.INVOCATIOMETHODID).append(" = ? AND ").append(InvocationNode.INVOCATIONLOCATION).append(" = ?").toString();
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, invocationNode.getInvocationMethodID());
            statement.setString(2, Object2Json(invocationNode.getInvocationLocation()));

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    private static void CreateCallgraphsTable() {
        String sql = new StringBuilder().append("CREATE TABLE ").append(callgraphsTable).append(" (\n")
                .append(CallGraphNode.METHODID).append(" integer ,\n")
                .append(CallGraphNode.INVOCATIONID).append(" integer,\n")
                .append(CallGraphNode.INTRAFLOW).append(" varchar );").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ImportCallgraphNode(CallGraphNode callGraphNode) {
        String sql = new StringBuilder("INSERT or IGNORE INTO ").append(callgraphsTable).append(" (")
                .append(CallGraphNode.METHODID).append(", ")
                .append(CallGraphNode.INVOCATIONID).append(", ")
                .append(CallGraphNode.INTRAFLOW).append(") VALUES (?, ?, ?)").toString();

        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, callGraphNode.getMethodID());
            statement.setInt(2, callGraphNode.getInvocationID());
            statement.setString(3, Object2Json(callGraphNode.getIntraflow()));

            statement.executeUpdate();

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static InvocationNode QueryInvocationAnnotationNode(String namespace, String classtype) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE ").append(ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(RuleNode.KIND).append(" LIKE 'annotation%'").toString();
        RuleNode ruleNode = QueryRuleNode(sql, namespace, classtype, null);

        InvocationNode invocationNode = new InvocationNode();
        invocationNode.setRuleNode(ruleNode);
        MethodNode methodNode = ruleNode.getMethodNode();
        invocationNode.setMethodNode(methodNode);
        return invocationNode;
    }

    public static InvocationNode QueryInvocationConstructorNode(String namespace, String classtype) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE "+ ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(MethodNode.METHOD).append(" = '<init>'").toString();
        RuleNode ruleNode = QueryRuleNode(sql, namespace, classtype, "<init>");

        InvocationNode invocationNode = new InvocationNode();
        invocationNode.setRuleNode(ruleNode);
        MethodNode methodNode = ruleNode.getMethodNode();
        invocationNode.setMethodNode(methodNode);
        return invocationNode;
    }

    public static RuleNode QueryInvocationMethodNode(String namespace, String classtype, String methodname) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE ").append(ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(RuleNode.CATEGORY).append(" != ").append("\"").append(RuleNode.GADGET).append("\"").toString();
        return QueryRuleNode(sql, namespace, classtype, methodname);
    }

    public static InvocationNode QueryInvocationMethodNode(CtExecutableReference<?> ctExecutableReference) {
        MethodNode methodNode = QueryNativeGadgetSourceMethodNode(ctExecutableReference);
        // Todo should this be improved? because processmethod will repeat the QueryInvocationMethodNode() again later.
        RuleNode ruleNode =
                QueryInvocationMethodNode(
                        ctExecutableReference.getDeclaringType().getTopLevelType().getPackage().getQualifiedName(),
                        ctExecutableReference.getDeclaringType().getTypeDeclaration().getSimpleName(),
                        ctExecutableReference.getDeclaringType().getSimpleName()
                );
        InvocationNode invocationNode = new InvocationNode();
        ruleNode.setMethodNode(methodNode);
        invocationNode.setMethodNode(methodNode);
        invocationNode.setRuleNode(ruleNode);
        return invocationNode;
    }

    private static MethodNode QueryNativeGadgetSourceMethodNode(String namespace, String classtype, String methodname) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE ").append(ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(RuleNode.CATEGORY).append(" = ").append("\"").append(RuleNode.GADGET).append("\"").toString();
        RuleNode ruleNode = QueryRuleNode(sql, namespace, classtype, methodname);

        return ruleNode.getMethodNode();
    }

    public static MethodNode QueryNativeGadgetSourceMethodNode(CtExecutableReference<?> ctExecutableReference) {
        MethodNode methodNode = new MethodNode();

        ClassNode classNode = new ClassNode();
        CtTypeReference<?> executableInvocation = ctExecutableReference.getDeclaringType();
        classNode.setNamespace(executableInvocation.getTopLevelType().getPackage().getQualifiedName());
        classNode.setName(executableInvocation.getTypeDeclaration().getSimpleName());
        CtType<?> clazz = executableInvocation.getDeclaration();
        methodNode.setNativeGadgetSource(methodNode.isNativeGadgetSource() && (MethodHierarchy.isSerializable(clazz) || MethodHierarchy.isExternalizable(clazz)));
        methodNode.setFullClasstype(classNode);
        methodNode.setName(ctExecutableReference.getSimpleName());
        methodNode.setReturntype(ctExecutableReference.getType().getQualifiedName());
        methodNode.setSignature(ctExecutableReference.getSignature());

        try {
            methodNode.setSourceCode(
                    ctExecutableReference.getExecutableDeclaration().getOriginalSourceFragment().getSourceCode());
            methodNode.setMethodLocation(
                    SpoonUtils.ConvertPosition2Location(methodNode,
                            ctExecutableReference.getExecutableDeclaration().getOriginalSourceFragment().getSourcePosition()));
        } catch (Exception e) {
            methodNode.setSourceCode(ctExecutableReference.toString());
            methodNode.setMethodLocation(new Location());
        }

        if (ctExecutableReference.getDeclaringType().getDeclaration() == null) {
            return methodNode;
        }

        MethodHierarchy methodHierarchy = new MethodHierarchy();
        methodHierarchy.FindMethodDefinition(
                ctExecutableReference.getDeclaringType().getDeclaration(),
                ctExecutableReference.getSimpleName(),
                ctExecutableReference.getParameters());


        HashSet<String> methodSet = methodHierarchy.getMethodSet();
        for (String qualifiedName : methodSet) {
            String namespace = FilenameUtils.getBaseName(qualifiedName);
            String classtype = FilenameUtils.getExtension(qualifiedName);
            methodNode = QueryNativeGadgetSourceMethodNode(namespace, classtype,
                    ctExecutableReference.getSimpleName());
            if (methodNode.isNativeGadgetSource()) {
                break;
            }
        }

        methodNode.setFullClasstype(classNode);
        return methodNode;
    }

    public static MethodNode QueryNegativeNode(String namespace, String classtype, String methodname) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE ").append(ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(RuleNode.CATEGORY).append(" = ").append("\"").append(RuleNode.NEGATIVE).append("\"").toString();
        RuleNode ruleNode = QueryRuleNode(sql, namespace, classtype, methodname);

        return ruleNode.getMethodNode();
    }

    private static RuleNode QueryRuleNode(String sql, String namespace, String classtype, String methodname) {
        RuleNode ruleNode = new RuleNode();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, namespace);
            statement.setString(2, classtype);

            ResultSet resultSet = statement.executeQuery();

            ClassNode classNode = new ClassNode();
            classNode.setNamespace(namespace);
            classNode.setName(classtype);

            MethodNode methodNode = new MethodNode();
            methodNode.setFullClasstype(classNode);
            methodNode.setName(methodname);
            ruleNode.setMethodNode(methodNode);

            if (methodname == null) {
                ruleNode.getMethodNode().setAnnotationFlag(true);
            } else if (methodname.equals(classtype)) {
                ruleNode.getMethodNode().setConstructorFlag(true);
            } else {
                ruleNode.getMethodNode().setMethodFlag(true);
            }

            while(resultSet.next()) {
                String method = resultSet.getString(MethodNode.METHOD);
                if (method == null ||
                        method.equals(methodname) ||
                        (method.startsWith(CharUtils.leftbracket) && CharUtils.RegexMatch(method, methodname))) {
                    ruleNode.setKind(resultSet.getString(RuleNode.KIND));
                    ruleNode.setRule(resultSet.getString(RuleNode.RULE));

                    String category = resultSet.getString(RuleNode.CATEGORY);

                    if (category.equals(RuleNode.SOURCE)) {
                        if (resultSet.getString(RuleNode.KIND).contains("annotation") || methodname == null) {
                            methodNode.setWebAnnotationSource(true);
                        } else {
                            methodNode.setWebInvocationSource(true);
                        }
                    } else if (category.equals(RuleNode.SINK)) {
                        methodNode.setSinkInvocation(true);
                    } else if (category.equals(RuleNode.GADGET)) {
                        methodNode.setNativeGadgetSource(true);
                    }

                    ruleNode.setMethodNode(methodNode);
                    ruleNode.setCategory(resultSet.getString(RuleNode.CATEGORY));
                    break;
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (methodname != null &&
                (methodname.startsWith("set") ||
                        methodname.startsWith("get") ||
                        methodname.equals("<init>") ||
                        methodname.equals(ruleNode.getMethodNode().getFullClasstype().getName()))) {
            ruleNode.getMethodNode().setJsonGadgetSource(true);
        }

        return ruleNode;
    }

    public static List<InvocationNode> QueryExistingWebInvocationSourceInvocationNode() {
        String sql = new StringBuilder().append("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" in (SELECT ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISWEBINVOCATIONSOURCE).append(" = 1)").toString();
        try {
            Statement statement = conn.createStatement();
            List<InvocationNode> invocationNodes = QueryInvocationNode(statement.executeQuery(sql));
            statement.close();
            return invocationNodes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Search all invocations in the same methods with the web sources invocations
    // and find out if there's a data flow from web source invocations to these invocations
    // at last store the data flow to callgraphs table
    /*
     * SELECT * FROM invocations WHERE invocations.InvocationID in (SELECT callgraphs.InvocationID FROM callgraphs WHERE callgraphs.MethodID in (SELECT callgraphs.MethodID FROM callgraphs WHERE callgraphs.InvocationID in (SELECT invocations.InvocationID FROM invocations WHERE invocations.InvocationMethodID = 3)));
     */
    public static void ImportWebInvocationSourceFlow() {
        List<InvocationNode> webInvocationSourceInvocationNodes = QueryExistingWebInvocationSourceInvocationNode();
        for (InvocationNode webInvocationSourceInvocationNode : webInvocationSourceInvocationNodes) {
            String sql = new StringBuilder().append("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" = ?)))").toString();

            PreparedStatement statement;
            try {
                statement = conn.prepareStatement(sql);
                statement.setInt(1, webInvocationSourceInvocationNode.getInvocationMethodID());

                List<InvocationNode> invocationsNodesInSameMethod = QueryInvocationNode(statement.executeQuery());
                for (InvocationNode invocationsNodeInSameMethod : invocationsNodesInSameMethod) {
                    ThreadFlow intraflow = SemgrepUtils.DetectIntraFlow(webInvocationSourceInvocationNode, invocationsNodeInSameMethod);

                    CallGraphNode callGraphNode = new CallGraphNode();
                    callGraphNode.setMethodID(webInvocationSourceInvocationNode.getInvocationMethodID());
                    callGraphNode.setInvocationID(invocationsNodeInSameMethod.getInvocationID());
                    callGraphNode.setIntraflow(intraflow);

                    DbUtils.ImportCallgraphNode(callGraphNode);
                }
                statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static MethodNode QueryMethodNodeFromWebSourceInvocation(int MethodID) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" = ?))").toString();
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, MethodID);

            List<MethodNode> ParentMethodNode = QueryMethodNode(statement.executeQuery());
            statement.close();
            return ParentMethodNode.get(0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<MethodNode> QueryMethodNode(ResultSet resultSet) {
        List<MethodNode> methodNodes = new ArrayList<>();

        try {
            while(resultSet.next()) {
                MethodNode methodNode = new MethodNode();

                methodNode.setMethodID(resultSet.getInt(CallGraphNode.METHODID));
                ClassNode classNode = new ClassNode();
                classNode.setNamespace(resultSet.getString(ClassNode.NAMESPACE));
                classNode.setName(resultSet.getString(ClassNode.CLASSTYPE));
                methodNode.setFullClasstype(classNode);
                methodNode.setName(resultSet.getString(MethodNode.METHOD));
                methodNode.setReturntype(resultSet.getString(MethodNode.RETURNTYPE));
                methodNode.setSignature(resultSet.getString(MethodNode.SIGNATURE));
                methodNode.setSourceCode(resultSet.getString(MethodNode.SOURCECODE));
                methodNode.setMethodLocation(
                        (Location) CharUtils.Json2Object(resultSet.getString(MethodNode.METHODLOCATION), Location.class));
                methodNode.setAnnotationFlag(Integer2Boolean(resultSet.getInt(MethodNode.ISANNOTATION)));
                methodNode.setConstructorFlag(Integer2Boolean(resultSet.getInt(MethodNode.ISCONSTRUCTOR)));
                methodNode.setMethodFlag(Integer2Boolean(resultSet.getInt(MethodNode.ISMETHOD)));
                methodNode.setWebAnnotationSource(Integer2Boolean(resultSet.getInt(MethodNode.ISWEBANNOTATIONSOURCE)));
                methodNode.setWebInvocationSource(Integer2Boolean(resultSet.getInt(MethodNode.ISWEBINVOCATIONSOURCE)));
                methodNode.setNativeGadgetSource(Integer2Boolean(resultSet.getInt(MethodNode.ISNATIVEGADGETSOURCE)));
                methodNode.setJsonGadgetSource(Integer2Boolean(resultSet.getInt(MethodNode.ISJSONGADGETSOURCE)));
                methodNode.setSinkInvocation(Integer2Boolean(resultSet.getInt(MethodNode.ISSINKINVOCATION)));
                methodNode.setSourcePropagator(Integer2Boolean(resultSet.getInt(MethodNode.ISSOURCEPROPAGATOR)));
                methodNode.setSinkPropagator(Integer2Boolean(resultSet.getInt(MethodNode.ISSINKPROPAGATOR)));

                methodNodes.add(methodNode);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return methodNodes;
    }

    private static List<InvocationNode> QueryInvocationNode(ResultSet resultSet) {
        List<InvocationNode> invocationNodes = new ArrayList<>();

        try {
            while(resultSet.next()) {
                InvocationNode invocationNode = new InvocationNode();

                invocationNode.setInvocationID(resultSet.getInt(CallGraphNode.INVOCATIONID));
                invocationNode.setInvocationMethodID(resultSet.getInt(InvocationNode.INVOCATIOMETHODID));
                ClassNode classNode = new ClassNode();
                classNode.setNamespace(resultSet.getString(ClassNode.NAMESPACE));
                classNode.setName(resultSet.getString(ClassNode.CLASSTYPE));
                RuleNode ruleNode = new RuleNode();
                ruleNode.setCategory(resultSet.getString(RuleNode.CATEGORY));
                ruleNode.setKind(resultSet.getString(RuleNode.KIND));
                ruleNode.setRule(resultSet.getString(RuleNode.RULE));

                invocationNode.setRuleNode(ruleNode);
                invocationNode.setSnippet(resultSet.getString(InvocationNode.SNIPPET));
                invocationNode.setInvocationLocation(
                        (Location) CharUtils.Json2Object(resultSet.getString(InvocationNode.INVOCATIONLOCATION), Location.class));

                String sql = new StringBuilder().append("SELECT * FROM ").append(methodsTable).append(" WHERE ").append(CallGraphNode.METHODID).append(" = ").append(invocationNode.getInvocationMethodID()).toString();
                Statement statement = conn.createStatement();
                List<MethodNode> methodNodes;
                methodNodes = QueryMethodNode(statement.executeQuery(sql));
                statement.close();
                MethodNode methodNode = methodNodes.get(0);
                methodNode.setFullClasstype(classNode);
                invocationNode.setMethodNode(methodNode); // Will only have one MethodNode

                invocationNodes.add(invocationNode);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invocationNodes;
    }

    public static List<InvocationNode> QuerySinkNodes() {
        List<InvocationNode> invocationNodes = new ArrayList<>();

        String sql = new StringBuilder("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" in (SELECT ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISSINKINVOCATION).append(" = 1)").toString();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            invocationNodes = QueryInvocationNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return invocationNodes;
    }

    // TODO there are duplicate annotation sources.
    public static List<InvocationNode> QuerySourceNodes() {
        List<InvocationNode> invocationNodes = new ArrayList<>();

        String sql = new StringBuilder().append("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" in (SELECT ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISWEBINVOCATIONSOURCE).append(" = 1 OR (").append(methodsTable).append(".").append(MethodNode.ISWEBANNOTATIONSOURCE).append(" = 1 AND ").append(methodsTable).append(".").append(MethodNode.ISANNOTATION).append(" = 1))").toString();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            invocationNodes = QueryInvocationNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return invocationNodes;
    }

    public static List<Result> QuerySinkGadgets() {
        List<Result> results = new ArrayList<>();
        String sql = new StringBuilder("SELECT * FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.INTRAFLOW).append(" IS NOT NULL AND ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append( invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" in (SELECT ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISSINKINVOCATION).append(" = 1))").toString();

        try (Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(sql);
            while(resultSet.next()) {
                ThreadFlow intraflow = (ThreadFlow) Json2Object(resultSet.getString(CallGraphNode.INTRAFLOW), ThreadFlow.class);
                List<ThreadFlow> threadFlows = new ArrayList<>();
                threadFlows.add(intraflow);

                List<CodeFlow> codeFlows = new ArrayList<>();
                codeFlows.add(new CodeFlow().withThreadFlows(threadFlows));

                List<Location> resultLocation = new ArrayList<>();
                resultLocation.add(intraflow.getLocations().get(1).getLocation());
                Result result = new Result()
                        .withMessage(new Message().withText(
                                "[P2 - Sink Gadget Method] " + intraflow.getMessage().getText()))
                        .withLocations(resultLocation)
                        .withCodeFlows(codeFlows)
                        .withRuleId(QueryRuleFromCallgraph(resultSet.getInt(CallGraphNode.INVOCATIONID)));
                results.add(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    private static String QueryRuleFromCallgraph(int InvocationID) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" = ?").toString();

        String rule = empty;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, InvocationID);
            ResultSet resultSet = stmt.executeQuery();
            rule = QueryInvocationNode(resultSet).get(0).getRuleNode().getRule();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rule;
    }


    // Update methods with source annotation as annotation source method
    public static void UpdateParentMethodAsWebAnnoationSource(int methodID) {
        String sql = new StringBuilder().append("UPDATE ").append(methodsTable).append(" SET ").append(MethodNode.ISWEBANNOTATIONSOURCE).append(" = 1 WHERE ").append(CallGraphNode.METHODID).append(" = ?").toString();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, methodID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Update all sources to propagator as well
    /*
     * UPDATE methods SET isSourcePropagator = 1 WHERE isWebAnnotationSource = 1 OR isWebInvocationSource = 1 OR isNativeGadgetSource = 1 OR isJsonGadgetSource = 1
     */
    public static int UpdateSources2SourcePropagator() {
        String sql = new StringBuilder().append("UPDATE ").append(methodsTable).append(" SET ").append(MethodNode.ISSOURCEPROPAGATOR).append(" = 1 WHERE ").append(MethodNode.ISWEBANNOTATIONSOURCE).append(" = 1 OR ").append(MethodNode.ISWEBINVOCATIONSOURCE).append(" = 1 OR ").append(MethodNode.ISNATIVEGADGETSOURCE).append(" = 1 OR ").append(MethodNode.ISJSONGADGETSOURCE).append(" = 1").toString();

        int updateRows = 0;
        try (Statement stmt = conn.createStatement()) {
            updateRows = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updateRows;
    }

    /*
     * UPDATE methods SET isSinkPropagator = 1 WHERE isSinkInvocation = 1
     */
    public static int UpdateSinks2SinkPropagator() {
        String sql = new StringBuilder().append("UPDATE methods SET ").append(MethodNode.ISSINKPROPAGATOR).append(" = 1 WHERE ").append(MethodNode.ISSINKINVOCATION).append(" = 1").toString();

        int updateRows = 0;
        try (Statement stmt = conn.createStatement()) {
            updateRows = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updateRows;
    }

    // Propagate from one source propagator to another source propagator in forward way
    /*
     * UPDATE methods SET isSourcePropagator = 1 WHERE methods.isSourcePropagator != 1 AND methods.MethodID in (SELECT invocations.InvocationMethodID FROM invocations WHERE invocations.InvocationID in (SELECT callgraphs.InvocationID FROM callgraphs WHERE callgraphs.intraflow IS NOT NULL AND callgraphs.MethodID in (SELECT methods.MethodID FROM methods WHERE methods.isSourcePropagator = 1 AND methods.isSinkPropagator != 1)))
     */
    // special case: after last time propagation, now the chain is like source -> source propagator -> sink propagator -> sink, so there might exist two nodes which is source propagator and sink propagator at the same time. And we always propagate from sink at first for convenience in finding convergences, so check if node is sink propagator before setting source propagator to the successor node.
    public static int UpdateForwardPropagator() {
        String sql = new StringBuilder().append("UPDATE ").append(methodsTable).append(" SET ").append(MethodNode.ISSOURCEPROPAGATOR).append(" = 1 WHERE ").append(methodsTable).append(".").append(MethodNode.ISSOURCEPROPAGATOR).append(" != 1 AND ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.INTRAFLOW).append(" IS NOT NULL AND ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISSOURCEPROPAGATOR).append(" = 1 AND ").append(methodsTable).append(".").append(MethodNode.ISSINKPROPAGATOR).append(" != 1)))").toString();

        int updateRows = 0;
        try (Statement stmt = conn.createStatement()) {
            updateRows = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updateRows;
    }

    /*
     * UPDATE methods SET isSinkPropagator = 1 WHERE methods.isSinkPropagator != 1 AND methods.MethodID in (SELECT callgraphs.MethodID FROM callgraphs where callgraphs.intraflow IS NOT NULL AND callgraphs.InvocationID in (SELECT invocations.InvocationID FROM invocations WHERE invocations.InvocationMethodID in (SELECT methods.MethodID FROM methods WHERE methods.isSinkPropagator = 1 AND methods.isSourcePropagator != 1)))
     */
    public static int UpdateBackwardPropagator() {
        String sql = new StringBuilder().append("UPDATE ").append(methodsTable).append(" SET ").append(MethodNode.ISSINKPROPAGATOR).append(" = 1 WHERE ").append(methodsTable).append(".").append(MethodNode.ISSINKPROPAGATOR).append(" != 1 AND ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(callgraphsTable).append(" where ").append(callgraphsTable).append(".").append(CallGraphNode.INTRAFLOW).append(" IS NOT NULL AND ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" in (SELECT ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISSINKPROPAGATOR).append(" = 1 AND ").append(methodsTable).append(".").append(MethodNode.ISSOURCEPROPAGATOR).append(" != 1)))").toString();

        int updateRows = 0;
        try (Statement stmt = conn.createStatement()) {
            updateRows = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updateRows;
    }

    public static List<MethodNode> QueryPropagatorConvergence() {
        String sql = new StringBuilder().append("SELECT * FROM ").append(methodsTable).append(" WHERE ").append(MethodNode.ISSINKPROPAGATOR).append(" = 1 AND ").append(MethodNode.ISSOURCEPROPAGATOR).append(" = 1").toString();

        List<MethodNode> convergences = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            convergences = QueryMethodNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return convergences;
    }

    @Nullable
    private static InvocationNode QueryInvocationNodeOfMethodNode(MethodNode methodNode) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" = ?").toString();

        List<InvocationNode> invocationNodes = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, methodNode.getMethodID());
            ResultSet rs = stmt.executeQuery();
            invocationNodes = QueryInvocationNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            return invocationNodes.get(0); // Might be null
        } catch (Exception e) {
            return null;
        }
    }

    public static Location QueryInvocationLocationOfMethodNode(MethodNode methodNode) {
        InvocationNode invocationNode = QueryInvocationNodeOfMethodNode(methodNode);
        try {
            return invocationNode.getInvocationLocation();
        } catch (Exception e) {
            return methodNode.getMethodLocation();
        }
    }

    public static String QueryInvocationSnippetOfMethodNode(MethodNode methodNode) {
        InvocationNode invocationNode = QueryInvocationNodeOfMethodNode(methodNode);
        try {
            return invocationNode.getSnippet();
        } catch (Exception e) {
            return methodNode.getSignature();
        }
    }

    public static void QueryPredNodes(MethodNode methodNode, DefaultMutableTreeNode treeNode, Set<Integer> unique) {
        // TODO BUG When found source node, should continue to propagate till non-pred node is found
        DefaultMutableTreeNode treeNodePropagatorClone = (DefaultMutableTreeNode) treeNode.clone();
        if (methodNode.isWebAnnotationSource() ||
                methodNode.isNativeGadgetSource() ||
                methodNode.isJsonGadgetSource()) {
            treeNode.setUserObject(methodNode.getMethodLocation());
            String text = "Source: '" + methodNode.getSignature() + "'";
            ((Location) treeNode.getUserObject()).setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getRegion().setMessage(new Message().withText(text));

            ((Location) treeNode.getUserObject()).getPhysicalLocation().getArtifactLocation().setDescription(
                    new Message().withText(getSourceType(methodNode)));
        } else if (methodNode.isWebInvocationSource()) {
            String text = "Source: '" + QueryInvocationNodeOfMethodNode(methodNode).getSnippet() + "'";
            ((Location) treeNode.getUserObject()).setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getArtifactLocation().setDescription(
                    new Message().withText(getSourceType(methodNode)));
        } else {
            setPropagator(methodNode, treeNode);
        }

        String sql = new StringBuilder().append("SELECT * FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISSOURCEPROPAGATOR).append(" = 1 AND ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.INTRAFLOW).append(" IS NOT NULL AND ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(InvocationNode.INVOCATIOMETHODID).append(" = ?))").toString();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, methodNode.getMethodID());
            ResultSet rs = stmt.executeQuery();
            List<MethodNode> methodNodes = QueryMethodNode(rs);

            for (MethodNode predNode: methodNodes) {
                DefaultMutableTreeNode predTreeNode = new DefaultMutableTreeNode(QueryInvocationLocationOfMethodNode(predNode));
                if (unique.add(predNode.getMethodID())) {
                    if (getSourceType(methodNode) != null && treeNode.getParent() != null) {
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();
                        setPropagator(methodNode, treeNodePropagatorClone);
                        parentNode.add(treeNodePropagatorClone);
                        treeNodePropagatorClone.add(predTreeNode);
                    } else {
                        treeNode.add(predTreeNode);
                    }
                    QueryPredNodes(predNode, predTreeNode, unique);
                    unique.remove(predNode.getMethodID());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static String getSourceType(MethodNode methodNode) {
        if (methodNode.isWebAnnotationSource() || methodNode.isWebInvocationSource()) {
            return "Web";
        } else if (methodNode.isNativeGadgetSource()) {
            return "Native Gadget";
        } else if (methodNode.isJsonGadgetSource()) {
            return "Json Gadget";
        } else {
            return null;
        }
    }

    public static void QuerySuccNodes(MethodNode methodNode, DefaultMutableTreeNode treeNode, Set<Integer> unique) {
        // TODO BUG When found sink node, should continue to propagate till non-succ node is found
        DefaultMutableTreeNode treeNodePropagatorClone = (DefaultMutableTreeNode) treeNode.clone();
        if (methodNode.isSinkInvocation()) {
            InvocationNode invocationNode = QueryInvocationNodeOfMethodNode(methodNode);
            String text = "Sink: '" + invocationNode.getSnippet() + "'";
            ((Location) treeNode.getUserObject()).setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getArtifactLocation().setDescription(
                    new Message().withText(invocationNode.getRuleNode().getRule()));
        } else {
            setPropagator(methodNode, treeNode);
        }

        String sql = new StringBuilder().append("SELECT * FROM ").append(methodsTable).append(" WHERE ").append(methodsTable).append(".").append(MethodNode.ISSINKPROPAGATOR).append(" = 1 AND ").append(methodsTable).append(".").append(CallGraphNode.METHODID).append(" in (SELECT ").append(invocationsTable).append(".").append(InvocationNode.INVOCATIOMETHODID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" in (SELECT ").append(callgraphsTable).append(".").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(callgraphsTable).append(" WHERE ").append(callgraphsTable).append(".").append(CallGraphNode.INTRAFLOW).append(" IS NOT NULL AND ").append(callgraphsTable).append(".").append(CallGraphNode.METHODID).append(" = ?))").toString();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, methodNode.getMethodID());
            ResultSet rs = stmt.executeQuery();
            List<MethodNode> methodNodes = QueryMethodNode(rs);
            for (MethodNode succNode: methodNodes) {
                DefaultMutableTreeNode succTreeNode = new DefaultMutableTreeNode(QueryInvocationLocationOfMethodNode(succNode));
                if (unique.add(succNode.getMethodID())) {
                    if (methodNode.isSinkInvocation() && treeNode.getParent() != null) {
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();
                        setPropagator(methodNode, treeNodePropagatorClone);
                        parentNode.add(treeNodePropagatorClone);
                        treeNodePropagatorClone.add(succTreeNode);
                    } else {
                        treeNode.add(succTreeNode);
                    }
                    QuerySuccNodes(succNode, succTreeNode, unique);
                    unique.remove(succNode.getMethodID());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void setPropagator(MethodNode methodNode, DefaultMutableTreeNode treeNode) {
        Location location = (Location) treeNode.getUserObject();
        String text = "Propagator: '" + QueryInvocationSnippetOfMethodNode(methodNode) + "'";
        location.setMessage(new Message().withText(text));
        location.getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
    }
}
