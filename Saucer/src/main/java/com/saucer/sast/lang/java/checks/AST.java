package com.saucer.sast.lang.java.checks;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.core.Node;
import com.saucer.sast.lang.java.parser.core.Scanner;
import com.saucer.sast.utils.CharUtils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.util.List;

public class AST {
    private final CtType<?> classtype;
    private final SpoonConfig spoonConfig;
    private final Node node = new Node();
    private final Scanner scanner;

    public AST(CtType<?> classtype, SpoonConfig spoonConfig, Scanner scanner) {
        this.classtype = classtype;
        this.spoonConfig = spoonConfig;
        this.scanner = scanner;
        node.setNamespace(classtype.getPackage().toString());
        node.setClasstype(classtype.getSimpleName());
    }

    public void checkMain() {
        List<CtMethod<?>> methods = classtype.getMethodsByName("main");
        for (CtMethod<?> method : methods) {
            if (method.isPublic() &&
                    method.isStatic() &&
                    method.getType().getSimpleName().equals("void") &&
                    method.getParameters().size() == 1 &&
                    method.getParameters().get(0).toString().equals("java.lang.String[] args")) {

                node.setMethod("main");
                node.setKind("main");
                node.setNodetype(Node.SourceNodeType);
                SourcePosition position = method.getPosition();
                node.setFile(position.getFile().getAbsolutePath());
                node.setLine(String.valueOf(position.getLine()));
                node.setCode(method.getSignature());

                // TODO: add to callgraph
                CharUtils.ReportDangeroursNode(node);
            }
        }
    }

    public static void main(String[] args) {

    }
}
