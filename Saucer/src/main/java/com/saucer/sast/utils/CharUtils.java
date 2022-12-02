package com.saucer.sast.utils;

import com.saucer.sast.lang.java.parser.core.RuleNode;
import com.saucer.sast.lang.java.parser.dataflow.CallGraphNode;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;
import java.util.regex.Matcher;
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
    public final static String dash = "-";
    public final static String carrot = "^";
    public final static String singleQuote = "'";
    public final static String doubleQuote = "\"";
    public final static String sharp = "#";
    public final static String space = " ";
    public final static String leftbracket = "(";
    public final static String leftbracketRegex = "\\(";
    public final static String rightbracket = ")";
    public final static String leftsquarebracket = "[";
    public final static String rightsquarebracket = "]";
    public final static String LF = "\n";
    public final static String empty = "";
    public final static String ClassExtension = ".class";
    public final static String JarExtension = ".jar";
    public final static String JavaExtension = ".java";
    public final static String CsvExtension = ".csv";
    public final static String TxtExtension = ".txt";
    public final static String MarkdownExtension = ".md";
    public final static String PdfExtension = ".pdf";
    public final static String JAVA = "java";

    public static boolean RegexMatch(String regex, String string) {
        return RegexMatchLastOccurence(regex, string) != null;
    }

    public static String RegexMatchLastOccurence(String regex, String string) {
        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(string);
        String lastOccurence = null;
        while (matcher.find()) {
            lastOccurence = matcher.group();
        }
        return lastOccurence;
    }

    public static String StringSubsitute(Map<String, String> map, String template) {
        StringSubstitutor stringSubstitutor = new StringSubstitutor(map);
        return stringSubstitutor.replace(template);
    }

    public static String SimplifySourceCode(String code, String edgeType) {
        if (edgeType.equals(CallGraphNode.SourceFlowType) || edgeType.equals(CallGraphNode.SinkNodeType)){

        }
        return code;
    }
}
