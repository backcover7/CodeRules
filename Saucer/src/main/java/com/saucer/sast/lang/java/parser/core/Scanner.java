package com.saucer.sast.lang.java.parser.core;

import com.contrastsecurity.sarif.*;
import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.nodes.*;
import com.saucer.sast.utils.DbUtils;
import com.saucer.sast.utils.SemgrepUtils;
import com.saucer.sast.utils.SpoonUtils;
import com.saucer.sast.utils.SarifUtils;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtClassImpl;

import java.lang.Exception;
import java.util.*;

public class Scanner {
    public void Scan() {
        System.out.println("[*] Analyzing the target source code ...");
        init();

        System.out.println("[*] Processing global tainted flow analysis ...");
        FlowAnalysis flowAnalysis = new FlowAnalysis(Integer.MAX_VALUE - 1);
        flowAnalysis.Analyze();

        System.out.println("[*] Create Sarif Report ...");
        SarifUtils.report();
    }

    public void init() {
        Collection<CtType<?>> classtypes = SpoonConfig.model.getElements(new TypeFilter<>(CtType.class));

        for(CtType<?> classtype: ProgressBar.wrap(classtypes, "[.] Class Analysis       ")) {
            Set<CtExecutable<?>> ctExecutables = new HashSet<>();
            if (classtype instanceof CtClassImpl) {
                ctExecutables.addAll(((CtClassImpl<?>) classtype).getConstructors());
            }
            ctExecutables.addAll(classtype.getMethods());

            for(CtExecutable<?> ctExecutable : ctExecutables) {
                CtExecutableReference<?> ctExecutableReference = ctExecutable.getReference();

                // TODO xstream flow
                MethodNode ctExecutableMethodNode = DbUtils.QueryNativeGadgetSourceMethodNode(ctExecutableReference);
                if (SemgrepUtils.ParseParamSize(ctExecutableReference.getSignature()) == 0) {
                    ctExecutableMethodNode.setJsonGadgetSource(false);
                }

                setInvocationProperties(ctExecutableMethodNode, ctExecutableReference);
                int MethodID = DbUtils.ImportMethodNode(ctExecutableMethodNode);

                try {
                    ProcessAnnotation(ctExecutable, ctExecutableMethodNode, MethodID);
                    ProcessConstructor(ctExecutable, ctExecutableMethodNode, MethodID);
                    ProcessMethod(ctExecutable, ctExecutableMethodNode, MethodID);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("[!] Error");
                }
            }
        }

        DbUtils.ImportWebInvocationSourceFlow();
    }

    private void ProcessAnnotation(CtExecutable<?> ctExecutable, MethodNode ctExecutableMethodNode, int MethodID) {
        List<CtAnnotation<?>> ctAnnotationList = ctExecutable.getElements(new TypeFilter<>(CtAnnotation.class));
        ctAnnotationList.parallelStream().forEach(annotation -> {
            String annotationType = annotation.getAnnotationType().getQualifiedName();
            String namespace = FilenameUtils.getBaseName(annotationType);
            String classtype = FilenameUtils.getExtension(annotationType);
            InvocationNode invocationAnnotation = DbUtils.QueryInvocationAnnotationNode(namespace, classtype);
            invocationAnnotation.setSnippet(annotation.getOriginalSourceFragment().getSourceCode());
            invocationAnnotation.setInvocationLocation(
                    SpoonUtils.ConvertPosition2Location(invocationAnnotation, annotation.getOriginalSourceFragment().getSourcePosition()));

            setInvocationProperties(invocationAnnotation.getMethodNode(), ctExecutable.getReference());
            invocationAnnotation.getMethodNode().setSignature(annotation.getActualAnnotation().toString());

            int MethodIDofInvocation = DbUtils.ImportMethodNode(invocationAnnotation.getMethodNode());
            int InvocationID = DbUtils.ImportInvocationNode(invocationAnnotation, MethodIDofInvocation);
            DbUtils.UpdateParentMethodAsWebAnnoationSource(MethodID);
//            DbUtils.ImportCallgraphNode(MethodID, InvocationID, null);
        });
    }

