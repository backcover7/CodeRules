package com.saucer.sast.lang.java.parser.nodes;

import com.contrastsecurity.sarif.Location;

public class MethodNode {
    private SimpleMethodNode simpleMethodNode;
    private String returntype;
    private String signature;
    private String sourceCode;
    private Location methodLocation;

    public static String METHOD = "methodname";
    public static String RETURNTYPE = "returntype";
    public static String SIGNATURE = "signature";
    public static String SOURCECODE = "sourcecode";
    public static String METHODLOCATION = "methodlocation";

    public SimpleMethodNode getSimpleMethodNode() {
        return simpleMethodNode;
    }

    public void setSimpleMethodNode(SimpleMethodNode simpleMethodNode) {
        this.simpleMethodNode = simpleMethodNode;
    }

    public String getReturntype() {
        return returntype;
    }

    public void setReturntype(String returntype) {
        this.returntype = returntype;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Location getMethodLocation() {
        return methodLocation;
    }

    public void setMethodLocation(Location methodLocation) {
        this.methodLocation = methodLocation;
    }
}
