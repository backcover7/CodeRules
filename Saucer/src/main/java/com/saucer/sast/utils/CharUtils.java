package com.saucer.sast.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharUtils {
    public final static String dotRegex = "\\.";
    public final static String dot = ".";
    public final static String slash = "/";
    public final static String backslash = "\\";
    public final static String comma = ",";
    public final static String backtick = "`";

    public final static String carrot = "^";
    public final static String star = "*";
    public final static String dollarRegex = "\\$";
    public final static String dollar = "$";
    public final static String colon = ":";
    public final static String at = "@";
    public final static String semicolon = ";";
    public final static String dash = "-";
    public final static String vertical = "|";
    public final static String singleQuote = "'";
    public final static String doubleQuote = "\"";
    public final static String sharp = "#";
    public final static String underscore = "_";
    public final static String space = " ";
    public final static String leftbracket = "(";
    public final static String leftbracketRegex = "\\(";
    public final static String rightbracket = ")";
    public final static String leftsquarebracket = "[";
    public final static String rightsquarebracket = "]";
    public final static String CR = "\r";
    public final static String LF = "\n";
    public final static String empty = "";
    public final static String ClassExtension = ".class";
    public final static String JarExtension = ".jar";
    public final static String JavaExtension = ".java";
    public final static String CsvExtension = ".csv";
    public final static String TxtExtension = ".txt";
    public final static String MarkdownExtension = ".md";
    public final static String PdfExtension = ".pdf";
    public final static String HtmlExtension = ".html";
    public final static String SarifExtension = ".sarif";
    public final static String JAVA = "java";
    public final static String banner = " ________  ________  ___  ___  ________  _______   ________          ___       \n" +
            "|\\   ____\\|\\   __  \\|\\  \\|\\  \\|\\   ____\\|\\  ___ \\ |\\   __  \\        |\\  \\      \n" +
            "\\ \\  \\___|\\ \\  \\|\\  \\ \\  \\\\\\  \\ \\  \\___|\\ \\   __/|\\ \\  \\|\\  \\       \\ \\  \\     \n" +
            " \\ \\_____  \\ \\   __  \\ \\  \\\\\\  \\ \\  \\    \\ \\  \\_|/_\\ \\   _  _\\       \\ \\  \\    \n" +
            "  \\|____|\\  \\ \\  \\ \\  \\ \\  \\\\\\  \\ \\  \\____\\ \\  \\_|\\ \\ \\  \\\\  \\|       \\ \\__\\   \n" +
            "    ____\\_\\  \\ \\__\\ \\__\\ \\_______\\ \\_______\\ \\_______\\ \\__\\\\ _\\        \\|__|   \n" +
            "   |\\_________\\|__|\\|__|\\|_______|\\|_______|\\|_______|\\|__|\\|__|           ___ \n" +
            "   \\|_________|                                                           |\\__\\\n" +
            "                                                                          \\|__|\n" +
            "                                                                               ";

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

    public static String StringSubstitute(Map<String, String> map, String template) {
        StringSubstitutor stringSubstitutor = new StringSubstitutor(map);
        return stringSubstitutor.replace(template);
    }

    public static String replaceEmpty2Null(String str) {
        if (str.equals(empty)) {
            return null;
        } else {
            return str;
        }
    }

    public static String Object2Json(Object obj) {
        if (obj == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = CharUtils.empty;
        try {
            json = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Object Json2Object(String json, Class<?> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = new Object();
        try {
            obj = mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static boolean Integer2Boolean(int code) {
        return code != 0;
    }

    public static int Boolean2Integer(boolean flag) {
        if (!flag) {
            return 0;
        } else {
            return 1;
        }
    }

    public static Object DeepCopy(Object obj) {
        XStream xstream = new XStream();
        xstream.allowTypesByWildcard(new String[]{"com.contrastsecurity.sarif.*", "com.saucer.sast.lang.java.parser.nodes.*"});
        String xml = xstream.toXML(obj);
        return xstream.fromXML(xml);
    }
}
