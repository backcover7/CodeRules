package csv.resolver.analyzer;

import utils.ClasspathUtils;

public class AnalyzeJDK {
    public static void getJDKCSV(boolean print) throws Exception {
        boolean JDK = true;
        AnalyzeSoot.getJarsCSV(ClasspathUtils.JDK, print, JDK);
    }
}