    private void ProcessConstructor(CtExecutable<?> ctExecutable, MethodNode ctExecutableMethodNode, int MethodID) {
        List<CtConstructorCall<?>> constructorCallList = ctExecutable.getElements(new TypeFilter<>(CtConstructorCall.class));
        constructorCallList.parallelStream().forEach(constructorCall -> {
            // All sink constructor should be fed with tainted data
            if (constructorCall.getExecutable().getParameters().size() != 0) {
                CtExecutableReference<?> executableReference = constructorCall.getExecutable();
                String qualifiedName = executableReference.getDeclaringType().getQualifiedName();
                String namespace = FilenameUtils.getBaseName(qualifiedName);
                String classtype = FilenameUtils.getExtension(qualifiedName);
                InvocationNode invocationConstructor = DbUtils.QueryInvocationConstructorNode(namespace, classtype);
                invocationConstructor.setSnippet(constructorCall.getOriginalSourceFragment().getSourceCode());
                invocationConstructor.setInvocationLocation(
                        SpoonUtils.ConvertPosition2Location(invocationConstructor, constructorCall.getOriginalSourceFragment().getSourcePosition()));
                setInvocationProperties(invocationConstructor.getMethodNode(), executableReference);

                int MethodIDofInvocation = DbUtils.ImportMethodNode(invocationConstructor.getMethodNode());
                int InvocationID = DbUtils.ImportInvocationNode(invocationConstructor, MethodIDofInvocation);
                ThreadFlow intraflow = SemgrepUtils.DetectIntraFlow(ctExecutableMethodNode, invocationConstructor);

                CallGraphNode callGraphNode = new CallGraphNode();
                callGraphNode.setMethodID(MethodID);
                callGraphNode.setInvocationID(InvocationID);
                callGraphNode.setIntraflow(intraflow);

                DbUtils.ImportCallgraphNode(callGraphNode);
            }
        });
    }

    private void ProcessMethod(CtExecutable<?> ctExecutable, MethodNode ctExecutableMethodNode, int MethodID) {
        List<CtInvocation<?>> ctInvocationList = ctExecutable.getElements(new TypeFilter<>(CtInvocation.class));
        for (CtInvocation<?> invocation : ctInvocationList) {
            CtExecutableReference<?> executableReference = invocation.getExecutable();
            CtTypeReference<?> executableInvocation = executableReference.getDeclaringType();

            // missing classpath
            if (executableInvocation == null || executableInvocation.getTypeDeclaration() == null) {
                continue;
                // throw new Exception("[!] Missing classpath!");
            }

            if (executableReference.isConstructor()) {
                ProcessConstructor(executableReference.getExecutableDeclaration(), ctExecutableMethodNode, MethodID);
            }

            MethodHierarchy methodHierarchy = new MethodHierarchy();
            methodHierarchy.FindMethodDefinition(
                    executableInvocation.getTypeDeclaration(),
                    executableReference.getSimpleName(),
                    executableReference.getParameters());

            HashSet<String> methodSet = methodHierarchy.getMethodSet();

            InvocationNode invocationMethod = DbUtils.QueryInvocationMethodNode(executableReference);
            setInvocationProperties(invocationMethod.getMethodNode(), executableReference);

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

            RuleNode ruleNode;
            for (String qualifiedName : methodSet) {
                String namespace = FilenameUtils.getBaseName(qualifiedName);
                String classtype = FilenameUtils.getExtension(qualifiedName);
                ruleNode = DbUtils.QueryInvocationMethodNode(
                        namespace, classtype, invocation.getExecutable().getSimpleName());
                if (ruleNode.getMethodNode().isWebInvocationSource()) {
                    invocationMethod.getMethodNode().setWebInvocationSource(true);
                    invocationMethod.setRuleNode(ruleNode);
                    break;
                } else if (ruleNode.getMethodNode().isSinkInvocation()) {
                    invocationMethod.getMethodNode().setSinkInvocation(true);
                    invocationMethod.setRuleNode(ruleNode);
                    break;
                }
            }

//            if (!SpoonConfig.model.getElements(new TypeFilter<>(CtType.class)).contains(executableInvocation.getTypeDeclaration())) {
//                // This is an invocation from third party dependency. Will not set gadgetsource flag to true on it.
//                invocationMethod.getMethodNode().setNativeGadgetSource(false);
//                invocationMethod.getMethodNode().setJsonGadgetSource(false);
//            }

            if (executableReference.getParameters().size() == 0) {
                // Native gadget source might have no parameters like readObject.
                // But setter/getter/constructor must have parameters to be a Json gadget source
                invocationMethod.getMethodNode().setJsonGadgetSource(false);
            }

            int MethodIDofInvocation = DbUtils.ImportMethodNode(invocationMethod.getMethodNode());
            int InvocationID = DbUtils.ImportInvocationNode(invocationMethod, MethodIDofInvocation);
            ThreadFlow intraflow = SemgrepUtils.DetectIntraFlow(ctExecutableMethodNode, invocationMethod);

            CallGraphNode callGraphNode = new CallGraphNode();
            callGraphNode.setMethodID(MethodID);
            callGraphNode.setInvocationID(InvocationID);
            callGraphNode.setIntraflow(intraflow);

            DbUtils.ImportCallgraphNode(callGraphNode);
        }
    }

