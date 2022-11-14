package csv.resolver.taint;

import utils.CsvUtils;
import utils.FilesUtils;
import utils.StringsUtils;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class CodeQLCSV extends SemCSV {
    private final static String CSVDirectory = Paths.get(FilesUtils.taint_csv, "collections/codeql").toString();
    public final static String CsvType = "codeql";

    private final static String Subtypes = "subtypes";
    private final static String Signature = "signature";
    private final static String Ext = "ext";
    private final static String InputOrOutput = "InputOrOutput";
    private final static String Provenance = "provenance";

    public static String[] KindBlackList = new String[]{
            "logging", "intent-start", "pending-intent-sent", "contentprovider", "android-external-storage-dir", "android-widget" };

    private final static int NamespaceIndex = 0;
    private final static int TypeIndex = 1;
    private final static int SubtypesIndex = 2;
    private final static int NameIndex = 3;
    private final static int SignatureIndex = 4;
    private final static int ExtIndex = 5;
    private final static int InputOrOutputIndex = 6;
    private final static int KindIndex = 7;
    private final static int ProvenanceIndex = 8;

    public HashMap<String, String> getCSVMap(String CodeQLCsv) {
        HashMap<String, String> CSVMap = new HashMap<>();

        String[] CSVArray = CodeQLCsv.split(StringsUtils.semicolon);
        CSVMap.put(Namespace, CSVArray[NamespaceIndex]);
        CSVMap.put(Type, CSVArray[TypeIndex]);
        CSVMap.put(Subtypes, CSVArray[SubtypesIndex]);
        CSVMap.put(Name, CSVArray[NameIndex]);
        CSVMap.put(Signature, CSVArray[SignatureIndex]);
        CSVMap.put(Ext, CSVArray[ExtIndex]);
        CSVMap.put(InputOrOutput, CSVArray[InputOrOutputIndex]);
        CSVMap.put(Kind, CSVArray[KindIndex]);
        CSVMap.put(Provenance, CSVArray[ProvenanceIndex]);

        return CSVMap;
    }

    public String SemorlizeCSV(String CodeQLCsv, HashSet<String> MethodSet) throws Exception {
        HashMap<String, String> CodeQLCSVMap = this.getCSVMap(CodeQLCsv);

        if (Arrays.asList(KindBlackList).contains(CodeQLCSVMap.get(Kind)) ||
                CodeQLCSVMap.get(Namespace).contains("android")
        ) {
            return StringsUtils.empty;
        }

        String calltype = this.getCallTypeColumn(CodeQLCSVMap);
        if (calltype.equals(StringsUtils.empty)) {
            return StringsUtils.empty;
        }

        return this.getPattern(CodeQLCSVMap, calltype, MethodSet);
    }

    public void WriteCodeQLYaml() throws Exception {
        ParseNodes parseNodes = new ParseNodes();
        parseNodes.AssembleRule(CSVDirectory, StringsUtils.CsvExtension, CsvType);
    }
}