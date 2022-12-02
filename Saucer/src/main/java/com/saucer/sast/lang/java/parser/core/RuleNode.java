package com.saucer.sast.lang.java.parser.core;

public class RuleNode {
    private String namespace;
    private String classtype;
    private String method;
    private String kind;
    private String nodetype;
    private String file;
    private String line;
    private String code;
    private String methodcode;

    public final static String SOURCENODE = "source";
    public final static String SINKNODE = "sink";
    public final static String GADGETSOURCENODE = "gadgetsource";
    public final static String NEGATIVENODE = "negative";

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClasstype() {
        return classtype;
    }

    public void setClasstype(String classtype) {
        this.classtype = classtype;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getNodetype() {
        return nodetype;
    }

    public void setNodetype(String nodetype) {
        this.nodetype = nodetype;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String position) {
        this.file = position;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String position) {
        this.line = position;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMethodcode() {
        return methodcode;
    }

    public void setMethodcode(String methodcode) {
        this.methodcode = methodcode;
    }
}
