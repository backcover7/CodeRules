package com.saucer.sast.lang.java.parser.core;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.MarkdownUtils;
import org.apache.commons.io.FilenameUtils;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import com.saucer.sast.utils.DbUtils;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtExecutableImpl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Scanner {
    public void Scan() throws SQLException, IOException, InterruptedException {
        System.out.println("[*] Analyzing the target source code ...");
        init();

        System.out.println("[*] Processing global tainted flow analysis ...");
        TaintedFlow taintedFlow = new TaintedFlow();
        taintedFlow.AnalyzeFromSetterGetterConsrtructor();

        System.out.println("[*] Creating final scan reports ...");
        MarkdownUtils markdownUtils = new MarkdownUtils();
        markdownUtils.init();
        FlagThreats();
        markdownUtils.finish();

        System.out.println("[!] Done!");
    }

    public void init() throws SQLException {
        Collection<CtType<?>> classtypes = SpoonConfig.model.getAllTypes();

        int currentCount = 0;
        int classtypeCounts = classtypes.size();
        for(CtType<?> classtype : classtypes) {
            currentCount ++;
            String progress = currentCount + CharUtils.space + CharUtils.slash + CharUtils.space + classtypeCounts;
            String prompt = "[%] Classes Analysis Progress: " + progress;
            System.out.print(prompt);

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

            for (int j = 0; j <= prompt.length(); j++) {
                System.out.print("\b");
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

    private void ProcessMethod(CtExecutable<?> ctExecutable, boolean isCtExecutableGadgetSource) throws SQLException {
        List<CtInvocation<?>> ctInvocationList = ctExecutable.getElements(new TypeFilter<>(CtInvocation.class));
        for (CtInvocation<?> invocation : ctInvocationList) {
            CtExecutableReference<?> executableReference = invocation.getExecutable();
            CtExecutableImpl<?> executableInvocation = (CtExecutableImpl<?>) executableReference.getExecutableDeclaration();

            if (executableReference.isConstructor()) {
                ProcessConstructor(executableReference.getExecutableDeclaration(), isCtExecutableGadgetSource);
            }

            MethodHierarchy methodHierarchy = new MethodHierarchy();
            methodHierarchy.FindMethodDefinition(
                    executableInvocation.getDeclaringType(),
                    executableReference.getSimpleName(),
                    executableReference.getParameters());

            HashSet<String> methodSet = methodHierarchy.getMethodSet();

            for (String qualifiedName : methodSet) {
                RuleNode ruleNode = CheckMethod(qualifiedName,
                        invocation.getExecutable().getSimpleName());
                ruleNode.setMethodcode(getParentMethodSourceCode(ctExecutable));
                setPosition(invocation.getPosition(), ruleNode);
                try {
                    ruleNode.setCode(invocation.getOriginalSourceFragment().getSourceCode());
                } catch (Exception e) {
                    ruleNode.setCode(ruleNode.toString());
                }
                GenerateCallGraphEdge(ctExecutable, ruleNode, isCtExecutableGadgetSource);
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

    // TODO simplify code
    private String getParentMethodSourceCode(CtExecutable<?> ctExecutable) {
        try {
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

    private void GenerateCallGraphEdge(CtExecutable<?> ctExecutable, RuleNode ruleNode, boolean isCtExecutableGadgetSource) {
        CallGraphNode callGraphNode = new CallGraphNode();

        CtExecutableReference<?> ctExecutableReference = ctExecutable.getReference();

        // If successor is from classpath then stop generating cg for it.
        if (SpoonConfig.model.getAllTypes().contains(ctExecutableReference.getDeclaringType().getDeclaration())) {
            // Set previous node in a call graph edge
            callGraphNode.setPreNamespace(ctExecutableReference.getDeclaringType().getPackage().getQualifiedName());
            callGraphNode.setPreClasstype(ctExecutableReference.getDeclaringType().getSimpleName());
            callGraphNode.setPreMethodName(ctExecutableReference.getSimpleName());
            setPosition(ctExecutableReference.getParent().getPosition(), ruleNode.getLine(), callGraphNode);
            callGraphNode.setPreParamSize(ctExecutableReference.getParameters().size());

            // Set successor node in a call graph edge
            callGraphNode.setSuccNamespace(ruleNode.getNamespace());
            callGraphNode.setSuccClasstype(ruleNode.getClasstype());
            callGraphNode.setSuccMethodName(ruleNode.getMethod());
            callGraphNode.setSuccCode(ruleNode.getCode());

            if (CheckBug(ruleNode)) {
                setTaintedPreNodeType(callGraphNode, ruleNode);
            } else {
                callGraphNode.setEdgeType(CallGraphNode.CommonFlowType);
            }

            callGraphNode.setPreGadgetSource(isCtExecutableGadgetSource);
            callGraphNode.setParentCode(ruleNode.getMethodcode());
            DbUtils.SaveCG2Db(callGraphNode);
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
        MarkdownUtils.ReportTaintedFlowHeader();
        for (LinkedList<HashMap<String, String>> taintedFlow : TaintedFlow.getTaintedPaths()) {
            MarkdownUtils.ReportTaintedFlow(taintedFlow);
        }

        MarkdownUtils.ReportGadgetSinkNode();
        // Flag sink gadget bug
        ArrayList<HashMap<String, RuleNode>> SinkGadgetNodes = DbUtils.QuerySinkGadgetNodeFlowRuleNode();
        for (HashMap<String, RuleNode> sinkGadgetNode : SinkGadgetNodes) {
            MarkdownUtils.ReportGadgetSinkNode(sinkGadgetNode);
        }

        MarkdownUtils.ReportSinkNodeHeader();
        // Flag sink node bug
        ArrayList<RuleNode> SinkNodes = DbUtils.QuerySinkNodeFlowRuleNode();
        for (RuleNode node : SinkNodes) {
            MarkdownUtils.ReportNode(node);
        }

        MarkdownUtils.ReportSourceNodeHeader();
        // Flag all source nodes
        ArrayList<RuleNode> SourceNodes = DbUtils.QuerySourceNodeFlowRuleNode();
        for (RuleNode node : SourceNodes) {
            MarkdownUtils.ReportNode(node);
        }
    }
}
