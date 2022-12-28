package com.saucer.sast.lang.java.parser.nodes;

import com.contrastsecurity.sarif.Location;

public class InvocationNode {
    private SourceNode sourceNode;
    private String snippet;
    private Location invocationLocation;
    private int InvocationID;

    public static String SNIPPET = "snippet";
    public static String INVOCATIONLOCATION = "invocationlocation";

    public SourceNode getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(SourceNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public Location getInvocationLocation() {
        return invocationLocation;
    }

    public void setInvocationLocation(Location invocationLocation) {
        this.invocationLocation = invocationLocation;
    }

    public int getInvocationID() {
        return InvocationID;
    }

    public void setInvocationID(int invocationID) {
        InvocationID = invocationID;
    }
}
