package com.saucer.sast.lang.java.parser.dataflow;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.utils.*;
import me.tongfei.progressbar.ProgressBar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

public class TaintedFlow {
    private final static String METHODDEFINITION = "methodDefinition";
    private final static String SOURCEINVOCATION = "sourceinvocation";
    private final static String PARAMPATTERN = "parampattern";

    public final static String WEBSOURCEFLAG = "websourceflag";
    public final static String GADGETSOURCEFLAGE = "gadgetsourceflag";
    public final static String SETTERGETTERCONSTRUCTORFLAG = "settergetterconstructorflag";
    List<String> TaintFlows = Arrays.asList(WEBSOURCEFLAG, GADGETSOURCEFLAGE
            // TODO
//            , SETTERGETTERCONSTRUCTORFLAG
    );

    private static String AnanlyzeFlag;
    private static HashSet<LinkedList<HashMap<String, String>>> taintedPaths4WebSource;
    private static HashSet<LinkedList<HashMap<String, String>>> taintedPaths4GadgetSource;
    private static HashSet<LinkedList<HashMap<String, String>>> taintedPaths4SetterGetterConstructorSource;

    public TaintedFlow() {
        taintedPaths4WebSource = new HashSet<>();
        taintedPaths4GadgetSource = new HashSet<>();
        taintedPaths4SetterGetterConstructorSource = new HashSet<>();
    }

    private String ProgressBarHint(String SourceFlag) {
        switch (SourceFlag) {
            case WEBSOURCEFLAG:
                return "[.] Web Sources Flow";
            case GADGETSOURCEFLAGE:
                return "[.] Gadget Flow     ";
            case SETTERGETTERCONSTRUCTORFLAG:
                return "[.] JSON Flow       ";
            default:
                return CharUtils.empty;
        }
    }

