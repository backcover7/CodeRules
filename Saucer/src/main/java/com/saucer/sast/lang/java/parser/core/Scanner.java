package com.saucer.sast.lang.java.parser.core;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import com.saucer.sast.utils.MarkdownUtils;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import com.saucer.sast.utils.DbUtils;
import spoon.support.reflect.declaration.CtClassImpl;

import java.sql.SQLException;
import java.util.*;

public class Scanner {
    public void Scan() throws Exception {
        System.out.println("[*] Analyzing the target source code ...");
        init();

        System.out.println("[*] Processing global tainted flow analysis ...");
        TaintedFlow taintedFlow = new TaintedFlow();
        taintedFlow.Analyze(TaintedFlow.WEBSOURCEFLAG);
        taintedFlow.Analyze(TaintedFlow.GADGETSOURCEFLAGE);
        taintedFlow.Analyze(TaintedFlow.SETTERGETTERCONSTRUCTORFLAG);

        System.out.println("[*] Creating final scan reports ...");
        MarkdownUtils markdownUtils = new MarkdownUtils();
        markdownUtils.init();
        FlagThreats();
        markdownUtils.finish();

        System.out.println("[!] Done!");
    }

    public void init() throws Exception {
        Collection<CtType<?>> classtypes = SpoonConfig.model.getElements(new TypeFilter<>(CtType.class));

        for(CtType<?> classtype : ProgressBar.wrap(classtypes, "[+] Classes Analysis")) {
            Set<CtExecutable<?>> ctExecutables = new HashSet<>();
            if (classtype instanceof CtClassImpl) {
                ctExecutables.addAll(((CtClassImpl<?>) classtype).getConstructors());
            }
            ctExecutables.addAll(classtype.getMethods());

            for (CtExecutable<?> ctExecutable : ctExecutables) {
                boolean isCtExecutableGadgetSource = false;
                MethodHierarchy methodHierarchy = new MethodHierarchy();
                methodHierarchy.FindMethodDefinition(
                        ctExecutable.getReference().getDeclaringType().getDeclaration(),
                        ctExecutable.getReference().getSimpleName(),
                        ctExecutable.getReference().getParameters());

                HashSet<String> methodSet = methodHierarchy.getMethodSet();
                for (String qualifiedName : methodSet) {
                    RuleNode ctExecutableruleNode = CheckGadgetSourceMethod(qualifiedName,
                            ctExecutable.getReference().getSimpleName());
                    if (ctExecutableruleNode.getNodetype() != null &&
                            ctExecutableruleNode.getNodetype().equals(RuleNode.GADGETSOURCENODE)) {
                        isCtExecutableGadgetSource = true;
                        break;
                    }
                }

                ProcessAnnotation(ctExecutable, isCtExecutableGadgetSource);
                ProcessConstructor(ctExecutable, isCtExecutableGadgetSource);
                ProcessMethod(ctExecutable, isCtExecutableGadgetSource);
            }
        }
    }

    private void ProcessAnnotation(CtExecutable<?> ctExecutable, boolean isCtExecutableGadgetSource) throws SQLException {
        List<CtAnnotation<?>> ctAnnotationList = ctExecutable.getElements(new TypeFilter<>(CtAnnotation.class));
        for (CtAnnotation<?> annotation : ctAnnotationList) {
            RuleNode ruleNode = CheckAnnotation(annotation.getAnnotationType().getQualifiedName());
            ruleNode.setMethodcode(getParentMethodSourceCode(ctExecutable));
            setPosition(annotation.getPosition(), ruleNode);
            ruleNode.setCode(annotation.getOriginalSourceFragment().getSourceCode());
            GenerateCallGraphEdge(ctExecutable, ruleNode, isCtExecutableGadgetSource);
        }
    }

