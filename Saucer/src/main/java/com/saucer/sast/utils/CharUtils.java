package com.saucer.sast.utils;

import com.saucer.sast.lang.java.parser.core.RuleNode;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

public class CharUtils {
    public final static String dotRegex = "\\.";
    public final static String dot = ".";
    public final static String slash = "/";
    public final static String comma = ",";
    public final static String star = "*";
    public final static String dollarRegex = "\\$";
    public final static String dollar = "$";
    public final static String colon = ":";
    public final static String at = "@";
    public final static String semicolon = ";";
    public final static String carrot = "^";
    public final static String singleQuote = "'";
    public final static String doubleQuote = "\"";
    public final static String sharp = "#";
    public final static String space = " ";
    public final static String leftbracket = "(";
    public final static String leftbracketRegex = "\\(";
    public final static String rightbracket = ")";
    public final static String LF = "\n";
    public final static String empty = "";
    public final static String ClassExtension = ".class";
    public final static String JarExtension = ".jar";
    public final static String JavaExtension = ".java";
    public final static String CsvExtension = ".csv";
    public final static String TxtExtension = ".txt";

    public static boolean RegexMatch(String regex, String string) {
        return Pattern.compile(regex, Pattern.MULTILINE).matcher(string).find();
    }

    public static void ReportDangerousNode(RuleNode ruleNode) {
        System.out.println("[+] Found " + ruleNode.getKind() + " kind of " + ruleNode.getNodetype() + "!");
        if (ruleNode.getKind().contains("annotation")) {
            System.out.println("    " + String.join(CharUtils.dot, ruleNode.getNamespace(), ruleNode.getClasstype()));
        } else {
            System.out.println("    " +
                    String.join(CharUtils.dot, ruleNode.getNamespace(), ruleNode.getClasstype(), ruleNode.getMethod()));
        }
        System.out.println("    " + ruleNode.getFile() + "#" + ruleNode.getLine());
        System.out.println("    " + ruleNode.getCode());
    }

    public static String FormatChainNode(HashMap<String, String> invocation) {
        if (invocation.get(DbUtils.SUCCMETHODNAME) == null) {
            // Annotation
            return invocation.get(DbUtils.SUCCNAMESPACE) + CharUtils.dot +
                    invocation.get(DbUtils.SUCCCLASSTYPE) + CharUtils.comma + CharUtils.space +
                    invocation.get(DbUtils.FILEPATH) + CharUtils.sharp + invocation.get(DbUtils.SUCCLINENUM) +
                    CharUtils.comma + CharUtils.space +
                    invocation.get(DbUtils.SUCCCODE);
        } else {
            // Invocation
            return invocation.get(DbUtils.SUCCNAMESPACE) + CharUtils.dot +
                    invocation.get(DbUtils.SUCCCLASSTYPE) + CharUtils.dot +
                    invocation.get(DbUtils.SUCCMETHODNAME) + CharUtils.comma + CharUtils.space +
                    invocation.get(DbUtils.FILEPATH) + CharUtils.sharp + invocation.get(DbUtils.SUCCLINENUM) +
                    CharUtils.comma + CharUtils.space +
                    invocation.get(DbUtils.SUCCCODE);
        }
    }

    public static void ReportTaintedFlow(LinkedList<String> taintedFlow) {
        System.out.println("[+] Found potential exploitable tainted flow: ");
        int index = 1;
        for (String node : taintedFlow) {
            System.out.println("    " + index + CharUtils.dot + CharUtils.space + node);
            index++;
        }
    }

    public static String StringSubsitute(Map<String, String> map, String template) {
        StringSubstitutor stringSubstitutor = new StringSubstitutor(map);
        return stringSubstitutor.replace(template);
    }
}
