package com.saucer.sast.utils;

import com.contrastsecurity.sarif.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.nodes.InvocationNode;
import com.saucer.sast.lang.java.parser.nodes.MethodNode;
import com.saucer.sast.lang.java.parser.nodes.SourceNode;

import java.io.IOException;
import java.lang.Exception;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SemgrepUtils {
    public final static String SemgrepRules = Paths.get("../semgrep").toAbsolutePath().normalize().toString();

    public final static String SemgrepJavaRules = Paths.get(SemgrepRules, "java").toAbsolutePath().normalize().toString();
    public final static String SemgrepJavaSourceRules = Paths.get(SemgrepJavaRules, "sources").toAbsolutePath().normalize().toString();
    public final static String SemgrepJavaSinkRules = Paths.get(SemgrepJavaRules, "sinks").toAbsolutePath().normalize().toString();

    public final static String SemgrepJavascriptRules = Paths.get(SemgrepRules, "javascript").toAbsolutePath().normalize().toString();
    public final static String SemgrepJavascriptSinkRules = Paths.get(SemgrepJavascriptRules, "sinks").toAbsolutePath().normalize().toString();

    public final static String EnableFixFlag = "--autofix";
    public final static String DisableAutoFix = "--no-autofix";
    private final static String METHODSIGNATURE = "method_signature";
    private final static String INVOCATIONSOURCESNIPPET = "invocation_source_snippet";
    private final static String INVOCATIONSINKSNIPPET = "invocation_sink_snippet";
    private final static String EITHERPARAMETERS = "either_parameters";
    private final static String FILEPATH = "file_path";

    public final static String SARIF_FORMAT = "--sarif";
    public final static String DATAFLOWTRACE_FLAG = "--dataflow-traces";

    public final static String EllipsisBody = "{...}";
    public final static String ParamPatternTemplate = "          - pattern: ";

    public final static String[] SemgrepCLI = new String[]{"semgrep", "scan", DATAFLOWTRACE_FLAG, "-f"};

    public static List<Result> RunSemgrepRule(String yaml, String codebase) {
        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(SemgrepCLI));
        cmd.add(yaml);
        cmd.add(codebase);
        cmd.add(SARIF_FORMAT);
        try {
            return ProcessSarifResult(new ProcessBuilder(cmd.toArray(new String[0])).start());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Result> ProcessSarifResult(Process process) throws IOException {
        String stdout = ProcessUtils.StdoutProcess(process);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(stdout, SarifSchema210.class).getRuns().get(0).getResults();
    }

    public static ThreadFlow DetectIntraFlow(InvocationNode source, InvocationNode invocation) {
        // Detect flow from web invocation source to any invocation in the same method.
        List<Result> SemgrepScanRes = new ArrayList<>();
        String YamlRule;
        try {
            if (!invocation.getSourceNode().getRuleNode().isAnnotationFlag()) {
                if (!invocation.getSourceNode().getMethodNode().getMethodLocation().equals(source.getSourceNode().getMethodNode().getMethodLocation())) {
                    YamlRule = CharUtils.StringSubstitute(
                            CreateTemplateMap(source, invocation), FileUtils.readResourceFile2String("taint4source.yaml"));
                    SemgrepScanRes = SemgrepTemplateScan(YamlRule);
                }
            }
        } catch (Exception e) {
            // no data flow if exception thrown here
        }

        if (SemgrepScanRes.size() == 0) {
            return null;
        } else {
            return SemgrepScanRes.get(0).getCodeFlows().get(0).getThreadFlows().get(0);
        }
    }

    public static ThreadFlow DetectIntraFlow(SourceNode sourceNode, InvocationNode invocationNode) {
        List<Result> SemgrepScanRes = new ArrayList<>();
        String YamlRule;
        try {
            if (!invocationNode.getSourceNode().getRuleNode().isAnnotationFlag()) {
                if (ParseParamSize(sourceNode.getMethodNode().getSignature()) == 0 ||
                        ParseParamSize(invocationNode.getSourceNode().getMethodNode().getSignature()) == 0) {
                    // taint flow from non-param method to non-param invocation
                    // taint flow from has-param method to non-param invocation
                    YamlRule = CharUtils.StringSubstitute(
                            CreateTemplateMap(sourceNode, invocationNode), FileUtils.readResourceFile2String("taint2nonparaminvocation.yaml"));
                    SemgrepScanRes = SemgrepTemplateScan(YamlRule);
                } else {
                    // taint flow from has-param method to has-param invocation
                    YamlRule = CharUtils.StringSubstitute(
                            CreateTemplateMap(sourceNode, invocationNode), FileUtils.readResourceFile2String("taint2invocation.yaml"));
                    SemgrepScanRes = SemgrepTemplateScan(YamlRule);
                }
            }
        } catch (Exception e) {
            // not intraflow if exception thrown here
        }

        if (SemgrepScanRes.size() == 0) {
            return null;
        } else {
            ThreadFlow intraflow = SemgrepScanRes.get(0).getCodeFlows().get(0).getThreadFlows().get(0);
            intraflow.setMessage(new Message().withText(invocationNode.getSourceNode().getMethodNode().getSimpleMethodNode().getFullQualifiedName()));
            return intraflow;
        }
    }

    private static List<Result> SemgrepTemplateScan(String YamlRule) {
        List<Result> SemgrepScanRes = new ArrayList<>();

        try {
            java.nio.file.Path tmpRule = Files.createTempFile(Paths.get(FileUtils.OutputDirectory), "tmp_rule", ".yaml");
            FileUtils.WriteFile(tmpRule.toAbsolutePath().normalize().toString(), YamlRule, false);
            SemgrepScanRes = RunSemgrepRule(
                    tmpRule.toAbsolutePath().normalize().toString(), SpoonConfig.codebase);
            Files.deleteIfExists(tmpRule);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SemgrepScanRes;
    }

    private static Map<String, String> CreateTemplateMap(InvocationNode source, InvocationNode invocation) {
        SourceNode ParentSourceNode = DbUtils.QueryMethodNodeFromWebSourceInvocation(source.getInvocationID());
        String parentMethodName = ParentSourceNode.getMethodNode().getSimpleMethodNode().getName();
        int paramSize = ParseParamSize(ParentSourceNode.getMethodNode().getSignature());
        String methodSignature = parentMethodName + CharUtils.leftbracket;
        String eitherParameters = CharUtils.empty;
        for (int i = 0; i < paramSize; i++) {
            String variable = "$VAR" + i;
            methodSignature += variable;

            eitherParameters += ParamPatternTemplate + variable;

            if (i != paramSize - 1) {
                methodSignature += CharUtils.comma;
                eitherParameters +=  CharUtils.LF;
            }
        }
        methodSignature += CharUtils.rightbracket + EllipsisBody;

        Map<String, String> map = new HashMap<>();
        map.put(INVOCATIONSOURCESNIPPET, source.getSnippet());
        map.put(METHODSIGNATURE, methodSignature);
        map.put(EITHERPARAMETERS, eitherParameters);
        map.put(INVOCATIONSINKSNIPPET, invocation.getSnippet());
        map.put(FILEPATH, invocation.getInvocationLocation().getPhysicalLocation().getArtifactLocation().getUri());
        return map;
    }

    private static Map<String, String> CreateTemplateMap(SourceNode sourceNode, InvocationNode invocationNode) {
        MethodNode methodNode = sourceNode.getMethodNode();
        String parentMethodName = methodNode.getSimpleMethodNode().getName();
        int paramSize = ParseParamSize(methodNode.getSignature());
        String methodSignature = parentMethodName + CharUtils.leftbracket;
        String eitherParameters = CharUtils.empty;
        for (int i = 0; i < paramSize; i++) {
            String variable = "$VAR" + i;
            methodSignature += variable;

            eitherParameters += ParamPatternTemplate + variable;

            if (i != paramSize - 1) {
                methodSignature += CharUtils.comma;
                eitherParameters +=  CharUtils.LF;
            }
        }
        methodSignature += CharUtils.rightbracket + EllipsisBody;

        Map<String, String> map = new HashMap<>();
        map.put(INVOCATIONSOURCESNIPPET, sourceNode.getMethodNode().getSimpleMethodNode().getName() + "(...)");
        map.put(METHODSIGNATURE, methodSignature);
        map.put(EITHERPARAMETERS, eitherParameters);
        map.put(INVOCATIONSINKSNIPPET, invocationNode.getSnippet());
        map.put(FILEPATH, methodNode.getMethodLocation().getPhysicalLocation().getArtifactLocation().getUri());
        return map;
    }

    public static int ParseParamSize(String methodSignature) {
        String parameters = CharUtils.RegexMatchLastOccurence("\\(.*\\)", methodSignature);
        if (parameters.contains(CharUtils.leftbracket + CharUtils.rightbracket)) {
            return 0;
        }
        return parameters.split(CharUtils.comma).length;
    }
}
