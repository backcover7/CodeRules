package parser.ast;

import config.SpoonConfig;
import org.apache.commons.io.FilenameUtils;
import parser.converter.CSVDefinition;
import parser.loader.LoadNodes;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

public class Scanner {
    private final static String SOURCE = "source node";
    private final static String SINK = "sink node";

    public void Scan(CtModel model, LoadNodes loadNodes) {

        Collection<CtType<?>> classtypes = model.getAllTypes();
        MethodHierarchy methodHierarchy = new MethodHierarchy();

        for(CtType<?> classtype : classtypes) {
            Set<CtMethod<?>> methods = classtype.getMethods();

            // TODO: Hierachy (https://github.com/pbadenski/call-hierarchy-printer)
            for (CtMethod<?> method : methods) {
                List<CtAnnotation<?>> annotations = method.getAnnotations();
                for (CtAnnotation<?> annotation : annotations) {
                    CtTypeReference<?> annotationType = annotation.getAnnotationType();

                    String annotationTypeQualifiedName = annotationType.getQualifiedName();
                    try {
                        List<Map<String, String>> SouceMethodList = loadNodes.getSourceMap().get(annotationTypeQualifiedName);
                        SouceMethodList.forEach(map -> {
                            if (map.get(CSVDefinition.METHOD).equals(CSVDefinition.ANNOTATION)) {
                                System.out.println("[+] Found a " + SOURCE + " in " + annotation.getPosition());
                            }});
                    } catch (Exception e) {}

                    try {
                        List<Map<String, String>> SinkMethodList = loadNodes.getSinkMap().get(annotationTypeQualifiedName);
                        SinkMethodList.forEach(map -> {
                            if (map.get(CSVDefinition.METHOD).equals(CSVDefinition.ANNOTATION)) {
                                System.out.println("[+] Found a " + SINK + " in " + annotation.getPosition());
                            }
                        });
                    } catch (Exception e) {}
                }

                List<CtExecutableReference<?>> executableReferences = method.getElements(new TypeFilter<>(CtExecutableReference.class));
                for (CtExecutableReference<?> ctExecutableReference : executableReferences) {
                    String methodName = ctExecutableReference.getSimpleName();
                    List<CtTypeReference<?>> parameters = ctExecutableReference.getParameters();

                    if (ctExecutableReference.isConstructor()) {
                        CtType<?> ConstructorType = ctExecutableReference.getType().getTypeDeclaration();
                        String ConstructorDeclarationType = methodHierarchy.FindConstructorDefinition(ConstructorType, parameters);
                        if (ConstructorDeclarationType != null) {
                            System.out.println(ConstructorDeclarationType);
                        }
                    } else {
                        CtType<?> MethodType = ctExecutableReference.getDeclaringType().getTypeDeclaration();
                        String MethodDeclarationType = methodHierarchy.FindMethodDefinition(MethodType, methodName, parameters);
                        if (MethodDeclarationType != null) {
                            System.out.println(MethodDeclarationType);
                        }
                    }
                }
            }
        }
    }

    private void CheckNodes(String qualifiedName, String methodName) {
        CheckSourceNodes(qualifiedName, methodName);
        CheckSinkNodes(qualifiedName, methodName);
    }

    private void CheckSourceNodes(String qualifiedName, String methodName) {

    }

    private void CheckSinkNodes(String qualifiedName, String methodName) {

    }

    public static void main(String[] args) {
        SpoonConfig spoonConfig = new SpoonConfig();

        String codebase = "src/main/resources/sink1.java";
        CtModel model = spoonConfig.getSpoonModel(codebase);

        LoadNodes loadNodes = new LoadNodes();
        new Scanner().Scan(model, loadNodes);
    }
}
