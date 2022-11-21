package lang.java.config;

import java.nio.file.Paths;

public class ClassPath {
    public final static String SRC_MAIN = Paths.get("src/main").toAbsolutePath().toString();
    public final static String SRC_MAIN_LIB = Paths.get(SRC_MAIN, "lib").toString();
    public final static String CURRENT_POM = Paths.get("pom.xml").toAbsolutePath().toString();

    public void DownloadPomDepdencies() {
        DownloadPomDepdencies(CURRENT_POM);
    }

    public void DownloadPomDepdencies(String pom) {
        DownloadPomDepdencies(pom, SRC_MAIN_LIB);
    }

    public void DownloadPomDepdencies(String POM, String directory) {
        String[] MvnCmd = new String[] {"mvn", "-f", POM, "dependency:copy-dependencies", "-DoutputDirectory=" + directory, "-DexcludeTransitive=true"};

        System.out.println("[+] Downloading dependencies from pom.xml ...");
        try {
            Process process = Runtime.getRuntime().exec(MvnCmd);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
