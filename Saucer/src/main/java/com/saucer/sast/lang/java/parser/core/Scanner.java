package com.saucer.sast.lang.java.parser.core;

import org.apache.commons.io.FilenameUtils;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.DbUtils;

import java.sql.SQLException;
import java.util.*;

public class Scanner {
    public void Scan(CtModel model) throws SQLException {
        Collection<CtType<?>> classtypes = model.getAllTypes();

        for(CtType<?> classtype : classtypes) {
            Set<CtMethod<?>> methods = classtype.getMethods();

            for (CtMethod<?> method : methods) {
                List<CtAnnotation<?>> ctAnnotationList = method.getElements(new TypeFilter<>(CtAnnotation.class));
                for (CtAnnotation<?> annotation : ctAnnotationList) {
                    String position = annotation.getPosition().toString();
                    Node node = CheckAnnotation(annotation.getAnnotationType().getQualifiedName());
                    CheckBug(position, node, annotation.toString());
                }

                List<CtConstructorCall<?>> constructorCallList = method.getElements(new TypeFilter<>(CtConstructorCall.class));
                for (CtConstructorCall<?> constructorCall : constructorCallList) {
                    CtExecutableReference<?> executableReference = constructorCall.getExecutable();

                    String position = executableReference.getParent().getPosition().toString();
                    Node node = CheckConstructor(executableReference.getDeclaringType().getQualifiedName());
                    CheckBug(position, node, constructorCall.toString());
                }

                List<CtInvocation<?>> ctInvocationList = method.getElements(new TypeFilter<>(CtInvocation.class));
                for (CtInvocation<?> invocation : ctInvocationList) {
                    CtMethod<?> executableInvocation = (CtMethod<?>) invocation.getExecutable().getExecutableDeclaration();
                    String position = invocation.getExecutable().getParent().getPosition().toString();

                    MethodHierarchy methodHierarchy = new MethodHierarchy();
                    methodHierarchy.FindMethodDefinition(
                            executableInvocation.getDeclaringType(),
                            invocation.getExecutable().getSimpleName(),
                            invocation.getExecutable().getParameters());

                    HashSet<String> methodSet = methodHierarchy.getMethodSet();
                    for (String methodInHierarchy : methodSet) {
                        Node node = CheckMethod(methodInHierarchy,
                                invocation.getExecutable().getSimpleName());
                        CheckBug(position, node, invocation.toString());
                    }
                }
            }
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

    private void CheckBug(String position, Node node, String code) {
        if (node.getKind() != null) {
            node.setPosition(position);
            CharUtils.FlagBug(node, code);
        }
    }
}
