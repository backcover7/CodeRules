package csv.resolver.taint;

import utils.FilesUtils;
import utils.StringsUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ParseNodes {
    private static final String CSVTYPE = "csvType";
    private static final String Semgrep_Java = Paths.get("../semgrep/java/collections").toAbsolutePath().normalize().toString();

    private static String RuleTemplatePrefix = "rules:\n" +
            "  - id: ${node} \n" +
            "    options:\n" +
            "      symbolic_propagation: true\n" +
            "    pattern-either:\n";

    private static String RuleTemplateSuffix =
            "    message: Semgrep found a match of method in ${node} csv\n" +
            "    languages: [java]\n" +
            "    severity: WARNING";

    void AssembleRule(String directory, String extension, String CsvType) throws Exception {
        ArrayList<String> filenames = FilesUtils.getExtensionFiles(directory, extension, false);
        for (String filename : filenames) {

            HashMap<String, String> NodeMap= new HashMap<>();
            NodeMap.put(CSVTYPE, CsvType);

            String YamlFileName = Paths.get(
                    Semgrep_Java,
                    CsvType,
                    Paths.get(filename).getFileName().toString().
                            replaceAll(StringsUtils.CsvExtension, StringsUtils.empty).
                            replaceAll(StringsUtils.TxtExtension, StringsUtils.empty)
                            + StringsUtils.YamlExtension).toString();

            FilesUtils.WriteFile(
                    YamlFileName,
                    new StringSubstitutor(NodeMap).replace(RuleTemplatePrefix),
                    false
            );

            filename = Paths.get(filename).toAbsolutePath().toString();

            BufferedReader lineReader = new BufferedReader(new FileReader(filename));
            String CSVline;

            HashSet<String> MethodSet = new HashSet<>();

            while ((CSVline = lineReader.readLine()) != null) {
                if (CSVline == null) {
                    break;
                }
                if (CSVline.isEmpty()) {
                    continue;
                }
                SemCSV semCSV = new SemCSV();
                String pattern = semCSV.SemorlizeCSV(CsvType, CSVline, MethodSet);
                if (pattern.equals(StringsUtils.empty)) {
                    continue;
                }
                FilesUtils.WriteFile(YamlFileName, pattern + StringsUtils.LF, true);
            }

            FilesUtils.WriteFile(
                    YamlFileName,
                    new StringSubstitutor(NodeMap).replace(RuleTemplateSuffix),
                    true
            );

            System.out.println("[+] Finish writing rules from " + CsvType + " - " + Paths.get(filename).getFileName().toString());
        }
    }
}
