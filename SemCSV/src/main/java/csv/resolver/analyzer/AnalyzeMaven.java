package csv.resolver.analyzer;

import utils.ClasspathUtils;

public class AnalyzeMaven {         // Method CSVs
    public static void getMavenCSV(boolean print) {
        try {
            AnalyzeSoot.getJarsCSV(ClasspathUtils.Dependencies, print, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
