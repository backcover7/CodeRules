package com.saucer.sast.utils;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.core.RuleNode;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;
import com.saucer.sast.lang.java.parser.dataflow.TaintedFlow;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

// https://github.com/Steppschuh/Java-Markdown-Generator
public class MarkdownUtils {
    private static String report;
    private static StringBuilder stringBuilder;
    private static String formatedtimestamp;

    public final static String newline = CharUtils.LF + CharUtils.LF;

    public MarkdownUtils() {
        SimpleDateFormat FilenameDateFormat = new SimpleDateFormat("MM-dd-yyyy-HH:mm:ss:SSS");
        SimpleDateFormat TitleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        Date date = new Date();
        String filenametimestamp = FilenameDateFormat.format(date);
        formatedtimestamp = TitleDateFormat.format(date);

        report = Paths.get(FileUtils.OutputDirectory,
                Paths.get(SpoonConfig.codebase).getFileName().toString() +
                "_Saucer_SAST_Report_" + filenametimestamp + CharUtils.MarkdownExtension).toAbsolutePath().toString();
        stringBuilder = new StringBuilder();
    }

    public void init() {
        StringBuilder reportHeader = new StringBuilder();
        reportHeader.append(header(Link(
                        Paths.get(SpoonConfig.codebase).getFileName().toString(),
                        Paths.get(SpoonConfig.codebase).toUri().toString()) + " - Saucer SAST Scan Report", 1));
        reportHeader.append(CharUtils.LF)
                .append(formatedtimestamp).append(newline)
                .append("*Report tool bugs to [kang.hou@salesforce.com](mailto:kang.hou@salesforce.com?subject=Saucer Bug Report&body=Please attach details and screenshot in the bug report.)*").append(newline);

        FileUtils.WriteFileLn(report, reportHeader.toString(), false);
    }

    public void finish() {
        FileUtils.WriteFile(report, stringBuilder.toString(), true);
    }

    public static void ReportTaintedFlow4WebSourceHeader() {
        stringBuilder.append(header("[P0/1/2] - Exploitable Tainted Flows from Web Sources", 2)).append(CharUtils.LF);
    }

    public static void ReportTaintedFlow4GadgetSourceHeader() {
        stringBuilder.append(header("[P0/1/2] - Exploitable Tainted Flows from Gadget Sources", 2)).append(CharUtils.LF);
    }

    public static void ReportTaintedFlow4SetterGetterConstructorSourceHeader() {
        stringBuilder.append(header("[P0/1/2] - Exploitable Tainted Flows from JSON Marshalsec Sources", 2)).append(CharUtils.LF);
    }

    public static void ReportGadgetSinkNode() {
        stringBuilder.append(header("[P2] - Exploitable Gadget Methods", 2)).append(CharUtils.LF);
    }

    public static void ReportSinkNodeHeader() {
        stringBuilder.append(header("[FYI] - Usage of Sink Functions", 2)).append(CharUtils.LF);
    }

    public static void ReportSourceNodeHeader() {
        stringBuilder.append(header("[FYI] - Usage of Source Functions", 2)).append(CharUtils.LF);
    }

    public static void ReportTaintedFlow(LinkedList<HashMap<String, String>> taintedFlow, String SourceFlag) throws SQLException {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        for (HashMap<String, String> invocation : taintedFlow) {
            String code;
            String node = createNodeInTaintedflow(invocation, DbUtils.PRENAMESPACE, DbUtils.PRECLASSTYPE, DbUtils.PREMETHODNAME);
            String path = Paths.get(invocation.get(DbUtils.FILEPATH)).toUri() + CharUtils.sharp + invocation.get(DbUtils.SUCCLINENUM);
            if (!SourceFlag.equals(TaintedFlow.WEBSOURCEFLAG) && index == 0) {
                code = invocation.get(DbUtils.PRESIGNATURE);
            } else {
                code = invocation.get(DbUtils.SUCCCODE);
            }
            String codeblock = CodeSnippet(code, false);
            sb.append(CharUtils.space).append(index + 1).append(CharUtils.dot).append(CharUtils.space).append(
                    String.join(
                            CharUtils.comma + CharUtils.space,
                            Link(node, path),
                            codeblock))
                    .append(newline);

            if (index == taintedFlow.size() - 1) {
                String kind = DbUtils.QueryInvocationMethodNode(
                        invocation.get(DbUtils.SUCCNAMESPACE),
                        invocation.get(DbUtils.SUCCCLASSTYPE),
                        invocation.get(DbUtils.SUCCMETHODNAME)).getKind();
                stringBuilder.append("### ").append(kind).append(CharUtils.space).append(CharUtils.dash).append(CharUtils.space)
                        .append(Link(node, path))
                        .append(newline);
            }

            index++;
        }
        stringBuilder.append("<details>").append("<summary>").append("Tainted Flow Path Details").append("</summary>").append(newline);
        stringBuilder.append(sb).append("</details>").append(newline);
    }