    public void Analyze() {
        TaintFlows.forEach(flow -> {
            try {
                Analyze(flow);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void Analyze(String SourceFlag) throws SQLException {
        AnanlyzeFlag = SourceFlag;
        switch (SourceFlag) {
            case WEBSOURCEFLAG:
                AnalyzeFromSource();
                if (taintedPaths4WebSource.size() != 0) {
                    System.out.println("[+] Found " + taintedPaths4WebSource.size() + " paths from web source to sinks!");
                } else {
                    System.out.println("[-] Found nothing from web sources to sinks.");
                }
                break;
            case GADGETSOURCEFLAGE:
                AnalyzeFromGadgetSource();
                if (taintedPaths4GadgetSource.size() != 0) {
                    System.out.println("[+] Found " + taintedPaths4GadgetSource.size() + " paths from gadget source to sinks!");
                } else {
                    System.out.println("[-] Found nothing from gadget sources to sinks.");
                }
                break;
            case SETTERGETTERCONSTRUCTORFLAG:
                AnalyzeFromSetterGetterConsrtructor();
                if (taintedPaths4SetterGetterConstructorSource.size() != 0) {
                    System.out.println("[+] Found " + taintedPaths4SetterGetterConstructorSource.size() +
                            " paths from JSON marshalsec source to sinks!");
                } else {
                    System.out.println("[-] Found nothing from JSON marshalsec sources to sinks.");
                }
                break;
            default:
                break;
        }
    }

    public static HashSet<LinkedList<HashMap<String, String>>> getTaintedPaths4WebSource() {
        return taintedPaths4WebSource;
    }

    public static HashSet<LinkedList<HashMap<String, String>>> getTaintedPaths4GadgetSource() {
        return taintedPaths4GadgetSource;
    }

    public static HashSet<LinkedList<HashMap<String, String>>> getTaintedPaths4SetterGetterConstructorSource() {
        return taintedPaths4SetterGetterConstructorSource;
    }

    public void AnalyzeFromSource() throws SQLException {
        ArrayList<HashMap<String, String>> sources = DbUtils.QuerySourceCallGraph();
        if (sources.size() != 0) {
            Analyze(sources, WEBSOURCEFLAG);
        }
    }

    public void AnalyzeFromGadgetSource() throws SQLException {
        ArrayList<HashMap<String, String>> sources = DbUtils.QueryGadgetSourceNodeCallGraph();
        if (sources.size() != 0) {
            Analyze(sources, GADGETSOURCEFLAGE);
        }
    }

    public void AnalyzeFromSetterGetterConsrtructor() throws SQLException {
        ArrayList<HashMap<String, String>> sources = DbUtils.QuerySetterGetterConstructorCallGraph();
        if (sources.size() != 0) {
            Analyze(sources, SETTERGETTERCONSTRUCTORFLAG);
        }
    }

    private void Analyze(ArrayList<HashMap<String, String>> sources, String flag) {
        // Start from all sources
        ProgressBar.wrap(sources.parallelStream(), ProgressBarHint(AnanlyzeFlag)).forEach(source -> {
            try {
                ArrayList<HashMap<String, String>> invocations = DbUtils.QuerySuccCallGraph(
                        source.get(DbUtils.PRENAMESPACE),
                        source.get(DbUtils.PRECLASSTYPE),
                        source.get(DbUtils.PREMETHODNAME),
                        source.get(DbUtils.PRESIGNATURE));

                invocations.parallelStream().forEach(invocation -> {
                    if (invocation.equals(source)) {
                        return;
                    }

                    ArrayList<HashMap<String, Object>> SemgrepScanRes = new ArrayList<>();
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

                        try {
                            Path taint4sourcePath =
                                    Files.createTempFile(Paths.get(FileUtils.tmp), "taint4source.yaml", ".yaml");
                            FileUtils.WriteFile(taint4sourcePath.toAbsolutePath().toString(), taint4source, false);
                            SemgrepScanRes = SemgrepUtils.RunSemgrepRule(
                                    taint4sourcePath.toAbsolutePath().toString(), SpoonConfig.codebase);
                            Files.deleteIfExists(taint4sourcePath);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (SemgrepScanRes.size() != 0) {
                        LinkedList<HashMap<String, String>> taintedFlow = new LinkedList<>();
                        taintedFlow.add(source);
                        taintedFlow.add(invocation);
                        try {
                            FlowAnalysis(invocation, taintedFlow);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        taintedFlow.removeLast();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void FlowAnalysis(HashMap<String, String> invocation, LinkedList<HashMap<String, String>> taintedFlow) throws SQLException {
        if (invocation.get(DbUtils.EDGETYPE).equals(CallGraphNode.SinkGadgetNodeFlowType)) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(invocation);
            if (SemgrepScanRes.size() != 0 ) {
                LinkedList<HashMap<String, String>> taintedFlowClone =
                        (LinkedList<HashMap<String, String>>) taintedFlow.clone();
                switch (AnanlyzeFlag) {
                    case WEBSOURCEFLAG:
                        taintedPaths4WebSource.add(taintedFlowClone);
                        break;
                    case GADGETSOURCEFLAGE:
                        taintedPaths4GadgetSource.add(taintedFlowClone);
                        break;
                    case SETTERGETTERCONSTRUCTORFLAG:
                        taintedPaths4SetterGetterConstructorSource.add(taintedFlowClone);
                        break;
                    default:
                        break;
                }
                try {
                    DbUtils.UpdateSinkFlowEdge(invocation);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                taintedFlow.removeLast();
                return;
            }
        }
        String namespace = invocation.get(DbUtils.SUCCNAMESPACE);
        String classtype = invocation.get(DbUtils.SUCCCLASSTYPE);
        String methodname = invocation.get(DbUtils.SUCCMETHODNAME);
        String signature = invocation.get(DbUtils.SUCCSIGNATURE);
        ArrayList<HashMap<String, String>> succinvocations = DbUtils.QuerySuccCallGraph(namespace, classtype, methodname, signature);

        succinvocations.parallelStream().forEach(succinvocation -> {
            LinkedList<HashMap<String, String>> taintedFlowCopy = (LinkedList<HashMap<String, String>>) taintedFlow.clone();
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(succinvocation);
            if (taintedFlowCopy.contains(succinvocation)) {
                return;
            }

            if (SemgrepScanRes.size() != 0) {
                taintedFlowCopy.add(succinvocation);
                try {
                    FlowAnalysis(succinvocation, taintedFlowCopy);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        taintedFlow.removeLast();
    }

    public ArrayList<HashMap<String, Object>> FlowFromArgs2Invocations(HashMap<String, String> invocation) {
        if (ParseParamSize(invocation.get(DbUtils.PRESIGNATURE)) == 0) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private HashMap<String, String> ProcessTemplateMap(HashMap<String, String> map) {
        int paramSize = ParseParamSize(map.get(DbUtils.PRESIGNATURE));
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

    private int ParseParamSize(String methodSignature) {
        String parameters = CharUtils.RegexMatchLastOccurence("\\(.*\\)", methodSignature);
        return parameters.split(CharUtils.comma).length;
    }
}