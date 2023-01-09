package com.saucer.sast.lang.java.parser.nodes;

import com.saucer.sast.utils.CharUtils;

public class ClassNode {
    private String namespace;
    private String name;

    public static String NAMESPACE = "namespace";
    public static String CLASSTYPE = "classtype";

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullQualifiedName() {
        if (!namespace.equals(CharUtils.empty)) {
            return String.join(
                    CharUtils.dot,
                    namespace,
                    name
            );
        } else {
            return name;
        }
    }
}
