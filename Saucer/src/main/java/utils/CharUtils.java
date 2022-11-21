package utils;

import lang.java.parser.core.Node;
import scala.Char;

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
    public final static String semicolon = ";";
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

    public static void ReportNode(Node node) {
        if (node.getKind() != null) {
            System.out.println("[+] Found " + node.getKind() + " kind of " + node.getNodetype() + "!");
            if (node.getKind().contains("annotation")) {
                System.out.println("    " +
                        String.join(CharUtils.dot, node.getNamespace(), node.getClasstype() +
                        " in the position " + node.getPosition() ));
            } else {
                System.out.println("    " +
                        String.join(CharUtils.dot, node.getNamespace(), node.getClasstype(), node.getMethod() +
                        " in the position " + node.getPosition() ));
            }
        }
    }
}