    private void setInvocationProperties(MethodNode methodNode, CtExecutableReference<?> executableReference) {
        if (!SpoonConfig.model.getElements(new TypeFilter<>(CtType.class)).contains(executableReference.getDeclaringType().getTypeDeclaration())) {
            // This is an invocation/constructor from third party dependency. Will not set gadgetsource flag to true on it.
            methodNode.setNativeGadgetSource(false);
            methodNode.setJsonGadgetSource(false);
        }
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

//    public void FlagThreats() throws SQLException {
//        // Flag flow of web vulnerability bug
//        HashSet<LinkedList<HashMap<String, String>>> taintedPaths4WebSource = FlowAnalysis.getTaintedPaths4WebSource();
//        if (taintedPaths4WebSource.size() != 0) {
//            MarkdownUtils.ReportTaintedFlow4WebSourceHeader();
//            for (LinkedList<HashMap<String, String>> taintedFlow : taintedPaths4WebSource) {
//                MarkdownUtils.ReportTaintedFlow(taintedFlow, FlowAnalysis.WEBSOURCEFLAG);
//            }
//        }
//
//        // Flag flow of native deserialization gadget bug
//        HashSet<LinkedList<HashMap<String, String>>> taintedPaths4GadgetSource = FlowAnalysis.getTaintedPaths4GadgetSource();
//        if (taintedPaths4GadgetSource.size() != 0) {
//            MarkdownUtils.ReportTaintedFlow4GadgetSourceHeader();
//            for (LinkedList<HashMap<String, String>> taintedFlow : taintedPaths4GadgetSource) {
//                MarkdownUtils.ReportTaintedFlow(taintedFlow, FlowAnalysis.GADGETSOURCEFLAGE);
//            }
//        }
//
//        // Flag flow of marshalsec deserialization gadget bug
//        HashSet<LinkedList<HashMap<String, String>>> taintedPaths4SetterGetterConstructorSource =
//                FlowAnalysis.getTaintedPaths4SetterGetterConstructorSource();
//        if (taintedPaths4SetterGetterConstructorSource.size() != 0) {
//            MarkdownUtils.ReportTaintedFlow4SetterGetterConstructorSourceHeader();
//            for (LinkedList<HashMap<String, String>> taintedFlow : taintedPaths4SetterGetterConstructorSource) {
//                MarkdownUtils.ReportTaintedFlow(taintedFlow, FlowAnalysis.SETTERGETTERCONSTRUCTORFLAG);
//            }
//        }
//
//        // Flag sink gadget bug
//        ArrayList<HashMap<String, Object>> SinkGadgetNodes = DbUtils0.QuerySinkGadgetNodeFlowRuleNode();
//        if (SinkGadgetNodes.size() != 0) {
//            MarkdownUtils.ReportGadgetSinkNode();
//            for (HashMap<String, Object> sinkGadgetNode : SinkGadgetNodes) {
//                MarkdownUtils.ReportGadgetSinkNode(sinkGadgetNode);
//            }
//        }
//
//        // Flag sink node bug
//        ArrayList<FlowNode0> SinkNodes = DbUtils0.QuerySinkNodeFlowRuleNode();
//        if (SinkNodes.size() != 0) {
//            MarkdownUtils.ReportSinkNodeHeader();
//            for (FlowNode0 node : SinkNodes) {
//                MarkdownUtils.ReportNode(node);
//            }
//        }
//
//        // Flag all source nodes
//        ArrayList<FlowNode0> SourceNodes = DbUtils0.QuerySourceNodeFlowRuleNode();
//        if (SourceNodes.size() != 0) {
//            MarkdownUtils.ReportSourceNodeHeader();
//            for (FlowNode0 node : SourceNodes) {
//                MarkdownUtils.ReportNode(node);
//            }
//        }
//    }
}
