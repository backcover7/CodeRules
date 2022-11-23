package com.saucer.sast.utils;

import com.saucer.sast.lang.java.parser.core.Node;

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

    public static void FlagBug(Node node, String code) {
        System.out.println("[+] Found " + node.getKind() + " kind of " + node.getNodetype() + "!");
        if (node.getKind().contains("annotation")) {
            System.out.println("    " + String.join(CharUtils.dot, node.getNamespace(), node.getClasstype()));
        } else {
            System.out.println("    " +
                    String.join(CharUtils.dot, node.getNamespace(), node.getClasstype(), node.getMethod()));
        }
        System.out.println("    " + node.getPosition());
        System.out.println("    " + code);
    }
}
