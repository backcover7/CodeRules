package parser.converter;

import utils.CharUtils;
import utils.FileUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class CodeQL {
    private final static int CODEQL_NAMESPACEINDEX = 0;
    private final static int CODEQL_CLASSTYPEINDEX = 1;
    private final static int CODEQL_METHODINDEX = 3;
    private final static int CODEQL_TAINTEDARGSINDEX = 6;
    private final static int CODEQL_KINDINDEX = 7;

    private final String separator = CharUtils.semicolon;
    private final String tag = "codeql";
    public final String csvDirectory = Paths.get(FileUtils.collections, this.tag).toString();

    public static String[] KindBlackList = new String[]{"logging"};

    public void process() {
        ArrayList<String> CsvFiles = FileUtils.getExtensionFiles(csvDirectory, CharUtils.CsvExtension, false);
        for (String csvFile : CsvFiles) {
            String WriteTarget;
            if (Paths.get(csvFile).getFileName().toString().contains("source")) {
                WriteTarget = FileUtils.sources;
            } else if (Paths.get(csvFile).getFileName().toString().contains("sink")) {
                WriteTarget = FileUtils.sinks;
            } else {
                // TODO: else summary or negative-summary?
                continue;
            }

            HashSet<String> methodSet = new HashSet<>();

            BufferedReader lineReader;
            try {
                lineReader = new BufferedReader(new FileReader(csvFile));

                String csv;
                while ((csv = lineReader.readLine()) != null) {
                    if (csv.isEmpty()) {
                        continue;
                    }
                    String[] csvArray = csv.split(separator);
                    if (!isIgnored(csvArray)) {
                        MergetMethod(csvArray, methodSet);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            methodSet.forEach(
                    (method) -> FileUtils.WriteFileLn(WriteTarget, method, true)
            );
        }
    }

    private void MergetMethod(String[] csvArray, HashSet<String> MethodMap) {
        if (csvArray[CODEQL_CLASSTYPEINDEX].equals(csvArray[CODEQL_METHODINDEX])) {
            csvArray[CODEQL_METHODINDEX] = "<init>";
        }

        String unique = String.join(
                CharUtils.colon,
                csvArray[CODEQL_NAMESPACEINDEX],
                csvArray[CODEQL_CLASSTYPEINDEX],
                csvArray[CODEQL_METHODINDEX],
                csvArray[CODEQL_KINDINDEX]
        );

        MethodMap.add(unique);
    }

    private boolean isIgnored(String[] csvArray) {
        return Arrays.asList(KindBlackList).contains(csvArray[CODEQL_KINDINDEX]);
    }

    private ArrayList<Integer> ResolveTaintedArgs(String TaintedArgs) {
        ArrayList<Integer> ResolvedArgs = new ArrayList<>();
        // TODO
        return ResolvedArgs;
    }

    public static void main(String[] args) {
        new CodeQL().process();
    }
}
