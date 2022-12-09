package com.saucer.sast.utils;

import com.contrastsecurity.sarif.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saucer.sast.lang.java.parser.core.RuleNode;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

// https://github.com/Contrast-Security-OSS/java-sarif
public class SarifUtils {
    private static Location parseNodeLocation(HashMap<String, String> invocation) {
        String qualifiedName = getFullyQualifiedName(
                invocation.get(DbUtils.SUCCNAMESPACE), invocation.get(DbUtils.SUCCCLASSTYPE), invocation.get(DbUtils.SUCCMETHODNAME));
        return parseNodeLocation(
                qualifiedName,
                "", // todo
                invocation.get(DbUtils.FILEPATH),
                invocation.get(DbUtils.SUCCCODE),
                Integer.parseInt(invocation.get(DbUtils.SUCCLINENUM))
        );
    }


    private static Location parseNodeLocation(RuleNode ruleNode) {
        String qualifiedName = getFullyQualifiedName(
                ruleNode.getNamespace(), ruleNode.getClasstype(), ruleNode.getMethod());
        return parseNodeLocation(
                qualifiedName,
                ruleNode.getKind(),
                ruleNode.getFile(),
                ruleNode.getCode(),
                Integer.parseInt(ruleNode.getLine())
        );
    }

    private static String getFullyQualifiedName(String namespace, String classtype, String method) {
        String qualifiedName = String.join(
                CharUtils.dot, namespace, classtype);
        if (method != null) {
            qualifiedName += CharUtils.dot + method;
        }
        return qualifiedName;
    }

    private static Location parseNodeLocation(String qualifiedname, String kind, String file, String code, int line) {
        Set<LogicalLocation> logicalLocations = new HashSet<>();
        LogicalLocation logicalLocation = new LogicalLocation();
        logicalLocation.setFullyQualifiedName(qualifiedname);
        logicalLocation.setKind(kind);
        logicalLocations.add(logicalLocation);

        PhysicalLocation physicalLocation = new PhysicalLocation();
        physicalLocation.setArtifactLocation(new ArtifactLocation().withUri(file));
        physicalLocation.setRegion(
                new Region().withSnippet(new ArtifactContent().withText(code)).withStartLine(line));

        return new Location().withLogicalLocations(logicalLocations).withPhysicalLocation(physicalLocation);
    }

    private static Result getTaintedFlowResult(HashSet<LinkedList<HashMap<String, String>>> flows, String description) {
        List<CodeFlow> codeFlows = new ArrayList<>();
        flows.parallelStream().forEach(flow -> {
            List<ThreadFlowLocation> threadFlowLocations = new LinkedList<>();
            for (HashMap<String, String> node : flow) {
                Location location = parseNodeLocation(node);

                ThreadFlowLocation threadFlowLocation = new ThreadFlowLocation();
                threadFlowLocation.setLocation(location);

                threadFlowLocations.add(threadFlowLocation);
            }
            ThreadFlow threadFlow = new ThreadFlow();
            threadFlow.setLocations(threadFlowLocations);

            List<ThreadFlow> threadFlows = new ArrayList<>();
            threadFlows.add(threadFlow);

            CodeFlow codeFlow = new CodeFlow();
            codeFlow.setThreadFlows(threadFlows);

        });
        Result result = new Result();

        result.setCodeFlows(codeFlows);
        result.setMessage(new Message().withText(description));

        return result;
    }

    private static Result getSourceNodeResult() throws SQLException {
        ArrayList<RuleNode> SourceNodes = DbUtils.QuerySourceNodeFlowRuleNode();
        return getNodeResult(SourceNodes, "Source Nodes");
    }

    private static Result getSinkNodeResult() throws SQLException {
        ArrayList<RuleNode> SourceNodes = DbUtils.QuerySinkNodeFlowRuleNode();
        return getNodeResult(SourceNodes, "Sink Nodes");
    }

    private static Result getNodeResult(ArrayList<RuleNode> nodes, String text) {
        Result result = new Result().withMessage(new Message().withText(text));
        List<Location> locations = new ArrayList<>();

        for (RuleNode node : nodes) {
            Location location = parseNodeLocation(node);
            locations.add(location);
        }
        result.setLocations(locations);
        return result;
    }

    private static List<Result> getSinkGadgetResult() throws SQLException {
        List<Result> results = new ArrayList<>();
        ArrayList<HashMap<String, Object>> SinkGadgetNodes = DbUtils.QuerySinkGadgetNodeFlowRuleNode();
        for (HashMap<String, Object> sinkGadgetNode : SinkGadgetNodes) {
            String result = (String) sinkGadgetNode.get(DbUtils.DATATRACE);
            results.add(getResult4Str(result));
        }
        return results;
    }

    private static Result getResult4Str(String result) {
        // TODO
        return new Result();
    }

    private static Result getWebFlowResult() {
        String description = "";
        Result result = getTaintedFlowResult(TaintedFlow.getTaintedPaths4WebSource(), description);
        result.setMessage(new Message().withText("Saucer found a taint flow from web sources to sinks"));
        return result;
    }

    private static Result getGadgetFlowResult() {
        String description = "";
        Result result = getTaintedFlowResult(TaintedFlow.getTaintedPaths4GadgetSource(), description);
        result.setMessage(new Message().withText("Saucer found a taint flow from native gadget sources to sinks"));
        return result;
    }

    private static Result getJsonFlowResult() {
        String description = "";
        Result result = getTaintedFlowResult(TaintedFlow.getTaintedPaths4SetterGetterConstructorSource(), description);
        result.setMessage(new Message().withText("Saucer found a taint flow from json gadget sources to sinks"));
        return result;
    }

    public static void report() throws SQLException, IOException {
        Run run = new Run();

        ToolComponent toolComponent = new ToolComponent()
                .withFullName("Saucer")
                .withDownloadUri(URI.create("https://git.soma.salesforce.com/kang-hou/"))
                .withLanguage("Java");

        Tool tool = new Tool();
        tool.setDriver(toolComponent);

        List<Result> results = new ArrayList<>();

        results.add(getWebFlowResult());
        results.add(getGadgetFlowResult());
        results.add(getJsonFlowResult());
//        results.addAll(getSinkGadgetResult());
        results.add(getSinkNodeResult());
        results.add(getSourceNodeResult());

        run.setTool(tool);
        run.setResults(results);

        List<Run> runs = new ArrayList<>();
        runs.add(run);

        SarifSchema210 sarifSchema210 = new SarifSchema210()
                .with$schema(URI.create("https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/schemas/sarif-schema-2.1.0.json"))
                .withRuns(runs)
                .withVersion(SarifSchema210.Version._2_1_0);

//        StringWriter writer = new StringWriter();
//        ObjectMapper mapper = new ObjectMapper();
//        JsonGenerator generator = mapper.writerWithDefaultPrettyPrinter().createGenerator(writer);
//        generator.writeObject(sarifSchema210);

        ObjectMapper objectMapper = new ObjectMapper();
        String sarif = objectMapper.writeValueAsString(sarifSchema210);

        FileUtils.WriteFile("target/result.sarif", sarif, false);
    }
}
