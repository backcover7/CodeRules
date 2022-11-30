package com.saucer.sast.lang.java.parser.dataflow;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.FileUtils;
import com.saucer.sast.utils.SemgrepUtils;

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
    private final static String INVOCATION = "invocation";
    private final static String PARAMPATTERN = "parampattern";

    public void Scan() throws SQLException, IOException, InterruptedException {
        // Start from all sources
        LinkedList<String> taintedFlow = new LinkedList<>();

        ArrayList<HashMap<String, String>> sources = DbUtils.QuerySourceNodeCallGraph();
        for (HashMap<String, String> source : sources) {
            ArrayList<HashMap<String, String>> invocations = DbUtils.QuerySuccNodeCallGraph(
                    source.get(DbUtils.PRENAMESPACE),
                    source.get(DbUtils.PRECLASSTYPE),
                    source.get(DbUtils.PREMETHODNAME));
            for (HashMap<String, String> invocation : invocations) {
                if (invocation.get(DbUtils.EDGETYPE).equals(CallGraphNode.SourceFlowType)) {
                    continue;
                }

                ArrayList<HashMap<String, Object>> SemgrepScanRes;
                if (source.get(DbUtils.SUCCMETHODNAME) == null) {
                    // Annotation source
                    SemgrepScanRes = FlowFromArgs2Invocations(invocation);
                } else {
                    // Invocation source
                    String taint4source = CharUtils.StringSubsitute(ProcessTemplateMap(invocation),
                            FileUtils.ReadFile2String(FileUtils.taint4source));
                    Path taint4sourcePath =
                            Files.createTempFile(Paths.get(FileUtils.tmp), "taint4source.yaml", ".yaml");
                    FileUtils.WriteFile(taint4sourcePath.toAbsolutePath().toString(), taint4source, false);
                    SemgrepScanRes = SemgrepUtils.RunSemgrepRule(taint4sourcePath.toAbsolutePath().toString(), SpoonConfig.codebase);
                    Files.deleteIfExists(taint4sourcePath);
                }

                if (SemgrepScanRes.size() != 0) {
                    taintedFlow.add(CharUtils.FormatChainNode(source));
                    taintedFlow.add(CharUtils.FormatChainNode(invocation));
                    FlowAnalysis(invocation, taintedFlow);
                }
            }
        }
    }

    private ArrayList<HashMap<String, Object>> FlowFromArgs2Invocations0(HashMap<String, String> invocation) {
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

    // TODO: bug in taintedpath
    public ArrayList<HashMap<String, Object>> FlowFromArgs2Invocations(HashMap<String, String> invocation) {
        if (invocation.get(DbUtils.PREPARAMSIZE).equals(String.valueOf(0))) {
            return new ArrayList<>();
        }
        ArrayList<HashMap<String, Object>> res = FlowFromArgs2Invocations0(invocation);

        String originalSuccCode = invocation.get(DbUtils.SUCCCODE);
        if (invocation.get(DbUtils.SUCCCODE).contains("java.lang")) {
            invocation.put(DbUtils.SUCCCODE,
                    invocation.get(DbUtils.SUCCCODE).replaceAll("java\\.lang\\.", CharUtils.empty));
            res.addAll(FlowFromArgs2Invocations0(invocation));
            invocation.put(DbUtils.SUCCCODE, originalSuccCode);
        }
        return res;
    }

    private void FlowAnalysis(HashMap<String, String> invocation, LinkedList<String> taintedFlow) throws SQLException {
        if (invocation.get(DbUtils.EDGETYPE).equals(CallGraphNode.SinkGadgetFlowType)) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(invocation);
            if (SemgrepScanRes.size() != 0 ) {
                CharUtils.ReportTaintedFlow(taintedFlow);
                DbUtils.UpdateSinkFlowEdge(invocation);
                taintedFlow.clear();
                return;
            }
        }
        String namespace = invocation.get(DbUtils.SUCCNAMESPACE);
        String classtype = invocation.get(DbUtils.SUCCCLASSTYPE);
        String methodname = invocation.get(DbUtils.SUCCMETHODNAME);
        ArrayList<HashMap<String, String>> succinvocations = DbUtils.QuerySuccNodeCallGraph(namespace, classtype, methodname);
        for (HashMap<String, String> succinvocation : succinvocations) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(succinvocation);
            if (SemgrepScanRes.size() != 0) {
                taintedFlow.add(CharUtils.FormatChainNode(succinvocation));
                FlowAnalysis(succinvocation, taintedFlow);
            }
        }
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
        map.put(INVOCATION, map.get(DbUtils.SUCCCODE));
        map.put(PARAMPATTERN, parampattern);
        return map;
    }
}