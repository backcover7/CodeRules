package com.saucer.sast.lang.java.parser.core;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.parser.nodes.InvocationNode;
import com.saucer.sast.utils.DbUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class FlowAnalysis {
    /*
     * Possible cases:
     *  source -> sink : isSource + isSourcePropagator + isSinkPropagator -> isSink + isSinkPropagator
     *  source -> node -> sink : isSource + isSourcePropagator -> isSourcePropagator + isSinkPropagator -> isSink + isSinkPropagator
     *  source -> node1 -> node2 -> sink : isSource + isSourcePropagator -> isSourcePropagator + isSinkPropagator -> isSinkPropagator -> isSink + isSinkPropagator
     */
    private int depth = 10;   // TODO default?

    public FlowAnalysis() {}

    public FlowAnalysis(int depth) {
        this.depth = depth;
    }

    public void Analyze() {
        // Update sink node to sink propagator at first time to check if sink exists. Otherwise, stop any flow analysis
        int RowsSinkPropagator = DbUtils.UpdateSinks2SinkPropagator();

        if (RowsSinkPropagator != 0) {
            // Update all types of source node to source propagator at first time.
            int RowsSourcePropagator = DbUtils.UpdateSources2SourcePropagator();
            if (RowsSourcePropagator != 0) {
                // Start to propagate from propagators.
                int count = LoopPropagation();
            }
        } else {
            System.err.println("[!] Found no sinks in this codebase. Will stop analyzing taint flows");
        }
    }

    private int LoopPropagation() {
        // Start sink propagation firstly for convenience of finding convergences.
        int count = 2;
        while (count < (depth + 1) / 2) {  // round up
            int backwardRows = DbUtils.UpdateBackwardPropagator();
            int forwardRows = DbUtils.UpdateForwardPropagator();
            if (backwardRows + forwardRows == 0) {
                break;
            }
            count ++;
        }
        return count;
    }

    private Set<List<Location>> FlowGen() {
        List<InvocationNode> convergences = DbUtils.QueryPropagatorConvergence();

        Set<List<Location>> paths = new HashSet<>();
        for (InvocationNode convergence : convergences) {
            Location convergenLocation = convergence.getInvocationLocation();
            DefaultMutableTreeNode convergencePredTreeNode = new DefaultMutableTreeNode(convergenLocation);
            DefaultMutableTreeNode convergenceSuccTreeNode = (DefaultMutableTreeNode) convergencePredTreeNode.clone();
            Set<Integer> uniquePred = new HashSet<>();
            uniquePred.add(convergence.getInvocationID());
            Set<Integer> uniqueSucc = new HashSet<>();
            uniqueSucc.add(convergence.getInvocationID());

            DbUtils.QueryPredNodes(convergence, convergencePredTreeNode, uniquePred);
            DbUtils.QuerySuccNodes(convergence, convergenceSuccTreeNode, uniqueSucc);

            List<Location[]> predLeafPaths = getAllLeafPath(convergencePredTreeNode);
            List<Location[]> succLeafPaths = getAllLeafPath(convergenceSuccTreeNode);

            for (Location[] predLeafPath : predLeafPaths) {
                for (Location[] succLeafPath : succLeafPaths) {
                    List<Location> succLeafPathList = new ArrayList<>(Arrays.asList(succLeafPath));
                    List<Location> predLeafPathList = new ArrayList<>(Arrays.asList(predLeafPath));
                    Collections.reverse(predLeafPathList);
                    succLeafPathList.remove(0);
                    predLeafPathList.addAll(succLeafPathList);

                    paths.add(predLeafPathList);
                }
            }
        }
        return paths;
    }

    private List<Location[]> getAllLeafPath(DefaultMutableTreeNode convergenceTreeNode) {
        List<Location[]> paths = new ArrayList<>();
        DefaultMutableTreeNode leafNode = convergenceTreeNode.getFirstLeaf();
        paths.add(Arrays.copyOf(leafNode.getUserObjectPath(), leafNode.getUserObjectPath().length, Location[].class));

        DefaultMutableTreeNode leaf = leafNode.getNextLeaf();
        while (leaf != null) {
            paths.add(Arrays.copyOf(leaf.getUserObjectPath(), leaf.getUserObjectPath().length, Location[].class));
            leaf = leaf.getNextLeaf();
        }
        return paths;
    }

    public List<Result> getTaintFlows() {
        Set<List<Location>> paths = FlowGen();
        List<Result> results = new ArrayList<>();
        paths.forEach(path -> {
            List<ThreadFlowLocation> threadFlowLocations = new ArrayList<>();
            for (Location location : path) {
                ThreadFlowLocation threadFlowLocation = new ThreadFlowLocation().withLocation(location);
                threadFlowLocations.add(threadFlowLocation);
            }

            List<ThreadFlow> threadFlows = new ArrayList<>();
            ThreadFlow threadFlow = new ThreadFlow().withLocations(threadFlowLocations);
            threadFlows.add(threadFlow);

            List<CodeFlow> codeFlows = new ArrayList<>();
            CodeFlow codeFlow = new CodeFlow().withThreadFlows(threadFlows);
            codeFlows.add(codeFlow);

            Location sink = path.get(path.size() - 1);
            results.add(
                    new Result()
                            .withRuleId(path.get(path.size() - 1).getPhysicalLocation().getArtifactLocation().getDescription().getText())
                            .withLocations(Collections.singletonList(sink))
                            .withMessage(new Message().withText("[Taint Flow - "
                                    + path.get(0).getPhysicalLocation().getArtifactLocation().getDescription().getText()
                                    + "] "
                                    + new ArrayList<>(sink.getLogicalLocations()).get(0).getFullyQualifiedName()))
                            .withCodeFlows(codeFlows));
        });
        return results;
    }
}
