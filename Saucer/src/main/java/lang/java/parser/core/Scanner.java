package lang.java.parser.core;

import lang.java.config.SpoonConfig;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import utils.CharUtils;
import utils.DbUtils;

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
                    FlagBug(position, node);
                }

                List<CtConstructorCall<?>> constructorCallList = method.getElements(new TypeFilter<>(CtConstructorCall.class));
                for (CtConstructorCall<?> constructorCall : constructorCallList) {
                    CtExecutableReference<?> executableReference = constructorCall.getExecutable();

                    String position = executableReference.getParent().getPosition().toString();
                    Node node = CheckConstructor(executableReference.getDeclaringType().getQualifiedName());
                    FlagBug(position, node);
                }

                List<CtInvocation<?>> ctInvocationList = method.getElements(new TypeFilter<>(CtInvocation.class));
                for (CtInvocation<?> invocation : ctInvocationList) {
                    CtMethod<?> executableInvocation = (CtMethod<?>) invocation.getExecutable().getExecutableDeclaration();

                    String position = invocation.getExecutable().getParent().getPosition().toString();
                    Node node = CheckMethod(executableInvocation.getDeclaringType().getQualifiedName(),
                            invocation.getExecutable().getSimpleName());
                    FlagBug(position, node);
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

    private void FlagBug(String position, Node node) {
        node.setPosition(position);
        CharUtils.ReportNode(node);
    }
}
