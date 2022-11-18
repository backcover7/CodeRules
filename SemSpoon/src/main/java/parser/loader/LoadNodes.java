package parser.loader;

import parser.converter.CSVDefinition;
import utils.CharUtils;
import utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadNodes {
    private final static String SinkCSV = Paths.get(FileUtils.csv, "nodes/sinks.csv").toString();
    private final static String SourceCSV = Paths.get(FileUtils.csv, "nodes/sources.csv").toString();

    private Map<String, List<Map<String, String>>> SourceMap;
    private Map<String, List<Map<String, String>>> SinkMap;

    public LoadNodes() {
        this.SourceMap = setNodeMap(SourceCSV);;
        this.SinkMap = setNodeMap(SinkCSV);
    }

    public Map<String, List<Map<String, String>>> getSourceMap() {
        return this.SourceMap;
    }

    public Map<String, List<Map<String, String>>> getSinkMap() {
        return this.SinkMap;
    }

    private Map<String, List<Map<String, String>>> setNodeMap(String CSVFile) {
//        {
//            "{namespace.classtype}" : [
//                {
//                  "method" : "{method}",
//                  "kind" : "{kind}"
//                }, ...
//             ]
//        }

        Map<String, List<Map<String, String>>> SinkMap = new HashMap<>();

        BufferedReader lineReader;
        try {
            lineReader = new BufferedReader(new FileReader(CSVFile));
            String csv;
            while ((csv = lineReader.readLine()) != null) {
                if (csv.trim().isEmpty()) {
                    continue;
                }
                String[] csvArray = csv.split(CharUtils.colon);
                if (csvArray.length != 4) {
                    throw new Exception("Wrong format of csv node: " + csv);
                }

                Map<String, String> MethodMap = new HashMap<>();
                MethodMap.put(CSVDefinition.METHOD, csvArray[CSVDefinition.METHODINDEX]);
                MethodMap.put(CSVDefinition.KIND, csvArray[CSVDefinition.KINDINDEX]);

                String namespace = csvArray[CSVDefinition.NAMESPACEINDEX];
                String classtype = csvArray[CSVDefinition.CLASSTYPEINDEX];
                String qualifiedName = namespace + "." + classtype;
                if (SinkMap.containsKey(qualifiedName)){
                    SinkMap.get(qualifiedName).add(MethodMap);
                } else {
                    List<Map<String, String>> ClasstypeList = new ArrayList<>();
                    ClasstypeList.add(MethodMap);
                    SinkMap.put(qualifiedName, ClasstypeList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SinkMap;
    }

    public static void main(String[] args) {
        Object a = new LoadNodes().getSourceMap();
        Object b = new LoadNodes().getSinkMap();
        System.out.println("a");
    }
}
