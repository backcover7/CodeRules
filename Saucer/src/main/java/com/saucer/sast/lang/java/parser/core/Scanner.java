package com.saucer.sast.lang.java.parser.core;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtExecutableImpl;

import java.sql.SQLException;
import java.util.*;

public class Scanner {
    public void Scan() throws SQLException {
        Collection<CtType<?>> classtypes = SpoonConfig.model.getAllTypes();

        for(CtType<?> classtype : classtypes) {
            Set<CtExecutable<?>> ctExecutables = new HashSet<>();
            ctExecutables.addAll(((CtClassImpl<?>) classtype).getConstructors());
            ctExecutables.addAll(classtype.getMethods());

            for (CtExecutable<?> ctExecutable : ctExecutables) {
                ProcessAnnotation(ctExecutable);
                ProcessConstructor(ctExecutable);
                ProcessMethod(ctExecutable);
            }
        }
    }

    private void ProcessAnnotation(CtExecutable<?> ctExecutable) throws SQLException {
        List<CtAnnotation<?>> ctAnnotationList = ctExecutable.getElements(new TypeFilter<>(CtAnnotation.class));
        for (CtAnnotation<?> annotation : ctAnnotationList) {
            Node node = CheckAnnotation(annotation.getAnnotationType().getQualifiedName());
            setPosition(annotation.getPosition(), node);
            node.setCode(annotation.toString());
            FlagBug(ctExecutable, node);
        }
    }

    private void ProcessConstructor(CtExecutable<?> ctExecutable) throws SQLException {
        List<CtConstructorCall<?>> constructorCallList = ctExecutable.getElements(new TypeFilter<>(CtConstructorCall.class));
        for (CtConstructorCall<?> constructorCall : constructorCallList) {
            // All sink constructor should be fed with tainted data
            if (constructorCall.getExecutable().getParameters().size() != 0) {
                CtExecutableReference<?> executableReference = constructorCall.getExecutable();

                Node node = CheckConstructor(executableReference.getDeclaringType().getQualifiedName());
                setPosition(executableReference.getParent().getPosition(), node);
                node.setCode(constructorCall.toString());
                FlagBug(ctExecutable, node);
            }
        }
    }

    private void ProcessMethod(CtExecutable<?> ctExecutable) throws SQLException {
        List<CtInvocation<?>> ctInvocationList = ctExecutable.getElements(new TypeFilter<>(CtInvocation.class));
        for (CtInvocation<?> invocation : ctInvocationList) {
            CtExecutableReference<?> executableReference = invocation.getExecutable();
            CtExecutableImpl<?> executableInvocation = (CtExecutableImpl<?>) executableReference.getExecutableDeclaration();

            if (executableReference.isConstructor()) {
                ProcessConstructor(executableReference.getExecutableDeclaration());
            }

            MethodHierarchy methodHierarchy = new MethodHierarchy();
            methodHierarchy.FindMethodDefinition(
                    executableInvocation.getDeclaringType(),
                    executableReference.getSimpleName(),
                    executableReference.getParameters());

            HashSet<String> methodSet = methodHierarchy.getMethodSet();
            for (String methodInHierarchy : methodSet) {
                Node node = CheckMethod(methodInHierarchy,
                        invocation.getExecutable().getSimpleName());
                setPosition(invocation.getPosition(), node);
                node.setCode(invocation.toString());
                FlagBug(ctExecutable, node);
            }
        }
    }

    private void setPosition(SourcePosition position, Node node) {
        if (position instanceof NoSourcePosition) {
            node.setFile(position.toString());
            node.setLine(String.valueOf(-1));
        } else {
            node.setFile(position.getFile().getAbsolutePath());
            node.setLine(String.valueOf(position.getLine()));
        }
    }

    private Node CheckAnnotation(String annotationType) throws SQLException {
        String namespace = FilenameUtils.getBaseName(annotationType);
        String classtype = FilenameUtils.getExtension(annotationType);
        return DbUtils.QueryAnnotationType(namespace, classtype);
    }

    private Node CheckConstructor(String qualifiedName) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryConstructor(namespace, classtype);
    }

    private Node CheckMethod(String qualifiedName, String methodName) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryMethod(namespace, classtype, methodName);
    }

    public boolean CheckBug(Node node) {
        return node.getKind() != null && !node.getKind().equals(Node.GadgetNodeType);
    }

    public void FlagBug(CtExecutable<?> ctExecutable, Node node) {
        if (CheckBug(node)) {
            CharUtils.ReportDangeroursNode(node);
        }
        GenerateCallGraphEdge(ctExecutable, node);
    }

    private void GenerateCallGraphEdge(CtExecutable<?> ctExecutable, Node node) {
        CallGraphNode callGraphNode = new CallGraphNode();

        CtExecutableReference<?> ctExecutableReference = ctExecutable.getReference();

        // If successor is from classpath then stop generating cg for it.
        if (SpoonConfig.model.getAllTypes().contains(ctExecutableReference.getDeclaringType().getDeclaration())) {
            // Set previous node in a call graph edge
            callGraphNode.setPreNamespace(ctExecutableReference.getDeclaringType().getPackage().getQualifiedName());
            callGraphNode.setPreClasstype(ctExecutableReference.getDeclaringType().getQualifiedName());
            callGraphNode.setPreMethod(ctExecutableReference.getSimpleName());
            try {
                callGraphNode.setFilePath(ctExecutableReference.getParent().getPosition().getFile().toString());
            } catch (Exception e) {
                callGraphNode.setFilePath("unknow file");
            }

            callGraphNode.setPreParamSize(ctExecutableReference.getParameters().size());

            // Set successor node in a call graph edge
            callGraphNode.setSuccNamespace(node.getNamespace());
            callGraphNode.setSuccClasstype(node.getClasstype());
            callGraphNode.setSuccMethod(node.getMethod());
            callGraphNode.setSuccCode(node.getCode());

            if (CheckBug(node)) {
                setTaintedPreNodeType(callGraphNode, node);
            } else {
                callGraphNode.setEdgeType(Node.CommonNodeType);
            }

            DbUtils.SaveCG2Db(callGraphNode);
        }
    }

    private void setTaintedPreNodeType(CallGraphNode callGraphNode, Node node) {
        String executableType = node.getNodetype();
        if (executableType.equals(Node.SourceNodeType)) {
            callGraphNode.setEdgeType(executableType);
        } else if (executableType.equals(Node.SinkNodeType)) {
            callGraphNode.setEdgeType(executableType);
        }
    }
}
