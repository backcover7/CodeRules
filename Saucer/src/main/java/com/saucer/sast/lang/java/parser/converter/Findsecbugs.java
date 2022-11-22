package com.saucer.sast.lang.java.parser.converter;

import com.saucer.sast.utils.CharUtils;
import com.saucer.sast.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.*;

public class Findsecbugs {
    private final String separator = CharUtils.colon;
    private final static String tag = "findsecbugs";
    public final static String csvDirectory = Paths.get(FileUtils.collections, tag, "injection-sinks").toString();

    private static String[] FilenameBlacklist = new String[] { "crlf-logs", "seam-el" };

    public void process() {
        ArrayList<String> CsvFiles = FileUtils.getExtensionFiles(csvDirectory, CharUtils.TxtExtension, true);
        for (String csvFile : CsvFiles) {
            HashSet<String> methodSet = new HashSet<>();

            BufferedReader lineReader;
            try {
                lineReader = new BufferedReader(new FileReader(csvFile));

                String csv;
                while ((csv = lineReader.readLine()) != null) {
                    csv = csv.trim();
                    if (csv.isEmpty() || csv.startsWith("-")) {
                        continue;
                    }
                    getFindsecbugsCsvArray(csv,
                            Paths.get(csvFile).getFileName().toString().replaceAll(CharUtils.TxtExtension, CharUtils.empty),
                            methodSet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            methodSet.forEach(
                    (node) -> FileUtils.WriteFileLn(FileUtils.sinks, node, true)
            );
        }
    }

    private void getFindsecbugsCsvArray(String csv, String kind, HashSet<String> methodSet) {
//        https://github.com/find-sec-bugs/find-sec-bugs/blob/master/findsecbugs-plugin/src/main/java/com/h3xstream/findsecbugs/injection/SinksLoader.java#L65
        String[] split = csv.split(separator);
        if (split.length != 2) {
            throw new IllegalArgumentException("There must be exactly one ':' in " + csv);
        }
        String stringArguments = split[1];

        String fullMethodName = split[0].split(CharUtils.leftbracketRegex)[0];
        String method = fullMethodName.split(CharUtils.dotRegex)[1];
        String fullClassName = fullMethodName.split(CharUtils.dotRegex)[0].replaceAll(CharUtils.slash, CharUtils.dot);
        String namespace = fullClassName.substring(0, fullClassName.lastIndexOf(CharUtils.dot));
        String classtype = fullClassName.substring(fullClassName.lastIndexOf(CharUtils.dot) + 1);

        if (!isIgnored(kind)) {
            MergetMethod(
                    new String[] {
                            namespace, classtype, method,
                            stringArguments,
                            kind
                    },
                    methodSet
            );
        }
    }


    private void MergetMethod(String[] csvArray, HashSet<String> MethodSet) {
        String unique = String.join(
                CharUtils.colon,
                csvArray[CSVDefinition.NAMESPACEINDEX],
                csvArray[CSVDefinition.CLASSTYPEINDEX],
                csvArray[CSVDefinition.METHODINDEX],
                csvArray[CSVDefinition.KINDINDEX]
        );

        if (csvArray[CSVDefinition.NAMESPACEINDEX].contains("(")) {
            System.out.println("a");
        }

        MethodSet.add(unique);
    }

    private boolean isIgnored(String kind) {
        return Arrays.asList(FilenameBlacklist).contains(kind);
    }

    private List<String> ResolveTaintedArgs(String TaintedArgs) {
        return Arrays.asList(TaintedArgs.split(CharUtils.colon));
    }
}