    private void ProcessConstructor(CtExecutable<?> ctExecutable, boolean isCtExecutableGadgetSource) throws SQLException {
        List<CtConstructorCall<?>> constructorCallList = ctExecutable.getElements(new TypeFilter<>(CtConstructorCall.class));
        for (CtConstructorCall<?> constructorCall : constructorCallList) {
            // All sink constructor should be fed with tainted data
            if (constructorCall.getExecutable().getParameters().size() != 0) {
                CtExecutableReference<?> executableReference = constructorCall.getExecutable();

                RuleNode ruleNode = CheckConstructor(executableReference.getDeclaringType().getQualifiedName());
                ruleNode.setMethodcode(getParentMethodSourceCode(ctExecutable));
                setPosition(executableReference.getParent().getPosition(), ruleNode);
                ruleNode.setCode(constructorCall.getOriginalSourceFragment().getSourceCode());
                GenerateCallGraphEdge(ctExecutable, ruleNode, isCtExecutableGadgetSource);
            }
        }
    }

    private void ProcessMethod(CtExecutable<?> ctExecutable, boolean isCtExecutableGadgetSource) throws Exception {
        List<CtInvocation<?>> ctInvocationList = ctExecutable.getElements(new TypeFilter<>(CtInvocation.class));
        for (CtInvocation<?> invocation : ctInvocationList) {
            CtExecutableReference<?> executableReference = invocation.getExecutable();
            CtTypeReference<?> executableInvocation = executableReference.getDeclaringType();

            if (executableInvocation == null) {
                // missing classpath
                continue;
//                throw new Exception("[!] Missing classpath!");
            }

            if (executableReference.isConstructor()) {
                ProcessConstructor(executableReference.getExecutableDeclaration(), isCtExecutableGadgetSource);
            }

            MethodHierarchy methodHierarchy = new MethodHierarchy();
            methodHierarchy.FindMethodDefinition(
                    executableInvocation.getTypeDeclaration(),
                    executableReference.getSimpleName(),
                    executableReference.getParameters());

            HashSet<String> methodSet = methodHierarchy.getMethodSet();

            RuleNode ruleNode = new RuleNode();
            for (String qualifiedName : methodSet) {
                ruleNode = CheckMethod(qualifiedName,
                        invocation.getExecutable().getSimpleName());

                ruleNode.setMethodcode(getParentMethodSourceCode(ctExecutable));
                setPosition(invocation.getPosition(), ruleNode);
                try {
                    ruleNode.setCode(invocation.getOriginalSourceFragment().getSourceCode());
                } catch (Exception e) {
                    ruleNode.setCode(ruleNode.toString());
                }

                if (methodSet.size() > 1 && ruleNode.getNodetype() != null) {
                    break;
                }
            }
            GenerateCallGraphEdge(ctExecutable, ruleNode, isCtExecutableGadgetSource);
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

    private String getParentMethodSourceCode(CtExecutable<?> ctExecutable) {
        try {
//            String MethodSourceCode = ctExecutable.getReference().getDeclaration().getOriginalSourceFragment().getSourceCode();
//            String extraspaces = CharUtils.RegexMatchLastOccurence("^( )+?(?=})", MethodSourceCode);
//            return MethodSourceCode.replaceAll("(?<=\n)" + extraspaces, CharUtils.empty);
            return SpoonConfig.launcher.getEnvironment().createPrettyPrinter().prettyprint(ctExecutable);
        } catch (Exception e) {
            return ctExecutable.getReference().getDeclaration().toString();
        }
    }

    private RuleNode CheckAnnotation(String annotationType) throws SQLException {
        String namespace = FilenameUtils.getBaseName(annotationType);
        String classtype = FilenameUtils.getExtension(annotationType);
        return DbUtils.QueryAnnotationTypeNode(namespace, classtype);
    }

    private RuleNode CheckConstructor(String qualifiedName) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryConstructorNode(namespace, classtype);
    }

    private RuleNode CheckMethod(String qualifiedName, String methodname) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryInvocationMethodNode(namespace, classtype, methodname);
    }

    private RuleNode CheckGadgetSourceMethod(String qualifiedName, String methodname) throws SQLException {
        String namespace = FilenameUtils.getBaseName(qualifiedName);
        String classtype = FilenameUtils.getExtension(qualifiedName);
        return DbUtils.QueryCtExecutableMethodNode(namespace, classtype, methodname);
    }

