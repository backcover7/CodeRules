package com.saucer.sast.lang.java.parser.nodes;

import com.saucer.sast.lang.java.parser.nodes.SimpleMethodNode;

// store to db
public class RuleNode {
    private SimpleMethodNode simpleMethodNode;
    private String category;
    private String kind;
    private String rule;
    private boolean isAnnotationFlag;
    private boolean isConstructorFlag;
    private boolean isMethodFlag;
    private boolean isWebAnnotationSource;
    private boolean isNativeGadgetSource;
    private boolean isJsonGadgetSource;
    private boolean isWebInvocationSource;
    private boolean isInvocationSink;
    private boolean isAnnotationSink;  // todo
    private boolean isSourcePropagator;
    private boolean isSinkPropagator;
    public static String ISANNOTATION = "isAnnotation";
    public static String ISCONSTRUCTOR = "isConstructor";
    public static String ISMETHOD = "isMethod";
    public static String ISWEBANNOTATIONSOURCE = "isWebAnnotationSource";
    public static String ISNATIVEGADGETSOURCE = "isNativeGadgetSource";
    public static String ISJSONGADGETSOURCE = "isJsonGadgetSource";
    public static String ISWEBINVOCATIONSOURCE = "isWebInvocationSource";
    public static String ISINVOCATIONSINK = "isInvocationSink";
    public static String ISANNOTATIONSINK = "isAnnotationSink";
    public static String ISSOURCEPROPAGATOR = "isSourcePropagator";
    public static String ISSINKPROPAGATOR = "isSinkPropagator";

    public static String CATEGORY = "category";
    public static String KIND = "kind";
    public static String RULE = "rule";

    public final static String SOURCE = "source";
    public final static String SINK = "sink";
    public final static String GADGET = "gadget";
    public final static String NEGATIVE = "negative";

    public SimpleMethodNode getSimpleMethodNode() {
        return simpleMethodNode;
    }

    public void setSimpleMethodNode(SimpleMethodNode simpleMethodNode) {
        this.simpleMethodNode = simpleMethodNode;
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

    public boolean isAnnotationFlag() {
        return isAnnotationFlag;
    }

    public void setAnnotationFlag(boolean annotationFlag) {
        isAnnotationFlag = annotationFlag;
    }

    public boolean isConstructorFlag() {
        return isConstructorFlag;
    }

    public void setConstructorFlag(boolean constructorFlag) {
        isConstructorFlag = constructorFlag;
    }

    public boolean isMethodFlag() {
        return isMethodFlag;
    }

    public void setMethodFlag(boolean methodFlag) {
        isMethodFlag = methodFlag;
    }

    public boolean isWebAnnotationSource() {
        return isWebAnnotationSource;
    }

    public void setWebAnnotationSource(boolean webAnnotationSource) {
        isWebAnnotationSource = webAnnotationSource;
    }

    public boolean isNativeGadgetSource() {
        return isNativeGadgetSource;
    }

    public void setNativeGadgetSource(boolean nativeGadgetSource) {
        isNativeGadgetSource = nativeGadgetSource;
    }

    public boolean isJsonGadgetSource() {
        return isJsonGadgetSource;
    }

    public void setJsonGadgetSource(boolean jsonGadgetSource) {
        isJsonGadgetSource = jsonGadgetSource;
    }

    public boolean isSourcePropagator() {
        return isSourcePropagator;
    }

    public void setSourcePropagator(boolean sourcePropagator) {
        isSourcePropagator = sourcePropagator;
    }

    public boolean isWebInvocationSource() {
        return isWebInvocationSource;
    }

    public void setWebInvocationSource(boolean webInvocationSource) {
        isWebInvocationSource = webInvocationSource;
    }

    public boolean isInvocationSink() {
        return isInvocationSink;
    }

    public void setInvocationSink(boolean invocationSink) {
        isInvocationSink = invocationSink;
    }

    public boolean isAnnotationSink() {
        return isAnnotationSink;
    }

    public void setAnnotationSink(boolean annotationSink) {
        isAnnotationSink = annotationSink;
    }

    public boolean isSinkPropagator() {
        return isSinkPropagator;
    }

    public void setSinkPropagator(boolean sinkPropagator) {
        isSinkPropagator = sinkPropagator;
    }

    public void setAnnotationFlagTrue() {
        isAnnotationFlag = true;
        isConstructorFlag = false;
        isMethodFlag = false;
    }

    public void setConstructorFlagTrue() {
        isAnnotationFlag = false;
        isConstructorFlag = true;
        isMethodFlag = false;
    }

    public void setMedthodFlagTrue() {
        isAnnotationFlag = false;
        isConstructorFlag = false;
        isMethodFlag = true;
    }
}
