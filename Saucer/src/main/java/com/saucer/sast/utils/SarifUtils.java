package com.saucer.sast.utils;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.parser.core.FlowAnalysis;
import com.saucer.sast.lang.java.parser.nodes.InvocationNode;

import java.net.URI;
import java.util.*;

// https://github.com/Contrast-Security-OSS/java-sarif
public class SarifUtils {
    private static List<Result> getSinkGadgets() {
        return DbUtils.QuerySinkGadgets();
    }

    private static List<Result> getNodes() {
        List<Result> sinkNodesResults = new ArrayList<>();
        List<InvocationNode> sinkNodes = DbUtils.QuerySinkNodes();
        for (InvocationNode sinkNode : sinkNodes) {
            List<Location> sinkLocations = new ArrayList<>();
            Location sinkLocation = sinkNode.getInvocationLocation();
            sinkLocations.add(sinkLocation);
            Result sinks = new Result().withMessage(new Message().withText(
                    "[FYI - Sink functions] " +
                    new ArrayList<>(sinkLocation.getLogicalLocations()).get(0).getFullyQualifiedName()
                            .replaceAll("\\.null", CharUtils.empty))); // remove null methodname in annotation
            sinkLocations.add(sinkNode.getInvocationLocation());
            sinks.setLocations(sinkLocations);
            sinks.setRuleId(sinkNode.getRuleNode().getRule());
            sinkNodesResults.add(sinks);
        }
        
        List<Result> sourceNodesResults = new ArrayList<>();
        List<InvocationNode> sourceNodes = DbUtils.QuerySourceNodes();
        for (InvocationNode sourceNode : sourceNodes) {
            List<Location> sourceLocations = new ArrayList<>();
            Location sourcLocation = sourceNode.getInvocationLocation();
            sourceLocations.add(sourcLocation);
            Result sources = new Result().withMessage(new Message().withText(
                    "[FYI - Source functions] " +
                    new ArrayList<>(sourcLocation.getLogicalLocations()).get(0).getFullyQualifiedName()
                            .replaceAll("\\.null", CharUtils.empty))); // remove null methodname in annotation
            sources.setLocations(sourceLocations);
            sources.setRuleId(sourceNode.getRuleNode().getRule());
            sourceNodesResults.add(sources);
        }

        List<Result> nodes = new ArrayList<>();
        nodes.addAll(sinkNodesResults);
        nodes.addAll(sourceNodesResults);
        return nodes;
    }

    public static void report() {
        Run run = new Run();

        ToolComponent toolComponent = new ToolComponent()
                .withName("Saucer")
                .withFullName("Saucer")
                .withDownloadUri(URI.create("https://git.soma.salesforce.com/kang-hou/"))
                .withLanguage("Java");

        Tool tool = new Tool();
        tool.setDriver(toolComponent);
        List<Result> results = new ArrayList<>();

        results.addAll(new FlowAnalysis().getTaintFlows());
        results.addAll(getSinkGadgets());
        results.addAll(getNodes());

        run.setTool(tool);
        run.setResults(results);

        List<Run> runs = new ArrayList<>();
        runs.add(run);

        SarifSchema210 sarifSchema210 = new SarifSchema210()
                .with$schema(URI.create("https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/schemas/sarif-schema-2.1.0.json"))
                .withRuns(runs)
                .withVersion(SarifSchema210.Version._2_1_0);

        String sarif = CharUtils.Object2Json(sarifSchema210);
        FileUtils.WriteFile("target/result.sarif", sarif, false);
    }
}
