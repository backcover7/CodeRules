package com.saucer.sast.lang.java.parser.nodes;

import com.contrastsecurity.sarif.Location;

public class InvocationNode {
    private MethodNode methodNode;
    private RuleNode ruleNode;
    private String snippet;
    private Location invocationLocation;
    private int InvocationID;
    private int InvocationMethodID;

    public static String SNIPPET = "snippet";
    public static String INVOCATIONLOCATION = "location";
    public static String INVOCATIOMETHODID = "InvocationMethodID";

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    public RuleNode getRuleNode() {
        return ruleNode;
    }

    public void setRuleNode(RuleNode ruleNode) {
        this.ruleNode = ruleNode;
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

    public int getInvocationMethodID() {
        return InvocationMethodID;
    }

    public void setInvocationMethodID(int invocationMethodID) {
        InvocationMethodID = invocationMethodID;
    }
}
