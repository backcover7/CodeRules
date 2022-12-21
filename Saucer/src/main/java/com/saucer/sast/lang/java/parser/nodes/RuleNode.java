package com.saucer.sast.lang.java.parser.nodes;

// store to db
public class RuleNode {
    private MethodNode methodNode;
    private String category;
    private String kind;
    private String rule;

    public static String CATEGORY = "category";
    public static String KIND = "kind";
    public static String RULE = "rule";

    public final static String SOURCE = "source";
    public final static String SINK = "sink";
    public final static String GADGET = "gadget";
    public final static String NEGATIVE = "negative";

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}
