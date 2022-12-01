package com.saucer.sast.lang.java.parser.dataflow;

public class CallGraphNode {
    private String PreNamespace;
    private String PreClasstype;
    private String PreMethodName;
    private String PreLineNum;
    private int PreParamSize;
    private String SuccNamespace;
    private String SuccClasstype;
    private String SuccMethodName;
    private String SuccCode;
    private String SuccLineNum;
    private String ParentCode;
    private String FilePath;
    private String EdgeType;

    public final static String SourceFlowType = "SourceFlow";
    public final static String SinkNodeType = "SinkNodeFlow";
    public final static String SinkGadgetFlowType = "SinkGadgetFlow";
    public final static String SinkGadgetFlowSource = "SinkGadgetFlowSource";
    public final static String SinkGadgetFlowSink = "SinkGadgetFlowSink";

    public final static String SinkFlowType = "SinkFlow";
    public final static String GadgetFlowType = "GadgetFlow";
    public final static String CommonFlowType = "CommonFlow";

    public String getPreNamespace() {
        return PreNamespace;
    }

    public void setPreNamespace(String preNamespace) {
        PreNamespace = preNamespace;
    }

    public String getPreClasstype() {
        return PreClasstype;
    }

    public void setPreClasstype(String preClasstype) {
        PreClasstype = preClasstype;
    }

    public String getPreMethodName() {
        return PreMethodName;
    }

    public void setPreMethodName(String preMethodName) {
        PreMethodName = preMethodName;
    }

    public String getPreLineNum() {
        return PreLineNum;
    }

    public void setPreLineNum(String preLineNum) {
        PreLineNum = preLineNum;
    }

    public String getSuccNamespace() {
        return SuccNamespace;
    }

    public int getPreParamSize() {
        return PreParamSize;
    }

    public void setPreParamSize(int preParamSize) {
        PreParamSize = preParamSize;
    }

    public void setSuccNamespace(String succNamespace) {
        SuccNamespace = succNamespace;
    }

    public String getSuccClasstype() {
        return SuccClasstype;
    }

    public void setSuccClasstype(String succClasstype) {
        SuccClasstype = succClasstype;
    }

    public String getSuccMethodName() {
        return SuccMethodName;
    }

    public void setSuccMethodName(String succMethodName) {
        SuccMethodName = succMethodName;
    }

    public String getSuccCode() {
        return SuccCode;
    }

    public void setSuccCode(String succCode) {
        SuccCode = succCode;
    }

    public String getSuccLineNum() {
        return SuccLineNum;
    }

    public void setSuccLineNum(String succLineNum) {
        SuccLineNum = succLineNum;
    }

    public String getFilePath() {
        return FilePath;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public String getParentCode() {
        return ParentCode;
    }

    public void setParentCode(String parentCode) {
        ParentCode = parentCode;
    }

    public String getEdgeType() {
        return EdgeType;
    }

    public void setEdgeType(String edgeType) {
        EdgeType = edgeType;
    }
}
