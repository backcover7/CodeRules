package com.saucer.sast.lang.java.parser.nodes;

public class SourceNode {
    private int MethodID;
    private MethodNode methodNode;
    private RuleNode ruleNode;

    public int getMethodID() {
        return MethodID;
    }

    public void setMethodID(int methodID) {
        MethodID = methodID;
    }

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
}
