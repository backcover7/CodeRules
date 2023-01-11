package com.saucer.sast.lang.java.parser.core;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.filter.Extracter;
import com.saucer.sast.lang.java.parser.nodes.*;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.SarifUtils;
import com.saucer.sast.utils.SemgrepUtils;
import com.saucer.sast.utils.SpoonUtils;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtClassImpl;

import java.lang.Exception;
import java.lang.reflect.Method;
import java.util.*;

public class Scanner {
    public void Scan() {
        System.out.println("[*] Analyzing the target source code ...");
        init();

        System.out.println("[*] Processing global tainted flow analysis ...");
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        flowAnalysis.Analyze();

        System.out.println("[*] Create Sarif Report ...");
        SarifUtils.report();
    }

    public void init() {
        Collection<CtType<?>> classtypes = SpoonConfig.model.getElements(new TypeFilter<>(CtType.class));

        ProgressBar.wrap(classtypes.parallelStream(), "[.] Class Analysis").forEach(classtype -> {
            Set<CtExecutable<?>> ctExecutables = new HashSet<>();
            if (classtype instanceof CtClassImpl) {
                ctExecutables.addAll(((CtClassImpl<?>) classtype).getConstructors());
            }
            ctExecutables.addAll(classtype.getMethods());

            ctExecutables.parallelStream().forEach(ctExecutable -> {
                CtExecutableReference<?> ctExecutableReference = ctExecutable.getReference();

                // TODO xstream flow
                RuleNode ctExecutableRuleNode = DbUtils.QueryNativeGadgetSourceMethodNode(ctExecutableReference);

                MethodNode methodNode = new MethodNode();
                methodNode.setSimpleMethodNode(ctExecutableRuleNode.getSimpleMethodNode());
                setInvocationProperties(methodNode, ctExecutableReference);

                if (SemgrepUtils.ParseParamSize(ctExecutableReference.getSignature()) == 0 && ctExecutableRuleNode.isJsonGadgetSource()) {
                    ctExecutableRuleNode.setJsonGadgetSource(false);
                }
                SourceNode ctExecutableSourceNode = new SourceNode();
                ctExecutableSourceNode.setMethodNode(methodNode);
                ctExecutableSourceNode.setRuleNode(ctExecutableRuleNode);
                int MethodID = DbUtils.ImportSourcecode(ctExecutableSourceNode);

                try {
                    ProcessAnnotation(ctExecutable, ctExecutableSourceNode, MethodID);
                    Method processConstructor = Scanner.class.getDeclaredMethod("ProcessConstructor", CtExecutable.class, SourceNode.class, int.class);
                    Method processMethod = Scanner.class.getDeclaredMethod("ProcessMethod", CtExecutable.class, SourceNode.class, int.class);
                    List<Method> methods = Arrays.asList(processConstructor, processMethod);
                    methods.stream()
                            .parallel()
                            .forEach(method -> {
                                try {
                                    method.invoke(this, ctExecutable, ctExecutableSourceNode, MethodID);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("[!] Something wrong when analyzing the source code!");
                }
            });
        });

        Extracter extracter = new Extracter();
        extracter.FilterAndUpdate();

        DbUtils.ImportWebInvocationSourceFlow();
    }

    private void ProcessAnnotation(CtExecutable<?> ctExecutable, SourceNode ctExecutableSourceNode, int MethodID) {
        List<CtAnnotation<?>> ctAnnotationList = ctExecutable.getElements(new TypeFilter<>(CtAnnotation.class));
        ctAnnotationList.parallelStream().forEach(annotation -> {
            String annotationType = annotation.getAnnotationType().getQualifiedName();
            String namespace = FilenameUtils.getBaseName(annotationType);
            String classtype = FilenameUtils.getExtension(annotationType);
            InvocationNode invocationAnnotation = DbUtils.QueryInvocationAnnotationNode(namespace, classtype);
            invocationAnnotation.setSnippet(annotation.getOriginalSourceFragment().getSourceCode());
            invocationAnnotation.setInvocationLocation(
                    SpoonUtils.ConvertPosition2Location(invocationAnnotation, annotation.getOriginalSourceFragment().getSourcePosition()));

            SourceNode annotationSourcenode = invocationAnnotation.getSourceNode();
            setInvocationProperties(annotationSourcenode.getMethodNode(), ctExecutable.getReference());
            invocationAnnotation.getSourceNode().getMethodNode().setSignature(annotation.getActualAnnotation().toString());

            DbUtils.ImportInvocationNode(invocationAnnotation);
            RuleNode ruleNode = invocationAnnotation.getSourceNode().getRuleNode();
            if (annotationSourcenode.getRuleNode().isWebAnnotationSource()) {
                DbUtils.UpdateParentMethodAsWebAnnoationSource(MethodID, ruleNode, RuleNode.ISWEBANNOTATIONSOURCE);
                ctExecutableSourceNode.getRuleNode().setWebAnnotationSource(true);
            } else if (annotationSourcenode.getRuleNode().isAnnotationSink()) {
                DbUtils.UpdateParentMethodAsWebAnnoationSource(MethodID, ruleNode, RuleNode.ISANNOTATIONSINK);
                ctExecutableSourceNode.getRuleNode().setAnnotationSink(true);
            }
        });
    }

    private void ProcessConstructor(CtExecutable<?> ctExecutable, SourceNode ctExecutableSourceNode, int MethodID) {
        List<CtConstructorCall<?>> constructorCallList = ctExecutable.getElements(new TypeFilter<>(CtConstructorCall.class));
        constructorCallList.parallelStream().forEach(constructorCall -> {
            CtExecutableReference<?> executableReference = constructorCall.getExecutable();
            String qualifiedName = executableReference.getDeclaringType().getQualifiedName();
            String namespace = FilenameUtils.getBaseName(qualifiedName);
            String classtype = FilenameUtils.getExtension(qualifiedName);
            InvocationNode invocationConstructor = DbUtils.QueryInvocationConstructorNode(namespace, classtype);
            invocationConstructor.setSnippet(constructorCall.getOriginalSourceFragment().getSourceCode());
            invocationConstructor.setInvocationLocation(
                    SpoonUtils.ConvertPosition2Location(invocationConstructor, constructorCall.getOriginalSourceFragment().getSourcePosition()));
            setInvocationProperties(invocationConstructor.getSourceNode().getMethodNode(), executableReference);

            ThreadFlow intraflow = SemgrepUtils.DetectIntraFlow(ctExecutableSourceNode, invocationConstructor);
            Propagate(ctExecutableSourceNode, invocationConstructor, intraflow);
            int InvocationID = DbUtils.ImportInvocationNode(invocationConstructor);

            CallGraphNode callGraphNode = new CallGraphNode();
            callGraphNode.setMethodID(MethodID);
            callGraphNode.setInvocationID(InvocationID);
            callGraphNode.setIntraflow(intraflow);

            DbUtils.ImportSourceNodeCallgraphNode(callGraphNode);
        });
    }

    private void ProcessMethod(CtExecutable<?> ctExecutable, SourceNode ctExecutableSourceNode, int MethodID) {
        List<CtInvocation<?>> ctInvocationList = ctExecutable.getElements(new TypeFilter<>(CtInvocation.class));
        ctInvocationList.parallelStream().forEach(invocation -> {
            CtExecutableReference<?> executableReference = invocation.getExecutable();
            CtTypeReference<?> executableInvocation = executableReference.getDeclaringType();

            if (executableInvocation == null || executableInvocation.getTypeDeclaration() == null) {
                // missing classpath
                // throw new Exception("[!] Missing classpath!");
                return;
            }

            if (executableReference.isConstructor()) {
                ProcessConstructor(executableReference.getExecutableDeclaration(), ctExecutableSourceNode, MethodID);
            }

            InvocationNode invocationMethod = DbUtils.QueryInvocationMethodNode(executableReference);
            setInvocationProperties(invocationMethod.getSourceNode().getMethodNode(), executableReference);

            try {
                invocationMethod.setSnippet(invocation.getOriginalSourceFragment().getSourceCode());
            } catch (Exception e) {
                invocationMethod.setSnippet(invocation.toString());
            }

            try {
                invocationMethod.setInvocationLocation(
                        SpoonUtils.ConvertPosition2Location(invocationMethod, invocation.getOriginalSourceFragment().getSourcePosition()));
            } catch (Exception e) {
                invocationMethod.setInvocationLocation(new Location());
            }

            ThreadFlow intraflow = SemgrepUtils.DetectIntraFlow(ctExecutableSourceNode, invocationMethod);
            Propagate(ctExecutableSourceNode, invocationMethod, intraflow);
            int InvocationID = DbUtils.ImportInvocationNode(invocationMethod);

            CallGraphNode callGraphNode = new CallGraphNode();
            callGraphNode.setMethodID(MethodID);
            callGraphNode.setInvocationID(InvocationID);
            callGraphNode.setIntraflow(intraflow);

            DbUtils.ImportSourceNodeCallgraphNode(callGraphNode);
        });
    }

    private void setInvocationProperties(MethodNode methodNode, CtExecutableReference<?> executableReference) {
//        if (!SpoonConfig.model.getElements(new TypeFilter<>(CtType.class)).contains(executableReference.getDeclaringType().getTypeDeclaration())) {
//            // This is an invocation/constructor from third party dependency. Will not set gadgetsource flag to true on it.
//            methodNode..setNativeGadgetSource(false);
//            methodNode.setJsonGadgetSource(false);
//        }

        methodNode.setSignature(executableReference.getSignature());
        methodNode.setReturntype(executableReference.getType().getQualifiedName());
        try {
            methodNode.setSourceCode(executableReference.getDeclaration().getOriginalSourceFragment().getSourceCode());
            methodNode.setMethodLocation(
                    SpoonUtils.ConvertPosition2Location(methodNode, executableReference.getDeclaration().getOriginalSourceFragment().getSourcePosition()));
        } catch (Exception e) {
            methodNode.setSourceCode(executableReference.toString());
            methodNode.setMethodLocation(new Location());
        }
    }

    private void Propagate(SourceNode parent, InvocationNode invocationNode, ThreadFlow intraflow) {
        if (intraflow != null) {
            RuleNode parentRule = parent.getRuleNode();
            RuleNode sonRule = invocationNode.getSourceNode().getRuleNode();
            sonRule.setSourcePropagator(
                    parentRule.isSourcePropagator() ||
                            parentRule.isNativeGadgetSource() ||
                            parentRule.isJsonGadgetSource() ||
                            parentRule.isWebAnnotationSource()
            );

            // This will help to find source endpoint in sourcenodes table easily
            sonRule.setWebAnnotationSource(parentRule.isWebAnnotationSource());
            sonRule.setNativeGadgetSource(parentRule.isNativeGadgetSource());
            sonRule.setJsonGadgetSource(parentRule.isJsonGadgetSource());
        }
    }
}
