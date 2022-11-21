package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SemgrepUtils {
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

    public static ArrayList<HashMap<String, String>> ProcessResult(String result) {
        String[] resultArray = result.split(CharUtils.LF);
        ArrayList<HashMap<String, String>> resultList = new ArrayList<>();
        for (String line : resultArray) {
            if (line.equals(CharUtils.empty)) {
                continue;
            }
            String[] lineArray = line.split(CharUtils.colon);
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

    /**
     *
     * @param process
     * @return ArrayList or null
     * @throws IOException
     */
    public static ArrayList<HashMap<String, String>> ProcessEMACSResult(Process process) throws IOException {
        String stdout = ProcessUtils.StdoutProcess(process);
        return ProcessResult(stdout);
    }
}
