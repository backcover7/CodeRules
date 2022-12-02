package com.saucer.sast.lang.java.parser.dataflow;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.utils.*;
import me.tongfei.progressbar.ProgressBar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class TaintedFlow {
    private final static String METHODDEFINITION = "methodDefinition";
    private final static String SOURCEINVOCATION = "sourceinvocation";
    private final static String PARAMPATTERN = "parampattern";

    public final static String WEBSOURCEFLAG = "websourceflag";
    public final static String GADGETSOURCEFLAGE = "gadgetsourceflag";
    public final static String SETTERGETTERCONSTRUCTORFLAG = "settergetterconstructorflag";

    private static String AnanlyzeFlag;
    private static ArrayList<LinkedList<HashMap<String, String>>> taintedPaths;

    public TaintedFlow() {}

    public TaintedFlow(String SourceFlag) throws SQLException, IOException, InterruptedException {
        AnanlyzeFlag = SourceFlag;
        taintedPaths = new ArrayList<>();
        switch (SourceFlag) {
            case WEBSOURCEFLAG:
                AnalyzeFromSource();
                break;
            case GADGETSOURCEFLAGE:
                AnalyzeFromGadgetSource();
                break;
            case SETTERGETTERCONSTRUCTORFLAG:
                AnalyzeFromSetterGetterConsrtructor();
                break;
            default:
                break;
        }
    }

    public static ArrayList<LinkedList<HashMap<String, String>>> getTaintedPaths() {
        return taintedPaths;
    }

    public void AnalyzeFromSource() throws SQLException, IOException, InterruptedException {
        ArrayList<HashMap<String, String>> sources = DbUtils.QuerySourceCallGraph();
        Analyze(sources, WEBSOURCEFLAG);
    }

    public void AnalyzeFromGadgetSource() throws SQLException, IOException, InterruptedException {
        ArrayList<HashMap<String, String>> sources = DbUtils.QueryGadgetSourceNodeCallGraph();
        Analyze(sources, GADGETSOURCEFLAGE);
    }

    public void AnalyzeFromSetterGetterConsrtructor() throws SQLException, IOException, InterruptedException {
        ArrayList<HashMap<String, String>> sources = DbUtils.QuerySetterGetterConstructorCallGraph();
        Analyze(sources, SETTERGETTERCONSTRUCTORFLAG);
    }

    private void Analyze(ArrayList<HashMap<String, String>> sources, String flag) throws SQLException, IOException, InterruptedException {
        // Start from all sources
        LinkedList<HashMap<String, String>> taintedFlow = new LinkedList<>();
        for (HashMap<String, String> source : ProgressBar.wrap(sources, "[+] Tainted Data Flow Analysis")) {
            ArrayList<HashMap<String, String>> invocations = DbUtils.QuerySuccCallGraph(
                    source.get(DbUtils.PRENAMESPACE),
                    source.get(DbUtils.PRECLASSTYPE),
                    source.get(DbUtils.PREMETHODNAME));
            for (HashMap<String, String> invocation : invocations) {
                if (invocation.equals(source)) {
                    continue;
                }

                ArrayList<HashMap<String, Object>> SemgrepScanRes;
                if (source.get(DbUtils.SUCCMETHODNAME) == null ||
                        flag.equals(GADGETSOURCEFLAGE) ||
                        flag.equals(SETTERGETTERCONSTRUCTORFLAG)) {
                    // Annotation web source, Gadget source, setter/getter/constructor source
                    SemgrepScanRes = FlowFromArgs2Invocations(invocation);
                } else {
                    // Invocation source
                    invocation.put(SOURCEINVOCATION, source.get(DbUtils.SUCCCODE));
                    String taint4source = CharUtils.StringSubsitute(ProcessTemplateMap(invocation),
                            FileUtils.ReadFile2String(FileUtils.taint4source));
                    Path taint4sourcePath =
                            Files.createTempFile(Paths.get(FileUtils.tmp), "taint4source.yaml", ".yaml");
                    FileUtils.WriteFile(taint4sourcePath.toAbsolutePath().toString(), taint4source, false);
                    SemgrepScanRes = SemgrepUtils.RunSemgrepRule(taint4sourcePath.toAbsolutePath().toString(), SpoonConfig.codebase);
                    Files.deleteIfExists(taint4sourcePath);
                }

                if (SemgrepScanRes.size() != 0) {
                    taintedFlow.add(source);
                    taintedFlow.add(invocation);
                    FlowAnalysis(invocation, taintedFlow);
                    taintedFlow.removeLast();
                }
            }
        }
    }

    private void FlowAnalysis(HashMap<String, String> invocation, LinkedList<HashMap<String, String>> taintedFlow) throws SQLException {
        if (invocation.get(DbUtils.EDGETYPE).equals(CallGraphNode.SinkGadgetNodeFlowType)) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(invocation);
            if (SemgrepScanRes.size() != 0 ) {
                taintedPaths.add((LinkedList<HashMap<String, String>>) taintedFlow.clone());
                DbUtils.UpdateSinkFlowEdge(invocation);
                taintedFlow.removeLast();
                return;
            }
        }
        String namespace = invocation.get(DbUtils.SUCCNAMESPACE);
        String classtype = invocation.get(DbUtils.SUCCCLASSTYPE);
        String methodname = invocation.get(DbUtils.SUCCMETHODNAME);
        ArrayList<HashMap<String, String>> succinvocations = DbUtils.QuerySuccCallGraph(namespace, classtype, methodname);
        for (HashMap<String, String> succinvocation : succinvocations) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(succinvocation);

            if (taintedFlow.contains(succinvocation)) {
                continue;
            }

            if (SemgrepScanRes.size() != 0) {
                taintedFlow.add(succinvocation);
                FlowAnalysis(succinvocation, taintedFlow);
            }
        }
        taintedFlow.removeLast();
    }

    public ArrayList<HashMap<String, Object>> FlowFromArgs2Invocations(HashMap<String, String> invocation) {
        if (invocation.get(DbUtils.PREPARAMSIZE).equals(String.valueOf(0))) {
            return new ArrayList<>();
        }
        String taint2invocation = CharUtils.StringSubsitute(ProcessTemplateMap(invocation),
                FileUtils.ReadFile2String(FileUtils.taint2invocation));

        ArrayList<HashMap<String, Object>> res = new ArrayList<>();
        try {
            Path taint2invocationPath = Files.createTempFile(Paths.get(FileUtils.tmp), "taint2invocation", ".yaml");
            FileUtils.WriteFile(taint2invocationPath.toAbsolutePath().toString(), taint2invocation, false);
            res = SemgrepUtils.RunSemgrepRule(
                    taint2invocationPath.toAbsolutePath().toString(), SpoonConfig.codebase);
            Files.deleteIfExists(taint2invocationPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return res;
    }

    private HashMap<String, String> ProcessTemplateMap(HashMap<String, String> map) {
        int paramSize = Integer.parseInt(map.get(DbUtils.PREPARAMSIZE));
        String methodDefinition = map.get(DbUtils.PREMETHODNAME) + CharUtils.leftbracket;
        String parampattern = CharUtils.empty;
        for (int i = 0; i < paramSize; i++) {
            String variable = "$VAR" + i;
            methodDefinition += variable;

            parampattern += SemgrepUtils.ParamPatternTemplate + variable;

            if (i != paramSize - 1) {
                methodDefinition += CharUtils.comma;
                parampattern +=  CharUtils.LF;
            }
        }
        methodDefinition += CharUtils.rightbracket + SemgrepUtils.EllipsisBody;
        map.put(METHODDEFINITION, methodDefinition);
        map.put(PARAMPATTERN, parampattern);
        return map;
    }

    public static void main(String[] args) throws Exception {
        String codebase = "/Users/kang.hou/Desktop/click-nodeps-2.3.0";
        SpoonConfig spoonConfig = new SpoonConfig();
        spoonConfig.init(codebase, "");

        DbUtils.connect();
        new TaintedFlow(GADGETSOURCEFLAGE).AnalyzeFromGadgetSource();
        System.out.println(taintedPaths);
    }
}