    public static void ReportGadgetSinkNode(HashMap<String, Object> node) {
        RuleNode gadgetFlowSource = (RuleNode) node.get(CallGraphNode.SinkGadgetNodeFlowSource);
        RuleNode gadgetFlowSink = (RuleNode) node.get(CallGraphNode.SinkGadgetNodeFlowSink);

        stringBuilder.append("### ")
                .append(new BoldText(gadgetFlowSink.getKind()))
                .append(CharUtils.space).append(CharUtils.dash).append(CharUtils.space)
                .append(Link(
                        String.join(CharUtils.dot, gadgetFlowSource.getNamespace(), gadgetFlowSource.getClasstype())
                                + CharUtils.sharp + ReplaceInitMethod(gadgetFlowSource),
                Paths.get(gadgetFlowSink.getFile()).toUri() + CharUtils.sharp + gadgetFlowSource.getLine()
                ))
                .append(CharUtils.LF);

        stringBuilder.append(CodeBlock((String) node.get(DbUtils.DATATRACE)));
        stringBuilder.append(newline);
    }

    private static String ReplaceInitMethod(RuleNode gadgetFlowSource) {
        if (gadgetFlowSource.getMethod().equals("<init>")) {
            return gadgetFlowSource.getClasstype();
        } else {
            return gadgetFlowSource.getMethod();
        }
    }

    public static void ReportNode(RuleNode ruleNode) {
        stringBuilder.append("### ")
                .append(new BoldText(ruleNode.getKind())).append(CharUtils.space).append(CharUtils.dash).append(CharUtils.space);

        String position = Paths.get(ruleNode.getFile()).toUri() + CharUtils.sharp + ruleNode.getLine();
        if (ruleNode.getKind().contains("annotation")) {
            stringBuilder.append(
                    Link(String.join(CharUtils.dot, ruleNode.getNamespace(), ruleNode.getClasstype()), position)
            );
        } else {
            stringBuilder.append(
                    Link(String.join(CharUtils.dot, ruleNode.getNamespace(), ruleNode.getClasstype()) + CharUtils.sharp + ruleNode.getMethod(), position));
        }
        stringBuilder.append(CharUtils.LF);
//        stringBuilder.append(CodeBlock(ruleNode.getMethodcode(), ruleNode.getCode()));
        stringBuilder.append(CodeSnippet(ruleNode.getCode(), true));
        stringBuilder.append(newline);
    }

    private static String Link(String text, String url) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CharUtils.leftsquarebracket).append(text).append(CharUtils.rightsquarebracket)
                .append(CharUtils.leftbracket).append(url).append(CharUtils.rightbracket);
        return stringBuilder.toString();
    }

    private static String CodeSnippet(String code, boolean md) {
        StringBuilder stringBuilder = new StringBuilder();
        if (md) {
            stringBuilder.append(CharUtils.backtick).append(code).append(CharUtils.backtick);
        } else {
            stringBuilder.append("<code>").append(code).append("</code>");
        }

        return stringBuilder.toString();
    }

    private static String CodeBlock(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("```java").append(CharUtils.LF).append(code).append(CharUtils.LF).append("```");
        return sb.toString();
    }

    private static String CodeBlock(String code, String marktext) {
        String escapedCode = StringEscapeUtils.escapeHtml4(code);
        String escapedMarktext = StringEscapeUtils.escapeHtml4(marktext);

        StringBuilder sb = new StringBuilder();
        sb.append("<pre><code>").append(hightlight(escapedMarktext, escapedCode)).append("</pre></code>");
        return sb.toString();
    }

    private static String hightlight(String marktext, String code) {
        return code.replace(marktext, "<mark>" + marktext + "</mark>");
    }

    private static String createNodeInTaintedflow(HashMap<String, String> invocation, String namespace, String classtype, String methodname) {
        String node;
        if (invocation.get(methodname) == null) {
            node = String.join(
                    CharUtils.dot,
                    invocation.get(namespace),
                    invocation.get(classtype)
            );
        } else {
            node = String.join(
                    CharUtils.dot,
                    invocation.get(namespace),
                    invocation.get(classtype)
            ) + CharUtils.sharp + invocation.get(methodname);
        }
        return node;
    }

    private static String header(String title, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(CharUtils.sharp);
        }
        return sb.append(CharUtils.space).append(title).toString();
    }
}