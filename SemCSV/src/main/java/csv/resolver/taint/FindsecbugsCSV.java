package csv.resolver.taint;

import utils.CsvUtils;
import utils.FilesUtils;
import utils.StringsUtils;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class FindsecbugsCSV extends SemCSV {
    private final static String CSVDirectory = Paths.get(FilesUtils.taint_csv, "collections/find-sec-bugs/injection-sinks").toString();
    public final static String CsvType = "findsecbugs";

    private final static String InputOrOutput = "InputOrOutput";

    private final static String[] ClassBlackList = new String[]{ "logging", "log4j", "slf4j", "org.pmw.tinylog", "org.jboss.seam.log" };

    public HashMap<String, String> getCSVMap(String FindsecbugsCsv) {
//        https://github.com/find-sec-bugs/find-sec-bugs/blob/master/findsecbugs-plugin/src/main/java/com/h3xstream/findsecbugs/injection/SinksLoader.java#L65
        String[] split = FindsecbugsCsv.split(StringsUtils.colon);
        if (split.length != 2) {
            throw new IllegalArgumentException("There must be exactly one ':' in " + FindsecbugsCsv);
        }
        String stringArguments = split[1];
//        String[] stringArgumentsArray = stringArguments.split(",");
//        int length = stringArgumentsArray.length;
//        if (length == 0) {
//            throw new IllegalArgumentException("no injectable parameters specified in " + CSVline);
//        }
//
//        int[] injectableParameters = new int[length];
//        for (int i = 0; i < length; i++) {
//            try {
//                injectableParameters[i] = Integer.parseInt(stringArgumentsArray[i]);
//            } catch (NumberFormatException ex) {
//                throw new IllegalArgumentException("cannot parse " + stringArgumentsArray[i], ex);
//            }
//        }
        String fullMethodName = split[0].split(StringsUtils.leftbracketRegex)[0];
        String methodName = fullMethodName.split(StringsUtils.dotRegex)[1];
        String fullClassName = fullMethodName.split(StringsUtils.dotRegex)[0].replaceAll(StringsUtils.slash, StringsUtils.dot);

        HashMap<String, String> CSVMap = new HashMap<>();

        CSVMap.put(Namespace, fullClassName.substring(0, fullClassName.lastIndexOf(StringsUtils.dot)));
        CSVMap.put(Type, fullClassName.substring(fullClassName.lastIndexOf(StringsUtils.dot) + 1));
        CSVMap.put(Name, methodName);
        CSVMap.put(InputOrOutput, stringArguments);
        CSVMap.put(Kind, StringsUtils.empty);

        return CSVMap;
    }

    public String SemorlizeCSV(String FindsecbugsCsv, HashSet<String> MethodSet) throws Exception {
        if (FindsecbugsCsv == null || FindsecbugsCsv.trim().isEmpty() || FindsecbugsCsv.trim().startsWith("-")) {
            return StringsUtils.empty;
        }

        HashMap<String, String> FindsecbugsCSVMap = this.getCSVMap(FindsecbugsCsv);

        if (Arrays.stream(ClassBlackList).anyMatch(FindsecbugsCSVMap.get(Namespace)::contains)) {
            return StringsUtils.empty;
        }

        String calltype = this.getCallTypeColumn(FindsecbugsCSVMap);
        if (calltype.equals(StringsUtils.empty)) {
            return StringsUtils.empty;
        }

        return this.getPattern(FindsecbugsCSVMap, calltype, MethodSet);
    }

    public void WriteFindsecbugsYaml() throws Exception {
        ParseNodes parseNodes = new ParseNodes();
        parseNodes.AssembleRule(CSVDirectory, StringsUtils.TxtExtension, CsvType);
    }
}
