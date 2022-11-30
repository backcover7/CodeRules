package com.saucer.sast.lang.java.parser.dataflow;

public class CallGraphNode {
    private String PreNamespace;
    private String PreClasstype;
    private String PreMethod;
    private String PreLineNum;
    private int PreParamSize;
    private String SuccNamespace;
    private String SuccClasstype;
    private String SuccMethod;
    private String SuccCode;
    private String SuccLineNum;
    private String FilePath;
    private String EdgeType;
    private String SinkType;

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

    public String getPreMethod() {
        return PreMethod;
    }

    public void setPreMethod(String preMethod) {
        PreMethod = preMethod;
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

    public String getSuccMethod() {
        return SuccMethod;
    }

    public void setSuccMethod(String succMethod) {
        SuccMethod = succMethod;
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

    public String getEdgeType() {
        return EdgeType;
    }

    public void setEdgeType(String edgeType) {
        EdgeType = edgeType;
    }

    public String getSinkType() {
        return SinkType;
    }

    public void setSinkType(String sinkType) {
        SinkType = sinkType;
    }
}
