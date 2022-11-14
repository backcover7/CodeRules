package csv.resolver.taint;

import config.DbConfig;
import utils.narytree.Tree;
import utils.narytree.Node;
import utils.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class SeekVariants {
    private static HashMap<String, String> OriginalCSVMap;
    private final static String FULLCLASSTYPE = "FullClassType";
    private final static String RETURNTYPE = "ReturnType";

    private ArrayList<ArrayList<Integer>> QueryIdFromReturnType(String returnType) throws Exception {
        String sql = "SELECT class_id, method_id FROM " + DbUtils.MethodTablename + " WHERE return_type = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setString(1, returnType);
        ResultSet resultSet = statement.executeQuery();

        ArrayList<ArrayList<Integer>> ids = new ArrayList<>();
        while(resultSet.next()){
            ArrayList<Integer> id = new ArrayList<>();
            id.add(resultSet.getInt(TableUtils.CLASSID));
            id.add(resultSet.getInt(TableUtils.METHODID));
            ids.add(id);
        }
        statement.close();

        return ids;
    }

    private boolean IsJDKClassId(String returnType) throws Exception {

        int lastIndexOfDot = returnType.lastIndexOf(StringsUtils.dot);
        String namespace = returnType.substring(0, lastIndexOfDot);
        String classtype = returnType.substring(lastIndexOfDot + 1);

        String sql = "SELECT category FROM " + DbUtils.ClassTablename + " WHERE namespace = ? and classtype = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setString(1, namespace);
        statement.setString(2, classtype);
        ResultSet resultSet = statement.executeQuery();

        String JDKClassFlag = resultSet.getString(TableUtils.Category);
        statement.close();

        if (JDKClassFlag.equals(SemCSV.JDKPackage)) {
            return true;
        } else {
            return false;
        }
    }

    private int QueryUniqueClassId(String namespace, String classtype) throws Exception {
        String sql = "SELECT class_id FROM " + DbUtils.ClassTablename + " WHERE namespace = ? and classtype = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setString(1, namespace);
        statement.setString(2, classtype);
        ResultSet resultSet = statement.executeQuery();

        int classId = resultSet.getInt(TableUtils.CLASSID);
        statement.close();

        return classId;
    }

    private ArrayList<Integer> QueryMethodIdsFromNameAndClass(int ClassId, String methodname) throws Exception {
        String sql = "SELECT method_id FROM " + DbUtils.MethodTablename +
                " WHERE class_id = ? AND methodname = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setInt(1, ClassId);
        statement.setString(2, methodname);

        ResultSet resultSet = statement.executeQuery();

        ArrayList<Integer> methodIds = new ArrayList<>();
        while(resultSet.next()) {
            methodIds.add(resultSet.getInt(TableUtils.METHODID));
        }

        return methodIds;
    }

    private String QueryAllFromClasses(int class_id) throws Exception {
        String sql = "SELECT namespace||':'||classtype||':'||type_modifer||':'||superclass||':'||interface" +
                " FROM " + DbUtils.ClassTablename + " WHERE class_id = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setInt(1, class_id);
        ResultSet resultSet = statement.executeQuery();

        String allClassInfo = resultSet.getString(1);

        statement.close();
        return allClassInfo;
    }

    private String QueryAllFromMethods(int method_id) throws Exception {
        String sql = "SELECT methodname||':'||method_modifier||':'||argument_size||':'||argument_type||':'||return_type||':'||kind||':'||category " +
                "FROM " + DbUtils.MethodTablename + " WHERE method_id = ?" +
                "and method_modifier like \"%public%\"";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setInt(1, method_id);
        ResultSet resultSet = statement.executeQuery();

        String allMethodInfo = resultSet.getString(1);

        statement.close();
        return allMethodInfo;
    }

    private String getFullClassType(String CustomCSV) {
        return String.join(
                StringsUtils.dot,
                new CustomCSV().getCSVMap(CustomCSV).get(SemCSV.Namespace),
                new CustomCSV().getCSVMap(CustomCSV).get(SemCSV.Type));
    }

    private String getRetrunType(String CustomCSV) {
        return new CustomCSV().getCSVMap(CustomCSV).get(CsvUtils.ReturnType);
    }

    private void addNode(HashMap<String, HashMap<String, HashSet<String>>> ChainNodes, String CustomCSV) {
        /**
         *
         * ChainNodes Structure:
         *   {
         *      FullClassType :
         *          {
         *              "javax.script.ScriptEngine" : [CSV1, CSV2, ... ],
         *              "apple.applescript.AppleScriptEngineFactory" : [CSV3, CSV4, ... ]
         *          }
         *     ReturnTypeIndex :
         *          {
         *              "javax.script.CompiledScript" : [CSV1, CSV3, ... ],
         *              "apple.applescript.AppleScriptEngine" : [CSV5, CSV7, ... ]
         *          }
         *   }
         */
        String FullClassType = getFullClassType(CustomCSV);
        String ReturnType = getRetrunType(CustomCSV);

        HashSet<String> FullClassTypeCsvSet = new HashSet<>();
        FullClassTypeCsvSet.add(CustomCSV);

        if (ChainNodes.get(FULLCLASSTYPE).containsKey(FullClassType)) {
            ChainNodes.get(FULLCLASSTYPE).get(FullClassType).add(CustomCSV);
        } else {
            ChainNodes.get(FULLCLASSTYPE).put(FullClassType, FullClassTypeCsvSet);
        }

        HashSet<String> ReturnTypeCsvSet = new HashSet<>();
        ReturnTypeCsvSet.add(CustomCSV);

        if (ChainNodes.get(RETURNTYPE).containsKey(ReturnType)) {
            ChainNodes.get(RETURNTYPE).get(ReturnType).add(CustomCSV);
        } else {
            ChainNodes.get(RETURNTYPE).put(ReturnType, ReturnTypeCsvSet);
        }
    }

    private void getCallChainNodes(
            String CustomCSV,
            HashMap<String, HashMap<String, HashSet<String>>> ChainNodes,
            HashSet<String> rets) throws Exception {

        // Compare classtype of a method and its
        String returnType = getFullClassType(CustomCSV);
        if (IsJDKClassId(returnType) !=
                OriginalCSVMap.get(SemCSV.Category).equals(SemCSV.JDKPackage)) {
            // If return type and the current csv all belong to 3pp package or JDK package at the same time.
            // Otherwise it should stop finding.
            return;
        } else if (IsJDKClassId(returnType)) {
            // TODO: If return type belongs to JDK. It should ignore if it is primitive type?
            String namespace = OriginalCSVMap.get(SemCSV.Namespace);
            if (!new CustomCSV().getCSVMap(CustomCSV).get(SemCSV.Namespace).equals(namespace)) {
                return;
            }
            else if (getCommonNamespaces().contains(namespace)) {
                return;
            }
        }

        ArrayList<ArrayList<Integer>> ids = QueryIdFromReturnType(returnType);
        for (ArrayList<Integer> id : ids) {
            String allClassInfo = QueryAllFromClasses(id.get(0));
            String allMethodInfo = QueryAllFromMethods(id.get(1));
            if (allMethodInfo != null) {
                String CSVline = String.join(StringsUtils.colon, allClassInfo, allMethodInfo);
                String PredReturnType = getFullClassType(CSVline);

                addNode(ChainNodes, CSVline);

                if (rets.contains(PredReturnType)) {
                    continue;
                } else {
                    rets.add(PredReturnType);
                }

                getCallChainNodes(CSVline, ChainNodes, rets);
            }
        }
    }

    private void getCallChainTree(
            String CustomCSV,
            HashMap<String, HashMap<String, HashSet<String>>> ChainNodes,
            Node<String> root,
            Set<String> Tree2LeafReturnType) {
        HashSet<String> PredNode = ChainNodes.get(RETURNTYPE).get(getFullClassType(CustomCSV));

        if (PredNode != null) {
            for (String node : PredNode) {
                Node<String> child = new Node<>(node);

                if (!Tree2LeafReturnType.contains(child.getData())) {
                    root.addChild(child);
                    Tree2LeafReturnType.add(child.getData());
                    getCallChainTree(node, ChainNodes, child, Tree2LeafReturnType);
                }
            }
        }
    }

    // Step1: Construct ChainNodes structure and put all found CSVs based on classtype and returntype
    private ArrayList<ArrayList<Node<String>>> getCallChainList(String CustomCSV) throws Exception {
        HashMap<String, HashMap<String, HashSet<String>>> ChainNodes = new HashMap<>();

        ChainNodes.put(FULLCLASSTYPE, new HashMap<>());
        ChainNodes.put(RETURNTYPE, new HashMap<>());

        addNode(ChainNodes, CustomCSV);
        // Find all related nodes
        getCallChainNodes(CustomCSV, ChainNodes, new HashSet<>());

        // Guarantee that there will be no duplicate items in a chain.
        Set<String> Tree2LeafReturnType = new HashSet<>();
        Tree2LeafReturnType.add(getFullClassType(CustomCSV));

        // Build tree from original csv to any leaf
        Node<String> root = new Node<>(CustomCSV);
        getCallChainTree(CustomCSV, ChainNodes, root, Tree2LeafReturnType);
        Tree<String> tree = new Tree<>(root);

        return tree.getPathsFromRootToAnyLeaf();
    }

    public ArrayList<String> getCustomCSV(String namespace, String classtype, String methodname) throws Exception {
        int classId = QueryUniqueClassId(namespace, classtype);
        ArrayList<Integer> methodIds = QueryMethodIdsFromNameAndClass(classId, methodname);
        ArrayList<String> methodCsvList = new ArrayList<>();
        for (int id : methodIds) {
            String allClassInfo = QueryAllFromClasses(classId);
            String allMethodInfo = QueryAllFromMethods(id);
            if (allMethodInfo != null) {
                methodCsvList.add(String.join(StringsUtils.colon, allClassInfo, allMethodInfo));
            }
        }
        return methodCsvList;
    }



    private String ExtractSuccMethodPattern(String pattern) {
        // (com.example.test $A).x( ... )
        // (com.example.test $A).y( ... ).x( ... )
        int rightbracketIndex = pattern.indexOf(StringsUtils.rightbracket);
        return pattern.substring(rightbracketIndex + 2);
    }

    public String PatternEitherWithVariants(String namespace, String classtype, String methodname, String kind) throws Exception {
        // Construct custom style csv from namespace, classtype, methodname, kind
        ArrayList<String> CustomCSVs = getCustomCSV(namespace, classtype, methodname);
        if (CustomCSVs.size() == 0) {
            return null;
        }

        // Construct CSVMap based on custom style csv.
        OriginalCSVMap = new CustomCSV().getCSVMap(CustomCSVs.get(0), kind);
        SemCSV semCSV = new SemCSV();

        // Find possible call chain
        // CustomCSVs.get(0) -> Do not care duplicate things with only different injected index
        ArrayList<ArrayList<Node<String>>> Chains = getCallChainList(CustomCSVs.get(0));
        if (Chains.size() == 1) {       // No chain found
            return null;
        }

        // Build patterns for yaml rule
        HashSet<String> patternChainSet = new HashSet<>();
        for (ArrayList<Node<String>> chain : Chains) {
            String pattern = StringsUtils.empty;
            for (Node<String> node : chain) {
                HashMap<String, String> CSVMap = new CustomCSV().getCSVMap(node.getData());

                String callType;
                if (CSVMap.get(SemCSV.Type).equals(CSVMap.get(SemCSV.Name)) || CSVMap.get(SemCSV.Name).equals("<init>")) {
                    callType = SemCSV.Constructor;
                } else if (CSVMap.get(CsvUtils.MethodModifiers).contains(SemCSV.STATIC)) {
                    callType = SemCSV.STATIC;
                } else {
                    callType = SemCSV.INSTANCE;
                }

                if (pattern.equals(StringsUtils.empty)) {
                    // Will only have one pattern because there's no Static/Instance call in assumption.
                    pattern = semCSV.AssemblePatternElem(CSVMap, callType).get(0);
                } else {
//                    pattern = String.join(
//                            StringsUtils.dot,
//                            semCSV.AssemblePatternElem(CSVMap, callType).get(0),
//                            ExtractSuccMethodPattern(pattern));
                    pattern = String.join(
                            StringsUtils.dot,
                            semCSV.AssemblePatternElem(CSVMap, callType).get(0),
                            " ... ",
                            methodname + "(...)");
                }
                patternChainSet.add(pattern);

                if (!callType.equals(SemCSV.INSTANCE)) {
                    // com.example.test.x( ... )     // Should not be in consideration
                    // new com.example.test( ... )   // Should not be in consideration
                    break;
                }
            }
        }

        ArrayList<String> patternChainList = new ArrayList<>(patternChainSet);
        return semCSV.WrapPatternEitherBlockYaml(OriginalCSVMap, patternChainList);
    }

    private static List<String> getCommonNamespaces() {
        return Arrays.asList(new String[] {
                "java.lang",
                "java.Time",
                "java.net",
                "java.util",
                "java.math",
                "java.math",
                "java.text",
                "java.io",
                "java.sql"
        });
    }
}