    public boolean CheckBug(RuleNode ruleNode) {
        return ruleNode.getNodetype() != null && !ruleNode.getNodetype().equals(RuleNode.GADGETSOURCENODE);
    }

    private void GenerateCallGraphEdge(CtExecutable<?> ctExecutable, RuleNode ruleNode, boolean isCtExecutableGadgetSource) throws SQLException {
        CallGraphNode callGraphNode = new CallGraphNode();
        CtExecutableReference<?> ctExecutableReference = ctExecutable.getReference();

        // If successor is from classpath then stop generating cg for it.
        if (SpoonConfig.model.getElements(new TypeFilter<>(CtType.class))
                .contains(ctExecutableReference.getDeclaringType().getDeclaration())) {
            String preNamespace = ctExecutableReference.getDeclaringType().getTopLevelType().getPackage().getQualifiedName();
            String preClasstype = ctExecutableReference.getDeclaringType().getSimpleName();
            String preMethodname = ctExecutableReference.getSimpleName();

            String succNamespace = ruleNode.getNamespace();
            String succClasstype = ruleNode.getClasstype();
            String succMethodname = ruleNode.getMethod();

            RuleNode negativeNode = DbUtils.QueryNegativeNode(succNamespace, succClasstype, succMethodname);
            if (negativeNode.getNodetype() != null) {
                return;
            }

            // Set previous node in a call graph edge
            callGraphNode.setPreNamespace(preNamespace);
            callGraphNode.setPreClasstype(preClasstype);
            callGraphNode.setPreMethodName(preMethodname);
            setPosition(ctExecutableReference.getParent().getPosition(), ruleNode.getLine(), callGraphNode);
            callGraphNode.setPreParamSize(ctExecutableReference.getParameters().size());

            // Set successor node in a call graph edge
            callGraphNode.setSuccNamespace(succNamespace);
            callGraphNode.setSuccClasstype(succClasstype);
            callGraphNode.setSuccMethodName(succMethodname);
            callGraphNode.setSuccCode(ruleNode.getCode());

            if (CheckBug(ruleNode)) {
                setTaintedPreNodeType(callGraphNode, ruleNode);
            } else {
                callGraphNode.setEdgeType(CallGraphNode.CommonFlowType);
            }

            callGraphNode.setPreGadgetSource(isCtExecutableGadgetSource);
            callGraphNode.setParentCode(ruleNode.getMethodcode());

            DbUtils.SaveCallGaraph2Db(callGraphNode);
        }
    }

    private void setTaintedPreNodeType(CallGraphNode callGraphNode, RuleNode ruleNode) {
        String executableType = ruleNode.getNodetype();
        if (executableType.equals(RuleNode.SOURCENODE)) {
            callGraphNode.setEdgeType(CallGraphNode.SourceFlowType);
        } else if (executableType.equals(RuleNode.SINKNODE)) {
            HashMap<String, String> invocation = getInvocation(callGraphNode);
            TaintedFlow taintedFlow = new TaintedFlow();
            ArrayList<HashMap<String, Object>> SemgrepScanRes = taintedFlow.FlowFromArgs2Invocations(invocation);
            if (SemgrepScanRes.size() != 0) {
                callGraphNode.setEdgeType(CallGraphNode.SinkGadgetNodeFlowType);
            } else {
                callGraphNode.setEdgeType(CallGraphNode.SinkNodeType);
            }
        }
    }

