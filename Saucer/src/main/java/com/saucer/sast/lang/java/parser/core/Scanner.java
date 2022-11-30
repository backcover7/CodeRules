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
            RuleNode ruleNode = CheckAnnotation(annotation.getAnnotationType().getQualifiedName());
            setPosition(annotation.getPosition(), ruleNode);
            ruleNode.setCode(annotation.toString());
            FlagBug(ctExecutable, ruleNode);
        }
    }

    private void ProcessConstructor(CtExecutable<?> ctExecutable) throws SQLException {
        List<CtConstructorCall<?>> constructorCallList = ctExecutable.getElements(new TypeFilter<>(CtConstructorCall.class));
        for (CtConstructorCall<?> constructorCall : constructorCallList) {
            // All sink constructor should be fed with tainted data
            if (constructorCall.getExecutable().getParameters().size() != 0) {
                CtExecutableReference<?> executableReference = constructorCall.getExecutable();

                RuleNode ruleNode = CheckConstructor(executableReference.getDeclaringType().getQualifiedName());
                setPosition(executableReference.getParent().getPosition(), ruleNode);
                ruleNode.setCode(constructorCall.toString());
                FlagBug(ctExecutable, ruleNode);
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
                RuleNode ruleNode = CheckMethod(methodInHierarchy,
                        invocation.getExecutable().getSimpleName());
                setPosition(invocation.getPosition(), ruleNode);
                ruleNode.setCode(invocation.toString());
                FlagBug(ctExecutable, ruleNode);
            }
        }
    }

    private void setPosition(SourcePosition position, RuleNode node) {
        if (position instanceof NoSourcePosition) {
            node.setFile(position.toString());
            node.setLine(String.valueOf(-1));
        } else {
            node.setFile(position.getFile().getAbsolutePath());
            node.setLine(String.valueOf(position.getLine()));
        }
    }

    private void setPosition(SourcePosition position, String succline, CallGraphNode node) {
        if (position instanceof NoSourcePosition) {
            node.setFilePath(position.toString());
            node.setPreLineNum(String.valueOf(-1));
            node.setSuccLineNum(String.valueOf(-1));
        } else {
            node.setFilePath(position.getFile().getAbsolutePath());
            node.setPreLineNum(String.valueOf(position.getLine()));
            node.setSuccLineNum(succline);
        }
    }

    private RuleNode CheckAnnotation(String annotationType) throws SQLException {
        String namespace = FilenameUtils.getBaseName(annotationType);
        String classtype = FilenameUtils.getExtension(annotationType);
        return DbUtils.QueryAnnotationType(namespace, classtype);
    }

    private RuleNode CheckConstructor(String qualifiedName) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryConstructor(namespace, classtype);
    }

    private RuleNode CheckMethod(String qualifiedName, String methodName) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryMethod(namespace, classtype, methodName);
    }

    public boolean CheckBug(RuleNode ruleNode) {
        return ruleNode.getKind() != null && !ruleNode.getKind().equals(RuleNode.GadgetNodeType);
    }

    public void FlagBug(CtExecutable<?> ctExecutable, RuleNode ruleNode) {
        if (CheckBug(ruleNode)) {
            CharUtils.ReportDangerousNode(ruleNode);
        }
        GenerateCallGraphEdge(ctExecutable, ruleNode);
    }

    private void GenerateCallGraphEdge(CtExecutable<?> ctExecutable, RuleNode ruleNode) {
        CallGraphNode callGraphNode = new CallGraphNode();

        CtExecutableReference<?> ctExecutableReference = ctExecutable.getReference();

        // If successor is from classpath then stop generating cg for it.
        if (SpoonConfig.model.getAllTypes().contains(ctExecutableReference.getDeclaringType().getDeclaration())) {
            // Set previous node in a call graph edge
            callGraphNode.setPreNamespace(ctExecutableReference.getDeclaringType().getPackage().getQualifiedName());
            callGraphNode.setPreClasstype(ctExecutableReference.getDeclaringType().getSimpleName());
            callGraphNode.setPreMethod(ctExecutableReference.getSimpleName());

            setPosition(ctExecutableReference.getParent().getPosition(), ruleNode.getLine(), callGraphNode);
            callGraphNode.setPreParamSize(ctExecutableReference.getParameters().size());

            // Set successor node in a call graph edge
            callGraphNode.setSuccNamespace(ruleNode.getNamespace());
            callGraphNode.setSuccClasstype(ruleNode.getClasstype());
            callGraphNode.setSuccMethod(ruleNode.getMethod());
            callGraphNode.setSuccCode(ruleNode.getCode());

            if (CheckBug(ruleNode)) {
                setTaintedPreNodeType(callGraphNode, ruleNode);
            } else {
                callGraphNode.setEdgeType(RuleNode.CommonNodeType);
            }

            DbUtils.SaveCG2Db(callGraphNode);
        }
    }

    private void setTaintedPreNodeType(CallGraphNode callGraphNode, RuleNode ruleNode) {
        String executableType = ruleNode.getNodetype();
        if (executableType.equals(RuleNode.SourceNodeType)) {
            callGraphNode.setEdgeType(executableType);
        } else if (executableType.equals(RuleNode.SinkNodeType)) {
            callGraphNode.setEdgeType(executableType);
            // TODO: gadget
        }
    }
}
