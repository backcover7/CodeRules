package com.saucer.sast.utils;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.parser.core.MethodHierarchy;
import com.saucer.sast.lang.java.parser.nodes.RuleNode;
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
    //todo
    public final static String dbname = Paths.get("target", "saucer.db").toAbsolutePath().normalize().toString();
    public final static String rulesTable = "rules";
    public final static String sourcenodeTable = "sourcenodes";
    public final static String invocationsTable = "invocations";
    public final static String fieldsTable = "fields";
    public final static String sourcenodeCGTable = "sourcenodeCG";

    public final static String invocationCGTable = "invocationCG";

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
        CreateSourcecodeTable();
        CreateInvocationsTable();
        CreateSourceNodeCallgraphsTable();
        CreateInvocationCallgraphsTable();
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

    private static void CreateSourcecodeTable() {
        String sql = new StringBuilder()
                .append("CREATE TABLE ").append(sourcenodeTable).append(" (\n")
                .append(CallGraphNode.METHODID).append(" integer PRIMARY KEY,\n")
                .append(ClassNode.NAMESPACE).append(" varchar,\n")
                .append(ClassNode.CLASSTYPE).append(" varchar,\n")
                .append(MethodNode.METHOD).append(" varchar,\n")
                .append(MethodNode.RETURNTYPE).append(" varchar,\n")
                .append(MethodNode.SIGNATURE).append(" varchar,\n")
                .append(MethodNode.SOURCECODE).append(" varchar,\n")
                .append(MethodNode.METHODLOCATION).append(" varchar,\n")
                .append(RuleNode.CATEGORY).append(" varchar,\n")
                .append(RuleNode.KIND).append(" varchar,\n")
                .append(RuleNode.RULE).append(" varchar,\n")
                .append(RuleNode.ISANNOTATION).append(" integer,\n")
                .append(RuleNode.ISCONSTRUCTOR).append(" integer,\n")
                .append(RuleNode.ISMETHOD).append(" integer,\n")
                .append(RuleNode.ISWEBANNOTATIONSOURCE).append(" integer,\n")
                .append(RuleNode.ISNATIVEGADGETSOURCE).append(" integer,\n")
                .append(RuleNode.ISJSONGADGETSOURCE).append(" integer,\n")
                .append(RuleNode.ISWEBINVOCATIONSOURCE).append(" integer,\n")
                .append(RuleNode.ISINVOCATIONSINK).append(" integer,\n")
                .append(RuleNode.ISANNOTATIONSINK).append(" integer,\n")
                .append(RuleNode.ISSOURCEPROPAGATOR).append(" integer,\n")
                .append(RuleNode.ISSINKPROPAGATOR).append(" integer,\n")
                .append("UNIQUE (").append(MethodNode.RETURNTYPE).append(",").append(MethodNode.SIGNATURE).append(",").append(MethodNode.SOURCECODE).append(",").append(MethodNode.METHODLOCATION).append(")").append(");").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int ImportSourcecode(SourceNode sourceNode) {
        String sql = new StringBuilder()
                .append("INSERT or IGNORE INTO ").append(sourcenodeTable).append(" (")
                .append(ClassNode.NAMESPACE).append(", ")
                .append(ClassNode.CLASSTYPE).append(", ")
                .append(MethodNode.METHOD).append(", ")
                .append(MethodNode.RETURNTYPE).append(", ")
                .append(MethodNode.SIGNATURE).append(", ")
                .append(MethodNode.SOURCECODE).append(", ")
                .append(MethodNode.METHODLOCATION).append(", ")
                .append(RuleNode.CATEGORY).append(", ")
                .append(RuleNode.KIND).append(", ")
                .append(RuleNode.RULE).append(", ")
                .append(RuleNode.ISANNOTATION).append(", ")
                .append(RuleNode.ISCONSTRUCTOR).append(", ")
                .append(RuleNode.ISMETHOD).append(", ")
                .append(RuleNode.ISWEBANNOTATIONSOURCE).append(", ")
                .append(RuleNode.ISNATIVEGADGETSOURCE).append(", ")
                .append(RuleNode.ISJSONGADGETSOURCE).append(", ")
                .append(RuleNode.ISWEBINVOCATIONSOURCE).append(", ")
                .append(RuleNode.ISINVOCATIONSINK).append(", ")
                .append(RuleNode.ISANNOTATIONSINK).append(", ")
                .append(RuleNode.ISSOURCEPROPAGATOR).append(", ")
                .append(RuleNode.ISSINKPROPAGATOR)
                .append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").toString();

        MethodNode methodNode = sourceNode.getMethodNode();
        SimpleMethodNode simpleMethodNode = methodNode.getSimpleMethodNode();
        ClassNode classNode = simpleMethodNode.getFullClasstype();
        RuleNode ruleNode = sourceNode.getRuleNode();

        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, classNode.getNamespace());
            statement.setString(2, classNode.getName());
            statement.setString(3, simpleMethodNode.getName());
            statement.setString(4, methodNode.getReturntype());
            statement.setString(5, methodNode.getSignature());
            statement.setString(6, methodNode.getSourceCode());
            statement.setString(7, Object2Json(methodNode.getMethodLocation()));
            statement.setString(8, ruleNode.getCategory());
            statement.setString(9, ruleNode.getKind());
            statement.setString(10, ruleNode.getRule());
            statement.setInt(11, Boolean2Integer(ruleNode.isAnnotationFlag()));
            statement.setInt(12, Boolean2Integer(ruleNode.isConstructorFlag()));
            statement.setInt(13, Boolean2Integer(ruleNode.isMethodFlag()));
            statement.setInt(14, Boolean2Integer(ruleNode.isWebAnnotationSource()));
            statement.setInt(15, Boolean2Integer(ruleNode.isNativeGadgetSource()));
            statement.setInt(16, Boolean2Integer(ruleNode.isJsonGadgetSource()));
            statement.setInt(17, Boolean2Integer(ruleNode.isWebInvocationSource()));
            statement.setInt(18, Boolean2Integer(ruleNode.isInvocationSink()));
            statement.setInt(19, Boolean2Integer(ruleNode.isAnnotationSink()));
            statement.setInt(20, Boolean2Integer(ruleNode.isSourcePropagator()));
            statement.setInt(21, Boolean2Integer(ruleNode.isSinkPropagator()));

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return QueryExistingSourceNodeID(sourceNode.getMethodNode());
    }

    private static int QueryExistingSourceNodeID(MethodNode methodNode) {
        int id = -1;
        try {
            String sql = new StringBuilder().append("SELECT ").append(CallGraphNode.METHODID).append(" FROM ").append(sourcenodeTable).append(" WHERE ").append(MethodNode.RETURNTYPE).append(" = ? AND ").append(MethodNode.SIGNATURE).append(" = ? AND ").append(MethodNode.SOURCECODE).append(" = ? AND ").append(MethodNode.METHODLOCATION).append(" = ?").toString();
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
                .append(ClassNode.NAMESPACE).append(" varchar,\n")
                .append(ClassNode.CLASSTYPE).append(" varchar,\n")
                .append(MethodNode.METHOD).append(" varchar,\n")
                .append(MethodNode.RETURNTYPE).append(" varchar,\n")
                .append(MethodNode.SIGNATURE).append(" varchar,\n")
                .append(MethodNode.SOURCECODE).append(" varchar,\n")
                .append(MethodNode.METHODLOCATION).append(" varchar,\n")
                .append(InvocationNode.SNIPPET).append(" varchar,\n")
                .append(InvocationNode.INVOCATIONLOCATION).append(" varchar,\n")
                .append(RuleNode.CATEGORY).append(" varchar,\n")
                .append(RuleNode.KIND).append(" varchar,\n")
                .append(RuleNode.RULE).append(" varchar,\n")
                .append(RuleNode.ISANNOTATION).append(" integer,\n")
                .append(RuleNode.ISCONSTRUCTOR).append(" integer,\n")
                .append(RuleNode.ISMETHOD).append(" integer,\n")
                .append(RuleNode.ISWEBANNOTATIONSOURCE).append(" integer,\n")
                .append(RuleNode.ISNATIVEGADGETSOURCE).append(" integer,\n")
                .append(RuleNode.ISJSONGADGETSOURCE).append(" integer,\n")
                .append(RuleNode.ISWEBINVOCATIONSOURCE).append(" integer,\n")
                .append(RuleNode.ISINVOCATIONSINK).append(" integer,\n")
                .append(RuleNode.ISANNOTATIONSINK).append(" integer,\n")
                .append(RuleNode.ISSOURCEPROPAGATOR).append(" integer,\n")
                .append(RuleNode.ISSINKPROPAGATOR).append(" integer,\n")
                .append("UNIQUE (").append(InvocationNode.INVOCATIONLOCATION).append("));").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int ImportInvocationNode(InvocationNode invocationNode) {
        String sql = new StringBuilder().append("INSERT or IGNORE INTO ").append(invocationsTable).append(" (")
                .append(ClassNode.NAMESPACE).append(", ")
                .append(ClassNode.CLASSTYPE).append(", ")
                .append(MethodNode.METHOD).append(", ")
                .append(MethodNode.RETURNTYPE).append(", ")
                .append(MethodNode.SIGNATURE).append(", ")
                .append(MethodNode.SOURCECODE).append(", ")
                .append(MethodNode.METHODLOCATION).append(", ")
                .append(InvocationNode.SNIPPET).append(", ")
                .append(InvocationNode.INVOCATIONLOCATION).append(", ")
                .append(RuleNode.CATEGORY).append(", ")
                .append(RuleNode.KIND).append(", ")
                .append(RuleNode.RULE).append(", ")
                .append(RuleNode.ISANNOTATION).append(", ")
                .append(RuleNode.ISCONSTRUCTOR).append(", ")
                .append(RuleNode.ISMETHOD).append(", ")
                .append(RuleNode.ISWEBANNOTATIONSOURCE).append(", ")
                .append(RuleNode.ISNATIVEGADGETSOURCE).append(", ")
                .append(RuleNode.ISJSONGADGETSOURCE).append(", ")
                .append(RuleNode.ISWEBINVOCATIONSOURCE).append(", ")
                .append(RuleNode.ISINVOCATIONSINK).append(", ")
                .append(RuleNode.ISANNOTATIONSINK).append(", ")
                .append(RuleNode.ISSOURCEPROPAGATOR).append(", ")
                .append(RuleNode.ISSINKPROPAGATOR).append(") ")
                .append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").toString();

        SourceNode sourceNode = invocationNode.getSourceNode();
        MethodNode methodNode = sourceNode.getMethodNode();
        SimpleMethodNode simpleMethodNode = methodNode.getSimpleMethodNode();
        ClassNode classNode = simpleMethodNode.getFullClasstype();
        RuleNode ruleNode = sourceNode.getRuleNode();

        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, classNode.getNamespace());
            statement.setString(2, classNode.getName());
            statement.setString(3, simpleMethodNode.getName());
            statement.setString(4, methodNode.getReturntype());
            statement.setString(5, methodNode.getSignature());
            statement.setString(6, methodNode.getSourceCode());
            statement.setString(7, Object2Json(methodNode.getMethodLocation()));
            statement.setString(8, invocationNode.getSnippet());
            statement.setString(9, Object2Json(invocationNode.getInvocationLocation()));
            statement.setString(10, ruleNode.getCategory());
            statement.setString(11, ruleNode.getKind());
            statement.setString(12, ruleNode.getRule());
            statement.setInt(13, Boolean2Integer(ruleNode.isAnnotationFlag()));
            statement.setInt(14, Boolean2Integer(ruleNode.isConstructorFlag()));
            statement.setInt(15, Boolean2Integer(ruleNode.isMethodFlag()));
            statement.setInt(16, Boolean2Integer(ruleNode.isWebAnnotationSource()));
            statement.setInt(17, Boolean2Integer(ruleNode.isNativeGadgetSource()));
            statement.setInt(18, Boolean2Integer(ruleNode.isJsonGadgetSource()));
            statement.setInt(19, Boolean2Integer(ruleNode.isWebInvocationSource()));
            statement.setInt(20, Boolean2Integer(ruleNode.isInvocationSink()));
            statement.setInt(21, Boolean2Integer(ruleNode.isAnnotationSink()));
            statement.setInt(22, Boolean2Integer(ruleNode.isSourcePropagator()));
            statement.setInt(23, Boolean2Integer(ruleNode.isSinkPropagator()));

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return QueryExistingInvocationNodeID(invocationNode);
    }

    private static int QueryExistingInvocationNodeID(InvocationNode invocationNode) {
        int id = -1;
        try {
            String sql = new StringBuilder().append("SELECT ").append(CallGraphNode.INVOCATIONID).append(" FROM ").append(invocationsTable).append(" WHERE ").append(InvocationNode.INVOCATIONLOCATION).append(" = ?").toString();
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, Object2Json(invocationNode.getInvocationLocation()));

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

    private static List<SourceNode> QuerySourceNode(ResultSet resultSet) {
        List<SourceNode> sourceNodes = new ArrayList<>();

        try {
            while(resultSet.next()) {
                SourceNode sourceNode = QuerySourceNode0(resultSet);
                sourceNodes.add(sourceNode);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sourceNodes;
    }

    private static List<InvocationNode> QueryInvocationNode(ResultSet resultSet) {
        List<InvocationNode> invocationNodes = new ArrayList<>();

        try {
            while(resultSet.next()) {
                SourceNode sourceNode = QuerySourceNode0(resultSet);
                
                InvocationNode invocationNode = new InvocationNode();
                invocationNode.setSourceNode(sourceNode);
                invocationNode.setInvocationID(resultSet.getInt(CallGraphNode.INVOCATIONID));
                invocationNode.setInvocationLocation(
                        (Location) Json2Object(resultSet.getString(InvocationNode.INVOCATIONLOCATION), Location.class));
                invocationNode.setSnippet(resultSet.getString(InvocationNode.SNIPPET));
                invocationNodes.add(invocationNode);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invocationNodes;
    }
    
    private static SourceNode QuerySourceNode0(ResultSet resultSet) {
        SourceNode sourceNode = new SourceNode();
        try {
            ClassNode classNode = new ClassNode();
            classNode.setNamespace(resultSet.getString(ClassNode.NAMESPACE));
            classNode.setName(resultSet.getString(ClassNode.CLASSTYPE));

            SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
            simpleMethodNode.setFullClasstype(classNode);
            simpleMethodNode.setName(resultSet.getString(MethodNode.METHOD));

            MethodNode methodNode = new MethodNode();
            methodNode.setSimpleMethodNode(simpleMethodNode);
            methodNode.setReturntype(resultSet.getString(MethodNode.RETURNTYPE));
            methodNode.setSignature(resultSet.getString(MethodNode.SIGNATURE));
            methodNode.setSourceCode(resultSet.getString(MethodNode.SOURCECODE));
            methodNode.setMethodLocation(
                    (Location) CharUtils.Json2Object(resultSet.getString(MethodNode.METHODLOCATION), Location.class));

            RuleNode ruleNode = new RuleNode();
            ruleNode.setCategory(resultSet.getString(RuleNode.CATEGORY));
            ruleNode.setKind(resultSet.getString(RuleNode.KIND));
            ruleNode.setRule(resultSet.getString(RuleNode.RULE));
            ruleNode.setAnnotationFlag(Integer2Boolean(resultSet.getInt(RuleNode.ISANNOTATION)));
            ruleNode.setConstructorFlag(Integer2Boolean(resultSet.getInt(RuleNode.ISCONSTRUCTOR)));
            ruleNode.setMethodFlag(Integer2Boolean(resultSet.getInt(RuleNode.ISMETHOD)));
            ruleNode.setWebAnnotationSource(Integer2Boolean(resultSet.getInt(RuleNode.ISWEBANNOTATIONSOURCE)));
            ruleNode.setNativeGadgetSource(Integer2Boolean(resultSet.getInt(RuleNode.ISNATIVEGADGETSOURCE)));
            ruleNode.setJsonGadgetSource(Integer2Boolean(resultSet.getInt(RuleNode.ISJSONGADGETSOURCE)));
            ruleNode.setWebInvocationSource(Integer2Boolean(resultSet.getInt(RuleNode.ISWEBINVOCATIONSOURCE)));
            ruleNode.setInvocationSink(Integer2Boolean(resultSet.getInt(RuleNode.ISINVOCATIONSINK)));
            ruleNode.setAnnotationSink(Integer2Boolean(resultSet.getInt(RuleNode.ISANNOTATIONSINK)));
            ruleNode.setSourcePropagator(Integer2Boolean(resultSet.getInt(RuleNode.ISSOURCEPROPAGATOR)));
            ruleNode.setSinkPropagator(Integer2Boolean(resultSet.getInt(RuleNode.ISSINKPROPAGATOR)));

            sourceNode.setMethodNode(methodNode);
            sourceNode.setRuleNode(ruleNode);   
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sourceNode;
    }

    private static void CreateSourceNodeCallgraphsTable() {
        String sql = new StringBuilder().append("CREATE TABLE ").append(sourcenodeCGTable).append(" (\n")
                .append(CallGraphNode.METHODID).append(" integer,\n")
                .append(CallGraphNode.INVOCATIONID).append(" integer,\n")
                .append(CallGraphNode.INTRAFLOW).append(" varchar );").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ImportSourceNodeCallgraphNode(CallGraphNode callGraphNode) {
        String sql = new StringBuilder("INSERT or IGNORE INTO ").append(sourcenodeCGTable).append(" (")
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

    private static void CreateInvocationCallgraphsTable() {
        String sql = new StringBuilder().append("CREATE TABLE ").append(invocationCGTable).append(" (\n")
                .append(CallGraphNode.INVOCATIONSOURCEID).append(" integer,\n")
                .append(CallGraphNode.INVOCATIONTARGETID).append(" integer,\n")
                .append(CallGraphNode.INTRAFLOW).append(" varchar );").toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ImportInvocationCallgraphNode(CallGraphNode callGraphNode) {
        String sql = new StringBuilder("INSERT or IGNORE INTO ").append(invocationCGTable).append(" (")
                .append(CallGraphNode.INVOCATIONSOURCEID).append(", ")
                .append(CallGraphNode.INVOCATIONTARGETID).append(", ")
                .append(CallGraphNode.INTRAFLOW).append(") VALUES (?, ?, ?)").toString();

        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, callGraphNode.getInvocationSourceID());
            statement.setInt(2, callGraphNode.getInvocationTargetID());
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
        return ExtractInvocationNodeFromRuleNode(ruleNode);
    }

    public static InvocationNode QueryInvocationConstructorNode(String namespace, String classtype) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE "+ ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(MethodNode.METHOD).append(" = '<init>'").toString();

        RuleNode ruleNode = QueryRuleNode(sql, namespace, classtype, "<init>");
        ruleNode.setNativeGadgetSource(false);
        ruleNode.setJsonGadgetSource(false);
        return ExtractInvocationNodeFromRuleNode(ruleNode);
    }

    public static InvocationNode QueryInvocationMethodNode(CtExecutableReference<?> executableReference) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE ").append(ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(RuleNode.CATEGORY).append(" != ").append("\"").append(RuleNode.GADGET).append("\"").toString();

        CtTypeReference<?> executableInvocation = executableReference.getDeclaringType();
        HashSet<String> methodSet = MethodHierarchy.getHierachySet(
                executableInvocation.getTypeDeclaration(),
                executableReference.getSimpleName(),
                executableReference.getParameters());

        RuleNode ruleNode = new RuleNode();
        for (String qualifiedName : methodSet) {
            String namespace = FilenameUtils.getBaseName(qualifiedName);
            String classtype = FilenameUtils.getExtension(qualifiedName);
            ruleNode = QueryRuleNode(sql, namespace, classtype, executableReference.getSimpleName());
            if (ruleNode.isInvocationSink() || ruleNode.isWebInvocationSource()) {
                break;
            }
        }

        ruleNode.getSimpleMethodNode().getFullClasstype().setNamespace(executableInvocation.getTopLevelType().getPackage().getQualifiedName());
        ruleNode.getSimpleMethodNode().getFullClasstype().setName(executableInvocation.getTypeDeclaration().getSimpleName());

        ruleNode.setNativeGadgetSource(false);
        ruleNode.setJsonGadgetSource(false);
        return ExtractInvocationNodeFromRuleNode(ruleNode);
    }

    private static InvocationNode ExtractInvocationNodeFromRuleNode(RuleNode ruleNode) {
        SourceNode sourceNode = new SourceNode();
        sourceNode.setRuleNode(ruleNode);
        MethodNode methodNode = new MethodNode();
        methodNode.setSimpleMethodNode(ruleNode.getSimpleMethodNode());
        sourceNode.setMethodNode(methodNode);
        InvocationNode invocationNode = new InvocationNode();
        invocationNode.setSourceNode(sourceNode);
        return invocationNode;
    }

    private static RuleNode QueryNativeGadgetSourceMethodNode(String namespace, String classtype, String methodname) {
        String sql = new StringBuilder().append("SELECT * FROM ").append(rulesTable).append(" WHERE ").append(ClassNode.NAMESPACE).append(" = ? AND ").append(ClassNode.CLASSTYPE).append(" = ? ").append("AND ").append(RuleNode.CATEGORY).append(" = ").append("\"").append(RuleNode.GADGET).append("\"").toString();

        return QueryRuleNode(sql, namespace, classtype, methodname);
    }

    public static RuleNode QueryNativeGadgetSourceMethodNode(CtExecutableReference<?> ctExecutableReference) {
        ClassNode classNode = new ClassNode();
        CtTypeReference<?> executableInvocation = ctExecutableReference.getDeclaringType();
        classNode.setNamespace(executableInvocation.getTopLevelType().getPackage().getQualifiedName());
        classNode.setName(executableInvocation.getTypeDeclaration().getSimpleName());
        SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
        simpleMethodNode.setFullClasstype(classNode);
        simpleMethodNode.setName(ctExecutableReference.getSimpleName());

        RuleNode ruleNode = new RuleNode();
        HashSet<String> methodSet = MethodHierarchy.getHierachySet(
                ctExecutableReference.getDeclaringType().getDeclaration(),
                ctExecutableReference.getSimpleName(),
                ctExecutableReference.getParameters());

        for (String qualifiedName : methodSet) {
            String namespace = FilenameUtils.getBaseName(qualifiedName);
            String classtype = FilenameUtils.getExtension(qualifiedName);
            ruleNode = QueryNativeGadgetSourceMethodNode(namespace, classtype,
                    ctExecutableReference.getSimpleName());
            if (ruleNode.isNativeGadgetSource()) {
                break;
            }
        }

        CtType<?> clazz = executableInvocation.getDeclaration();
        ruleNode.setNativeGadgetSource(
                ruleNode.isNativeGadgetSource() && (MethodHierarchy.isSerializable(clazz) || MethodHierarchy.isExternalizable(clazz)));

        ruleNode.setSimpleMethodNode(simpleMethodNode);
        return ruleNode;
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
            SimpleMethodNode simpleMethodNode = new SimpleMethodNode();
            simpleMethodNode.setFullClasstype(classNode);
            simpleMethodNode.setName(methodname);
            ruleNode.setSimpleMethodNode(simpleMethodNode);

            if (methodname == null) {
                ruleNode.setAnnotationFlagTrue();
            } else if (methodname.equals(classtype) || methodname.equals("<init>")) {
                ruleNode.setConstructorFlagTrue();
            } else {
                ruleNode.setMedthodFlagTrue();
            }

            while(resultSet.next()) {
                String method = resultSet.getString(MethodNode.METHOD);
                if (method == null ||
                        method.equals(methodname) ||
                        (method.startsWith(CharUtils.leftbracket) && CharUtils.RegexMatch(method, methodname))) {
                    ruleNode.setKind(resultSet.getString(RuleNode.KIND));
                    ruleNode.setRule(resultSet.getString(RuleNode.RULE));
                    String category = resultSet.getString(RuleNode.CATEGORY);
                    ruleNode.setCategory(category);

                    switch (category) {
                        case RuleNode.SOURCE:
                            if (resultSet.getString(RuleNode.KIND).contains("annotation") || methodname == null) {
                                ruleNode.setWebAnnotationSource(true);
                            } else {
                                ruleNode.setWebInvocationSource(true);
                            }
                            break;
                        case RuleNode.SINK:
                            ruleNode.setInvocationSink(true);
                            break;
                        case RuleNode.GADGET:
                            ruleNode.setNativeGadgetSource(true);
                            break;
                    }

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
                        methodname.equals(ruleNode.getSimpleMethodNode().getFullClasstype().getName()))) {
            ruleNode.setJsonGadgetSource(true);
        }

        return ruleNode;
    }
//
    public static List<InvocationNode> QueryExistingWebInvocationSourceInvocationNode() {
        String sql = new StringBuilder().append("SELECT * FROM ").append(invocationsTable).append(" WHERE ").append(invocationsTable).append(".").append(RuleNode.ISWEBINVOCATIONSOURCE).append(" = 1").toString();
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
    // at last store the data flow to sourcenodeCG table
    /*
     * SELECT * FROM invocations WHERE invocations.InvocationID in (SELECT sourcenodeCG.InvocationID FROM sourcenodeCG WHERE sourcenodeCG.MethodID in (SELECT sourcenodeCG.MethodID FROM sourcenodeCG WHERE sourcenodeCG.InvocationID = ?));
     */
    public static void ImportWebInvocationSourceFlow() {
        List<InvocationNode> webInvocationSourceInvocationNodes = QueryExistingWebInvocationSourceInvocationNode();
        for (InvocationNode webInvocationSourceInvocationNode : webInvocationSourceInvocationNodes) {
            String sql = "SELECT * FROM invocations WHERE invocations.InvocationID in (SELECT sourcenodeCG.InvocationID FROM sourcenodeCG WHERE sourcenodeCG.MethodID in (SELECT sourcenodeCG.MethodID FROM sourcenodeCG WHERE sourcenodeCG.InvocationID = ?))";

            PreparedStatement statement;
            try {
                statement = conn.prepareStatement(sql);
                statement.setInt(1, webInvocationSourceInvocationNode.getInvocationID());

                List<InvocationNode> invocationsNodesInSameMethod = QueryInvocationNode(statement.executeQuery());
                for (InvocationNode invocationsNodeInSameMethod : invocationsNodesInSameMethod) {
                    ThreadFlow intraflow = SemgrepUtils.DetectIntraFlow(webInvocationSourceInvocationNode, invocationsNodeInSameMethod);

                    CallGraphNode callGraphNode = new CallGraphNode();
                    callGraphNode.setInvocationSourceID(webInvocationSourceInvocationNode.getInvocationID());
                    callGraphNode.setInvocationTargetID(invocationsNodeInSameMethod.getInvocationID());
                    callGraphNode.setIntraflow(intraflow);

                    DbUtils.ImportInvocationCallgraphNode(callGraphNode);
                }
                statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static SourceNode QueryMethodNodeFromWebSourceInvocation(int InvocationID) {
        String sql = "SELECT * FROM sourcenodes WHERE sourcenodes.MethodID in (SELECT sourcenodeCG.MethodID FROM sourcenodeCG WHERE sourcenodeCG.InvocationID = ?)";
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, InvocationID);

            List<SourceNode> ParentSourceNode = QuerySourceNode(statement.executeQuery());
            statement.close();
            return ParentSourceNode.get(0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<InvocationNode> QuerySinkNodes() {
        List<InvocationNode> invocationNodes = new ArrayList<>();

        String sql = "SELECT * FROM invocations WHERE isInvocationSink = 1 OR (isAnnotationSink = 1 AND isSinkPropagator = 0)";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            invocationNodes = QueryInvocationNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return invocationNodes;
    }

    public static List<InvocationNode> QueryWebSourceNodes() {
        List<InvocationNode> invocationNodes = new ArrayList<>();

        String sql = "SELECT * FROM invocations WHERE isWebInvocationSource = 1 OR (isWebAnnotationSource = 1 AND isSourcePropagator = 0)";
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
        // todo missing annotationSink gadget
        String sql = "SELECT * FROM sourcenodeCG WHERE sourcenodeCG.intraflow IS NOT NULL AND sourcenodeCG.InvocationID in (SELECT invocations.InvocationID FROM invocations WHERE invocations.isInvocationSink = 1)";

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
            rule = QueryInvocationNode(resultSet).get(0).getSourceNode().getRuleNode().getRule();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rule;
    }


    // Update methods with source annotation as annotation source method
    public static void UpdateParentMethodAsWebAnnoationSource(int methodID, RuleNode ruleNode, String SourceType) {
        String sql = new StringBuilder().append("UPDATE ").append(sourcenodeTable).append(" SET ").append(SourceType).append(" = 1, ").append(RuleNode.CATEGORY).append(" = ?, ").append(RuleNode.KIND).append(" = ?, ").append(RuleNode.RULE).append(" = ? WHERE ").append(CallGraphNode.METHODID).append(" = ?").toString();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ruleNode.getCategory());
            stmt.setString(2, ruleNode.getKind());
            stmt.setString(3, ruleNode.getRule());
            stmt.setInt(4, methodID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Update all sources to propagator as well
    /*
     * UPDATE invocations SET isSourcePropagator = 1 WHERE isWebAnnotationSource = 1 OR isWebInvocationSource = 1 OR isNativeGadgetSource = 1 OR isJsonGadgetSource = 1
     */
    public static int UpdateSources2SourcePropagator() {
        String sql = "UPDATE invocations SET isSourcePropagator = 1 WHERE isWebInvocationSource = 1;" +
        // update sourceinvocation to sourcepropagator
        "UPDATE invocations SET isSourcePropagator = 1 WHERE invocations.isSourcePropagator != 1 AND invocations.InvocationID in (SELECT invocationCG.invocationTargetID FROM invocationCG WHERE invocationCG.intraflow IS NOT NULL AND invocationCG.InvocationSourceID in (SELECT invocations.InvocationID FROM invocations WHERE invocations.isWebInvocationSource = 1))";

        int count = 0;
        try (Statement stmt = conn.createStatement()) {
            count = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        count += QueryOtherSourceNode();
        return count;
    }

    private static int QueryOtherSourceNode() {
        String sql = "SELECT * FROM invocations WHERE isWebAnnotationSource = 1 OR isNativeGadgetSource = 1 OR isJsonGadgetSource = 1";
        List<InvocationNode> convergences = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            convergences = QueryInvocationNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return convergences.size();
    }

    /*
     * UPDATE invocations SET isSinkPropagator = 1 WHERE isSinkInvocation = 1
     */
    public static int UpdateSinks2SinkPropagator() {
        String sql = "UPDATE invocations SET isSinkPropagator = 1 WHERE isInvocationSink = 1;" +
        // update sinkannotation to sinkpropagator
        "UPDATE invocations SET isSinkPropagator = 1 WHERE invocations.isSinkPropagator != 1 AND (invocations.returntype, invocations.signature, invocations.sourcecode, invocations.methodlocation) in (SELECT sourcenodes.returntype, sourcenodes.signature, sourcenodes.sourcecode, sourcenodes.methodlocation FROM sourcenodes WHERE sourcenodes.isAnnotationSink = 1)";


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
        String sql = "SELECT sourcenodeCG.InvocationID FROM sourcenodeCG WHERE sourcenodeCG.intraflow IS NOT NULL AND sourcenodeCG.MethodID in (SELECT sourcenodes.MethodID FROM sourcenodes WHERE (sourcenodes.returntype, sourcenodes.signature, sourcenodes.sourcecode, sourcenodes.methodlocation) in (SELECT invocations.returntype, invocations.signature, invocations.sourcecode, invocations.methodlocation FROM invocations WHERE invocations.isSourcePropagator = 1 AND invocations.isSinkPropagator != 1))";

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
        String sql =
        // update sinkinvocation to sinkpropagator
        "UPDATE invocations SET isSinkPropagator = 1 WHERE invocations.isSinkPropagator != 1 AND (invocations.returntype, invocations.signature, invocations.sourcecode, invocations.methodlocation) in (SELECT sourcenodes.returntype, sourcenodes.signature, sourcenodes.sourcecode, sourcenodes.methodlocation FROM sourcenodes WHERE sourcenodes.MethodID in (SELECT sourcenodeCG.MethodID FROM sourcenodeCG WHERE sourcenodeCG.intraflow IS NOT NULL AND sourcenodeCG.InvocationID in (SELECT invocations.InvocationID FROM invocations WHERE invocations.isSinkPropagator = 1 AND invocations.isSourcePropagator != 1)))";

        int updateRows = 0;
        try (Statement stmt = conn.createStatement()) {
            updateRows = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updateRows;
    }

    public static List<InvocationNode> QueryPropagatorConvergence() {
        String sql = "SELECT * FROM invocations WHERE isSourcePropagator = 1 AND isSinkPropagator = 1";

        List<InvocationNode> convergences = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            convergences = QueryInvocationNode(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return convergences;
    }

    private static SourceNode QueryParentMethodOfSourcePropagator(int InvocationID) {
        String sql = "SELECT * FROM sourcenodes WHERE sourcenodes.MethodID in (SELECT sourcenodeCG.MethodID FROM sourcenodeCG WHERE sourcenodeCG.intraflow IS NOT NULL AND sourcenodeCG.InvocationID = ?)";
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, InvocationID);

            List<SourceNode> ParentSourceNode = QuerySourceNode(statement.executeQuery());
            statement.close();
            return ParentSourceNode.get(0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<InvocationNode> QueryParentInvocationOfSourcePropagator(int InvocationID) {
        String sql = "SELECT * FROM invocations WHERE invocations.InvocationID in (SELECT invocationCG.InvocationSourceID FROM invocationCG WHERE invocationCG.intraflow IS NOT NULL AND invocationCG.InvocationTargetID = ?)";
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, InvocationID);

            List<InvocationNode> ParentSourceNode = QueryInvocationNode(statement.executeQuery());
            statement.close();
            return ParentSourceNode;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void QueryPredNodes(InvocationNode invocationNode, DefaultMutableTreeNode treeNode, Set<Integer> unique) {
        RuleNode ruleNode = invocationNode.getSourceNode().getRuleNode();
        if (ruleNode.isWebAnnotationSource() ||
                ruleNode.isNativeGadgetSource() ||
                ruleNode.isJsonGadgetSource()) {
            SourceNode realSourceNode = QueryParentMethodOfSourcePropagator(invocationNode.getInvocationID());
            DefaultMutableTreeNode parentNode = new DefaultMutableTreeNode(realSourceNode.getMethodNode().getMethodLocation());
            String text = "Source: '" + realSourceNode.getMethodNode().getSignature() + "'";
            ((Location) parentNode.getUserObject()).setMessage(new Message().withText(text));
            ((Location) parentNode.getUserObject()).getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
            ((Location) parentNode.getUserObject()).getPhysicalLocation().getArtifactLocation().setDescription(
                    new Message().withText(getSourceType(realSourceNode.getRuleNode())));

            setPropagatorLabel(invocationNode, treeNode);
            treeNode.add(parentNode);
        } else {
            setPropagatorLabel(invocationNode, treeNode);
        }

        String sql = "SELECT * FROM invocations WHERE invocations.isSourcePropagator = 1 AND (invocations.returntype, invocations.signature, invocations.sourcecode, invocations.methodlocation) in (SELECT sourcenodes.returntype, sourcenodes.signature, sourcenodes.sourcecode, sourcenodes.methodlocation FROM sourcenodes WHERE sourcenodes.MethodID in (SELECT sourcenodeCG.MethodID FROM sourcenodeCG WHERE sourcenodeCG.intraflow IS NOT NULL AND sourcenodeCG.InvocationID = ?))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invocationNode.getInvocationID());
            ResultSet rs = stmt.executeQuery();
            List<InvocationNode> predNodes = QueryInvocationNode(rs);

            if (predNodes.size() == 0) {
                // Check web invocation prednode
                predNodes = QueryParentInvocationOfSourcePropagator(invocationNode.getInvocationID());
            }

            for (InvocationNode predNode: predNodes) {
                DefaultMutableTreeNode predTreeNode = new DefaultMutableTreeNode(predNode.getInvocationLocation());
                if (unique.add(predNode.getInvocationID())) {
                    if (predNode.getSourceNode().getRuleNode().isWebInvocationSource()) {
                        String text = "Source: '" + predNode.getSnippet() + "'";
                        ((Location) predTreeNode.getUserObject()).setMessage(new Message().withText(text));
                        ((Location) predTreeNode.getUserObject()).getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
                        ((Location) predTreeNode.getUserObject()).getPhysicalLocation().getArtifactLocation().setDescription(
                                new Message().withText(getSourceType(predNode.getSourceNode().getRuleNode())));
                        treeNode.add(predTreeNode);
                    } else {
                        treeNode.add(predTreeNode);
                        QueryPredNodes(predNode, predTreeNode, unique);
                    }
                    unique.remove(predNode.getInvocationID());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static String getSourceType(RuleNode ruleNode) {
        if (ruleNode.isWebAnnotationSource() || ruleNode.isWebInvocationSource()) {
            return "Web";
        } else if (ruleNode.isNativeGadgetSource()) {
            return "Native Gadget";
        } else if (ruleNode.isJsonGadgetSource()) {
            return "Json Gadget";
        } else {
            return null;
        }
    }

    @Nullable
    private static boolean isSinkType(RuleNode ruleNode) {
        if (ruleNode.isInvocationSink() || ruleNode.isAnnotationSink()) {
            return true;
        }
        return false;
    }

    public static void QuerySuccNodes(InvocationNode invocationNode, DefaultMutableTreeNode treeNode, Set<Integer> unique) {
        // TODO BUG When found sink node, should continue to propagate till non-succ node is found
        DefaultMutableTreeNode treeNodePropagatorClone = (DefaultMutableTreeNode) treeNode.clone();
        RuleNode ruleNode = invocationNode.getSourceNode().getRuleNode();
        if (ruleNode.isInvocationSink()) {
            String text = "Sink: '" + invocationNode.getSnippet() + "'";
            ((Location) treeNode.getUserObject()).setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
            ((Location) treeNode.getUserObject()).getPhysicalLocation().getArtifactLocation().setDescription(
                    new Message().withText(ruleNode.getRule()));
        } else if (ruleNode.isAnnotationSink()) {
            // todo
        } else {
            setPropagatorLabel(invocationNode, treeNode);
        }

        String sql = "SELECT * FROM invocations WHERE invocations.isSinkPropagator = 1 AND invocations.InvocationID in (SELECT sourcenodeCG.InvocationID FROM sourcenodeCG WHERE sourcenodeCG.intraflow IS NOT NULL AND sourcenodeCG.MethodID in (SELECT sourcenodes.MethodID FROM sourcenodes WHERE (sourcenodes.returntype, sourcenodes.signature, sourcenodes.sourcecode, sourcenodes.methodlocation) in (SELECT invocations.returntype, invocations.signature, invocations.sourcecode, invocations.methodlocation FROM invocations WHERE invocations.InvocationID = ?)))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invocationNode.getInvocationID());
            ResultSet rs = stmt.executeQuery();
            for (InvocationNode succNode: QueryInvocationNode(rs)) {
                DefaultMutableTreeNode succTreeNode = new DefaultMutableTreeNode(succNode.getInvocationLocation());
                if (unique.add(succNode.getInvocationID())) {
                    if (isSinkType(ruleNode) && treeNode.getParent() != null) {
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();
                        setPropagatorLabel(invocationNode, treeNodePropagatorClone);
                        parentNode.add(treeNodePropagatorClone);
                        treeNodePropagatorClone.add(succTreeNode);
                    } else {
                        treeNode.add(succTreeNode);
                    }
                    QuerySuccNodes(succNode, succTreeNode, unique);
                    unique.remove(succNode.getInvocationID());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void setPropagatorLabel(InvocationNode invocationNode, DefaultMutableTreeNode treeNode) {
        Location location = (Location) treeNode.getUserObject();
        String text = "Propagator: '" + invocationNode.getSnippet() + "'";
        location.setMessage(new Message().withText(text));
        location.getPhysicalLocation().getRegion().setMessage(new Message().withText(text));
    }
}
