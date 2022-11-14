package utils;

import config.DbConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringJoiner;

public class RuleUtils {
    public static final String[] SemgrepCLI = new String[]{"semgrep", "scan", "-f"};
    public static final String EnableFixFlag = "--autofix";
    public static final String DisableAutoFix = "--no-autofix";

    public static final String EMACS = "--emacs";     // FilePath:LineNumber:Index:Severity(RuleName):MatchCode:Message
    public static final String FilePath = "FilePath";
    public static final String LineNumber = "LineNumber";
    public static final String Index = "Index";
    public static final String Severity = "Severity";
    public static final String MatchCode = "MatchCode";
    public static final String Message = "Message";

    public static final String SEMGREP_JAVA_OPTIMIZATION = "../semgrep/java/Optimization";
    public static final String import_star_stmt = Paths.get(SEMGREP_JAVA_OPTIMIZATION, "import_star_stmt.yaml").toString();
    public static final String java_lang = Paths.get(SEMGREP_JAVA_OPTIMIZATION, "java_lang.yaml").toString();
    public static final String call_chain = Paths.get(SEMGREP_JAVA_OPTIMIZATION, "call_chain.yaml").toString();

    private static final String ClassInstanceRegex = "(?<=\\().+?(?= \\$.+?\\))";
    private static final String MethodInstanceRegex = "(?<=\\)\\.).+?(?=\\(.)";

    public static ArrayList<HashMap<String, String>> ProcessResult(String result) {
        String[] resultArray = result.split(StringsUtils.LF);
        ArrayList<HashMap<String, String>> resultList = new ArrayList<>();
        for (String line : resultArray) {
            if (line.equals(StringsUtils.empty)) {
                continue;
            }
            String[] lineArray = line.split(StringsUtils.colon);
            HashMap<String, String> SingleResMap = new HashMap<>();
            SingleResMap.put(FilePath, lineArray[0]);
            SingleResMap.put(LineNumber, lineArray[1]);
            SingleResMap.put(Index, lineArray[2]);
            SingleResMap.put(Severity, lineArray[3]);
            SingleResMap.put(MatchCode, lineArray[4]);
            SingleResMap.put(Message, lineArray[5]);
            resultList.add(SingleResMap);
        }
        return resultList;
    }

    public static ArrayList<HashMap<String, String>> ProcessEMACSResult(Process process) throws IOException {
        String stdout = ProcessUtils.StdoutProcess(process);    // null or result
        ArrayList<HashMap<String, String>> ResultList = new ArrayList<>();
        if (stdout != null) {
            ResultList = RuleUtils.ProcessResult(stdout);
        }
        return ResultList;
    }

    public static String ExtractNamespace(String MatchCode) {
        MatchCode = StringUtils.removeStart(MatchCode, StringsUtils.IMPORT + StringsUtils.space);
        return StringUtils.removeEnd(MatchCode, StringsUtils.dot + StringsUtils.star + StringsUtils.semicolon);
    }

    public static String QueryImportReplacement(String namespace) throws Exception {
        String sql = "SELECT * FROM " + DbUtils.ClassTablename + " WHERE namespace = ?";

        PreparedStatement statement = DbConfig.connection.prepareStatement(sql);
        statement.setString(1, namespace);
        ResultSet resultSet = statement.executeQuery();

        StringJoiner stringJoiner = new StringJoiner(StringsUtils.LF);
        while(resultSet.next()){
            if (resultSet.getString(TableUtils.CLASSTYPE).contains(StringsUtils.dollar)) {
                continue;
            }

            stringJoiner.add(
                    new StringJoiner(StringsUtils.dot)
                            .add(StringsUtils.IMPORT + StringsUtils.space + resultSet.getString(TableUtils.NAMESPACE))
                            .add(resultSet.getString(TableUtils.CLASSTYPE) + StringsUtils.semicolon)
                            .toString()
            );
        }
        statement.close();

        return stringJoiner.toString();
    }
}