package com.saucer.sast.lang.java.parser.nodes;

import com.saucer.sast.utils.CharUtils;

public class SimpleMethodNode {
    private ClassNode fullClasstype;
    private String name;

    public ClassNode getFullClasstype() {
        return fullClasstype;
    }

    public void setFullClasstype(ClassNode fullClasstype) {
        this.fullClasstype = fullClasstype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullQualifiedName() {
        return String.join(
                CharUtils.dot,
                fullClasstype.getFullQualifiedName(),
                name
        );
    }
}
