package utils;

import soot.SootClass;
import soot.Type;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringsUtils {
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
    public final static String leftbracketRegex = "\\(";
    public final static String rightbracket = ")";
    public final static String LF = "\n";
    public final static String empty = "";
    public final static String ClassExtension = ".class";
    public final static String JarExtension = ".jar";
    public final static String JavaExtension = ".java";
    public final static String CsvExtension = ".csv";
    public final static String TxtExtension = ".txt";
    public final static String DbExtension = ".db";
    public final static String YamlExtension = ".yaml";
    public final static String POM_XML = "pom.xml";
    public final static String ClassCSV = "Class CSV";
    public final static String MethodCSV = "Method CSV";
    public final static String IMPORT = "import";

    public static String RegexMatch(String regex, String string) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);

        if (matcher.find()) {
            return matcher.group(0);
        } else {
            return null;
        }
    }

    public static String resolveModifers(String modifiers) {
        return modifiers.replaceAll(space, comma);
    }

    public static String concatClassArray(Class<?>[] classes) {
        StringJoiner stringJoiner = new StringJoiner(comma);
        for (Class<?> clazz:classes) {
            stringJoiner.add(clazz.getName());
        }
        return stringJoiner.toString();
    }

    public static String concatObjectArray(Object[] classes) {
        StringJoiner stringJoiner = new StringJoiner(comma);
        for (Object obj : classes) {
            stringJoiner.add(obj.toString());
        }
        return stringJoiner.toString();
    }

    public static String concatSootTypeArray(List<Type> types) {
        StringJoiner stringJoiner = new StringJoiner(comma);
        for (Type type : types) {
            stringJoiner.add(type.toString());
        }
        return stringJoiner.toString();
    }

    public static String removePackageName(String packageName, String name) {
        return name.replaceAll(packageName + dot, empty);
    }

    public static String nullClass2Empty(Class<?> result) {
        if(result == null) {
            return empty;
        }
        return result.getName();
    }
}
