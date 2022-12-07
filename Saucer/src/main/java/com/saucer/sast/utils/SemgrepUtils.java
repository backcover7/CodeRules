package com.saucer.sast.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.nio.file.Paths;

public class SemgrepUtils {
    public final static String SemgrepRules = Paths.get("../semgrep").toAbsolutePath().toString();

    public final static String SemgrepJavaRules = Paths.get(SemgrepRules, "java").toAbsolutePath().toString();
    public final static String SemgrepJavaSourceRules = Paths.get(SemgrepJavaRules, "sources").toAbsolutePath().toString();
    public final static String SemgrepJavaSinkRules = Paths.get(SemgrepJavaRules, "sinks").toAbsolutePath().toString();

    public final static String SemgrepJavascriptRules = Paths.get(SemgrepRules, "javascript").toAbsolutePath().toString();
    public final static String SemgrepJavascriptSinkRules = Paths.get(SemgrepJavascriptRules, "sinks").toAbsolutePath().toString();
    
    public final static String EnableFixFlag = "--autofix";
    public final static String DisableAutoFix = "--no-autofix";

    public final static String JSON_FORMAT = "--json";     // results[index][path], results[index][extra][lines], results[index][extra][metavars][$METAVAR][abstract_content]
    public final static String Results = "results";
    public final static String Path = "path";
    public final static String Start = "start";
    public final static String Col = "col";
    public final static String Line = "line";
    public final static String Extra = "extra";
    public final static String Lines = "lines";
    public final static String Dataflow_Traces = "dataflow_trace";
    public final static String Taint_Source = "taint_source";
    public final static String Intermediate_Vars = "intermediate_vars";
    public final static String Content = "content";
    public final static String Location = "location";
    public final static String Metavars = "metavars";
    public final static String Abstract_Content = "abstract_content";

    public final static String EMACS_FORMAT = "--emacs";     // FilePath:Position:Index:Severity(RuleName):MatchCode:Message
    public final static String FilePath = "FilePath";
    public final static String LineNumber = "LineNumber";
    public final static String Index = "Index";
    public final static String Severity = "Severity";
    public final static String MatchCode = "MatchCode";
    public final static String Message = "Message";
    public final static int FilePathIndex = 0;
    public final static int PositionIndex = 1;
    public final static int IndexIndex = 2;
    public final static int SeverityIndex = 3;
    public final static int MatchCodeIndex = 4;
    public final static int MessageIndex = 5;

    public final static String DATAFLOWTRACE_FLAG = "--dataflow-traces";

    public final static String EllipsisBody = "{...}";
    public final static String ParamPatternTemplate = "          - pattern: ";

    public final static String[] SemgrepCLI = new String[]{"semgrep", "scan", DATAFLOWTRACE_FLAG, JSON_FORMAT, "-f"};

    public static ArrayList<HashMap<String, Object>> RunSemgrepRule(String yaml, String codebase) throws IOException {
        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(SemgrepCLI));
        cmd.add(yaml);
        cmd.add(codebase);
//        System.out.println("[.] Running semgrep rule scan...");
        Process process = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
        return ProcessJSONResult(process);
    }

    /**
     * @return ArrayList or null
     */
    public static ArrayList<HashMap<String, Object>> ProcessJSONResult(Process process) throws IOException {
//        System.out.println("[.] Processing JSON format result...");
        String stdout = ProcessUtils.StdoutProcess(process);

        ArrayList<HashMap<String, Object>> resultList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(stdout, Map.class);

        ArrayList<HashMap<String, Object>> semgrepResultList = (ArrayList<HashMap<String, Object>>) map.get(Results);
        for (HashMap<String, Object> semgrepResult : semgrepResultList) {
            String path = (String) semgrepResult.get(Path);
            HashMap<String, Integer> start = (HashMap<String, Integer>) semgrepResult.get(Start);
            String col = String.valueOf(start.get(Col));
            String line = String.valueOf(start.get(Line));
            HashMap<String, Object> extraMap = (HashMap<String, Object>) semgrepResult.get(Extra);
            String lines = (String) extraMap.get(Lines);
            HashMap<String, String> metavars = (HashMap<String, String>) extraMap.get(Metavars);

            String dataflow_trace = CharUtils.empty;
            if (extraMap.containsKey(Dataflow_Traces)) {
                HashMap<String, Object> dataflowtraceMap = (HashMap<String, Object>) extraMap.get(Dataflow_Traces);
                HashMap<String, Object> taintsourceMap = (HashMap<String, Object>) dataflowtraceMap.get(Taint_Source);
                dataflow_trace += ConcatFlowLine(taintsourceMap) + CharUtils.LF;
                if (dataflowtraceMap.containsKey(Intermediate_Vars)) {
                    ArrayList<HashMap<String, Object>> intermediatevarsMap =
                            (ArrayList<HashMap<String, Object>>) dataflowtraceMap.get(Intermediate_Vars);
                    for (HashMap<String, Object> var : intermediatevarsMap) {
                        dataflow_trace += ConcatFlowLine(var) + CharUtils.LF;
                    }
                }
                dataflow_trace += line + CharUtils.vertical + CharUtils.space + lines.trim();
            }

            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put(Path, path);
            resultMap.put(Col, col);
            resultMap.put(Line, line);
            resultMap.put(Lines, lines);
            resultMap.put(Metavars, metavars);
            resultMap.put(Dataflow_Traces, dataflow_trace);

            resultList.add(resultMap);
        }

        return resultList;
    }

    /**
     * @return ArrayList or null
     */
    @Deprecated
    public static ArrayList<HashMap<String, String>> ProcessEMACSResult(Process process) throws IOException {
        System.out.println("[.] Processing EMAC format result...");
        String stdout = ProcessUtils.StdoutProcess(process);

        String[] resultArray = stdout.split(CharUtils.LF);
        ArrayList<HashMap<String, String>> resultList = new ArrayList<>();
        for (String line : resultArray) {
            if (line.equals(CharUtils.empty)) {
                continue;
            }
            String[] lineArray = line.split(CharUtils.colon);
            HashMap<String, String> SingleResMap = new HashMap<>();
            SingleResMap.put(FilePath, lineArray[0]);
            SingleResMap.put(LineNumber, lineArray[1]);
            SingleResMap.put(Index, lineArray[2]);
            SingleResMap.put(Severity, lineArray[3]);
            SingleResMap.put(MatchCode, lineArray[4]);
            SingleResMap.put(Message, lineArray[5]);
            resultList.add(SingleResMap);
        }
        return resultList;
    }

    public static String ReportPosition(HashMap<String, Object> resultMap) {
        return resultMap.get(Path) + CharUtils.sharp +
                resultMap.get(Line) + CharUtils.colon +
                resultMap.get(Col);
    }

    private static String ConcatFlowLine(HashMap<String, Object> flowNode) {
        String sourceContent = (String) flowNode.get(Content);

        String sourceLine = String.valueOf(((HashMap<String, Object>) ((HashMap<String, Object>)
                flowNode.get(Location)).get(Start)).get(Line));
        return sourceLine + CharUtils.vertical + CharUtils.space + sourceContent.trim();
    }
}
