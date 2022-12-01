package com.saucer.sast.utils;

import com.saucer.sast.lang.java.config.SpoonConfig;
import com.saucer.sast.lang.java.parser.core.RuleNode;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;

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

        report = Paths.get(FileUtils.report,
                Paths.get(SpoonConfig.codebase).getFileName().toString() +
                "_Saucer_SAST_Report_" + filenametimestamp + CharUtils.MarkdownExtension).toAbsolutePath().toString();
        stringBuilder = new StringBuilder();
    }

    public void init() {
        StringBuilder reportHeader = new StringBuilder();
        reportHeader.append("# ")
                .append(Link(
                        Paths.get(SpoonConfig.codebase).getFileName().toString(),
                        Paths.get(SpoonConfig.codebase).toUri().toString()))
                .append(" - Saucer SAST Scan Report").append(CharUtils.LF)
                .append(formatedtimestamp).append(newline)
                .append("*Report tool bugs to [kang.hou@salesforce.com](mailto:kang.hou@salesforce.com?subject=Saucer Bug Report&body=Please attach details and screenshot in the bug report.)*").append(newline);

        FileUtils.WriteFileLn(report, reportHeader.toString(), false);
    }

    public void finish() {
        FileUtils.WriteFile(report, stringBuilder.toString(), true);
    }

    public static void ReportTaintedFlowHeader() {
        stringBuilder.append("## [P0/1] - Exploitable Tainted Flows").append(CharUtils.LF);
    }

    public static void ReportGadgetSinkNode() {
        stringBuilder.append("## [P2] - Exploitable Gadget Methods").append(CharUtils.LF);
    }

    public static void ReportSinkNodeHeader() {
        stringBuilder.append("## [P3] - Usage of Sink Functions").append(CharUtils.LF);
    }

    public static void ReportSourceNodeHeader() {
        stringBuilder.append("## [INFO] - Usage of Source Functions").append(CharUtils.LF);
    }

    public static void ReportTaintedFlow(LinkedList<HashMap<String, String>> taintedFlow) throws SQLException {
        Table.Builder tableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_CENTER, Table.ALIGN_LEFT)
                .addRow("Step", "Code", "Path", "Node");

        int index = 1;
        for (HashMap<String, String> invocation : taintedFlow) {
            String code = invocation.get(DbUtils.SUCCCODE);
            String codeblock = CodeBlock(code);
            String path = invocation.get(DbUtils.FILEPATH) + CharUtils.sharp + invocation.get(DbUtils.SUCCLINENUM);
            String node;
            if (invocation.get(DbUtils.SUCCMETHODNAME) == null) {
                node = String.join(
                        CharUtils.dot,
                        invocation.get(DbUtils.SUCCNAMESPACE),
                        invocation.get(DbUtils.SUCCCLASSTYPE)
                        );
            } else {
                node = String.join(
                        CharUtils.dot,
                        invocation.get(DbUtils.SUCCNAMESPACE),
                        invocation.get(DbUtils.SUCCCLASSTYPE),
                        invocation.get(DbUtils.SUCCMETHODNAME)
                );
            }

            tableBuilder.addRow(index, codeblock, path, node);

            if (index == taintedFlow.size()) {
                String kind = DbUtils.QueryInvocationMethodNode(invocation.get(
                        DbUtils.SUCCNAMESPACE),
                        invocation.get(DbUtils.SUCCCLASSTYPE),
                        invocation.get(DbUtils.SUCCMETHODNAME)).getKind();
                stringBuilder.append("### ")
                        .append(kind).append(CharUtils.space).append(CharUtils.dash).append(CharUtils.space)
                        .append(Link(node, Paths.get(path).toUri().toString()))
                        .append(CharUtils.LF);
            }

            index++;
        }
        stringBuilder.append(tableBuilder.build()).append(newline);
    }

    public static void ReportGadgetSinkNode(HashMap<String, RuleNode> node) {
        RuleNode gadgetFlowSource = node.get(CallGraphNode.SinkGadgetNodeFlowSource);
        RuleNode gadgetFlowSink = node.get(CallGraphNode.SinkGadgetNodeFlowSink);

        stringBuilder.append("### ")
                .append(new BoldText(gadgetFlowSink.getKind()))
                .append(CharUtils.space).append(CharUtils.dash).append(CharUtils.space)
                .append(Link(String.join(CharUtils.dot, gadgetFlowSource.getNamespace(), gadgetFlowSource.getClasstype(), gadgetFlowSource.getMethod()),
                Paths.get(gadgetFlowSink.getFile()).toUri() + CharUtils.sharp + gadgetFlowSource.getLine()
                ))
                .append(CharUtils.LF);
//        String text;
//        if (gadgetFlowSink.getKind().contains("annotation")) {
//            text = String.join(CharUtils.dot,
//                    gadgetFlowSink.getNamespace(), gadgetFlowSink.getClasstype());
//        } else {
//            text = String.join(CharUtils.dot,
//                    gadgetFlowSink.getNamespace(), gadgetFlowSink.getClasstype(), gadgetFlowSink.getMethod());
//        }
//        stringBuilder.append(Link(text, gadgetFlowSink.getFile() + CharUtils.sharp + gadgetFlowSink.getLine())).append(newline);
        stringBuilder.append(CodeBlock(gadgetFlowSink.getMethodcode(), gadgetFlowSink.getCode()));
        stringBuilder.append(newline);
    }

    public static void ReportNode(RuleNode ruleNode) {
        stringBuilder.append("### ")
                .append(new BoldText(ruleNode.getKind())).append(CharUtils.space).append(CharUtils.dash).append(CharUtils.space);

        String position = Paths.get(ruleNode.getFile() + CharUtils.sharp + ruleNode.getLine()).toUri().toString();
        if (ruleNode.getKind().contains("annotation")) {
            stringBuilder.append(
                    Link(String.join(CharUtils.dot, ruleNode.getNamespace(), ruleNode.getClasstype()), position)
            );
        } else {
            stringBuilder.append(
                    Link(String.join(CharUtils.dot, ruleNode.getNamespace(), ruleNode.getClasstype(), ruleNode.getMethod()), position));
        }
        stringBuilder.append(CharUtils.LF);
        stringBuilder.append(CodeBlock(ruleNode.getMethodcode(), ruleNode.getCode()));
        stringBuilder.append(newline);
    }

    private static String Link(String text, String url) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CharUtils.leftsquarebracket).append(text).append(CharUtils.rightsquarebracket)
                .append(CharUtils.leftbracket).append(url).append(CharUtils.rightbracket);
        return stringBuilder.toString();
    }

    private static String CodeBlock(String code) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("`").append(code).append("`");
        return stringBuilder.toString();
    }

    private static String CodeBlock(String code, String marktext) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<pre><code>").append(hightlight(marktext, code)).append("</pre></code>");
        return stringBuilder.toString();
    }

    private static String hightlight(String marktext, String code) {
        return code.replace(marktext, "<mark>" + marktext + "</mark>");
    }
}