    private HashMap<String, String> getInvocation(CallGraphNode callGraphNode) {
        HashMap<String, String> invocation = new HashMap<>();

        invocation.put(DbUtils.PRENAMESPACE, callGraphNode.getPreNamespace());
        invocation.put(DbUtils.PRECLASSTYPE, callGraphNode.getPreClasstype());
        invocation.put(DbUtils.PREMETHODNAME, callGraphNode.getPreMethodName());
        invocation.put(DbUtils.PRELINENUM, callGraphNode.getPreLineNum());
        invocation.put(DbUtils.PREPARAMSIZE, String.valueOf(callGraphNode.getPreParamSize()));
        invocation.put(DbUtils.PREGADGETSOURCE, String.valueOf(callGraphNode.isPreGadgetSource()));
        invocation.put(DbUtils.SUCCNAMESPACE, callGraphNode.getSuccNamespace());
        invocation.put(DbUtils.SUCCCLASSTYPE, callGraphNode.getSuccClasstype());
        invocation.put(DbUtils.SUCCMETHODNAME, callGraphNode.getSuccMethodName());
        invocation.put(DbUtils.SUCCCODE, callGraphNode.getSuccCode());
        invocation.put(DbUtils.SUCCLINENUM, callGraphNode.getSuccLineNum());
        invocation.put(DbUtils.PARENTCODE, callGraphNode.getParentCode());
        invocation.put(DbUtils.FILEPATH, callGraphNode.getFilePath());
        invocation.put(DbUtils.EDGETYPE, callGraphNode.getEdgeType());

        return invocation;
    }

    public void FlagThreats() throws SQLException {
        // Flag flow of web vulnerability bug
        ArrayList<LinkedList<HashMap<String, String>>> taintedPaths4WebSource = TaintedFlow.getTaintedPaths4WebSource();
        if (taintedPaths4WebSource.size() != 0) {
            MarkdownUtils.ReportTaintedFlow4WebSourceHeader();
            for (LinkedList<HashMap<String, String>> taintedFlow : taintedPaths4WebSource) {
                MarkdownUtils.ReportTaintedFlow(taintedFlow);
            }
        }

        // Flag flow of native deserialization gadget bug
        ArrayList<LinkedList<HashMap<String, String>>> taintedPaths4GadgetSource = TaintedFlow.getTaintedPaths4GadgetSource();
        if (taintedPaths4GadgetSource.size() != 0) {
            MarkdownUtils.ReportTaintedFlow4GadgetSourceHeader();
            for (LinkedList<HashMap<String, String>> taintedFlow : taintedPaths4GadgetSource) {
                MarkdownUtils.ReportTaintedFlow(taintedFlow);
            }
        }

        // Flag flow of marshalsec deserialization gadget bug
        ArrayList<LinkedList<HashMap<String, String>>> taintedPaths4SetterGetterConstructorSource =
                TaintedFlow.getTaintedPaths4SetterGetterConstructorSource();
        if (taintedPaths4SetterGetterConstructorSource.size() != 0) {
            MarkdownUtils.ReportTaintedFlow4SetterGetterConstructorSourceHeader();
            for (LinkedList<HashMap<String, String>> taintedFlow : taintedPaths4SetterGetterConstructorSource) {
                MarkdownUtils.ReportTaintedFlow(taintedFlow);
            }
        }

        // Flag sink gadget bug
        ArrayList<HashMap<String, RuleNode>> SinkGadgetNodes = DbUtils.QuerySinkGadgetNodeFlowRuleNode();
        if (SinkGadgetNodes.size() != 0) {
            MarkdownUtils.ReportGadgetSinkNode();
            for (HashMap<String, RuleNode> sinkGadgetNode : SinkGadgetNodes) {
                // TODO simplify code to data trace
                MarkdownUtils.ReportGadgetSinkNode(sinkGadgetNode);
            }
        }

        // Flag sink node bug
        ArrayList<RuleNode> SinkNodes = DbUtils.QuerySinkNodeFlowRuleNode();
        if (SinkNodes.size() != 0) {
            MarkdownUtils.ReportSinkNodeHeader();
            for (RuleNode node : SinkNodes) {
                // TODO simplify code to less lines
                MarkdownUtils.ReportNode(node);
            }
        }

        // Flag all source nodes
        ArrayList<RuleNode> SourceNodes = DbUtils.QuerySourceNodeFlowRuleNode();
        if (SourceNodes.size() != 0) {
            MarkdownUtils.ReportSourceNodeHeader();
            for (RuleNode node : SourceNodes) {
                // TODO simplify code to less lines
                MarkdownUtils.ReportNode(node);
            }
        }
    }
}
