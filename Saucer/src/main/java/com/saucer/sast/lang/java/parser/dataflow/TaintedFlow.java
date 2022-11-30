package com.saucer.sast.lang.java.parser.dataflow;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.core.RuleNode;
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

    public void StartFromSource() throws SQLException, IOException, InterruptedException {
        LinkedList<String> taintedFlow = new LinkedList<>();

        ArrayList<HashMap<String, String>> sources = DbUtils.QuerySourceNode();
        for (HashMap<String, String> source : sources) {
            ArrayList<HashMap<String, String>> invocations = DbUtils.QuerySuccNode(
                    source.get(DbUtils.PRENAMESPACE),
                    source.get(DbUtils.PRECLASSTYPE),
                    source.get(DbUtils.PREMETHODNAME));
            for (HashMap<String, String> invocation : invocations) {
                if (invocation.get(DbUtils.EDGETYPE).equals(RuleNode.SourceNodeType)) {
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

    public ArrayList<HashMap<String, Object>> FlowFromArgs2Invocations0(HashMap<String, String> invocation)
            throws IOException, InterruptedException {
        String taint2invocation = CharUtils.StringSubsitute(ProcessTemplateMap(invocation),
                FileUtils.ReadFile2String(FileUtils.taint2invocation));

        Path taint4annotationsourcePath =
                Files.createTempFile(Paths.get(FileUtils.tmp), "taint2invocation", ".yaml");
        FileUtils.WriteFile(taint4annotationsourcePath.toAbsolutePath().toString(), taint2invocation, false);
        ArrayList<HashMap<String, Object>> res = SemgrepUtils.RunSemgrepRule(
                taint4annotationsourcePath.toAbsolutePath().toString(), SpoonConfig.codebase);
        Files.deleteIfExists(taint4annotationsourcePath);
        return res;
    }

    public ArrayList<HashMap<String, Object>> FlowFromArgs2Invocations(HashMap<String, String> invocation)
            throws IOException, InterruptedException {
        ArrayList<HashMap<String, Object>> res = FlowFromArgs2Invocations0(invocation);

        if (invocation.get(DbUtils.SUCCCODE).contains("java.lang")) {
            invocation.put(DbUtils.SUCCCODE,
                    invocation.get(DbUtils.SUCCCODE).replaceAll("java\\.lang\\.", CharUtils.empty));
            res.addAll(FlowFromArgs2Invocations0(invocation));
        }
        return res;
    }

    private void FlowAnalysis(HashMap<String, String> invocation, LinkedList<String> taintedFlow) throws SQLException, IOException, InterruptedException {
        if (invocation.get(DbUtils.EDGETYPE).equals(RuleNode.SinkNodeType)) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(invocation);
            if (SemgrepScanRes.size() != 0 ) {
                CharUtils.ReportTaintedFlow(taintedFlow);
                taintedFlow.clear();
                return;
            }
        }
        String namespace = invocation.get(DbUtils.SUCCNAMESPACE);
        String classtype = invocation.get(DbUtils.SUCCCLASSTYPE);
        String methodname = invocation.get(DbUtils.SUCCMETHODNAME);
        ArrayList<HashMap<String, String>> succinvocations = DbUtils.QuerySuccNode(namespace, classtype, methodname);
        for (HashMap<String, String> succinvocation : succinvocations) {
            ArrayList<HashMap<String, Object>> SemgrepScanRes = FlowFromArgs2Invocations(succinvocation);
            if (SemgrepScanRes.size() != 0 && succinvocation.get(DbUtils.EDGETYPE).equals(RuleNode.SinkNodeType)) {
                taintedFlow.add(CharUtils.FormatChainNode(succinvocation));
                FlowAnalysis(succinvocation, taintedFlow);
            }
        }
    }

    private HashMap<String, String> ProcessTemplateMap(HashMap<String, String> map) {
        int paramSize = Integer.parseInt(map.get(DbUtils.PREPARAMSIZE));
        String methodDefinition = map.get(DbUtils.PREMETHODNAME) + CharUtils.leftbracket;
        for (int i = 0; i < paramSize; i++) {
            methodDefinition += "$VAR" + i;
            if (i != paramSize - 1) {
                methodDefinition += CharUtils.comma;
            }
        }
        methodDefinition += CharUtils.rightbracket + SemgrepUtils.EllipsisBody;
        map.put(METHODDEFINITION, methodDefinition);
        map.put(INVOCATION, map.get(DbUtils.SUCCCODE));
        return map;
    }
}