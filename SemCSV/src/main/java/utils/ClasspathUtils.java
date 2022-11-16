package utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class ClasspathUtils {
    public final static String SRC_MAIN = Paths.get("src/main").toAbsolutePath().toString();
    public final static String Current_POM = Paths.get("pom.xml").toAbsolutePath().toString();
    public final static String SRC_MAIN_LIB = Paths.get(SRC_MAIN, "Lib").toString();
    public final static String JDK = Paths.get(SRC_MAIN_LIB, "JDK").toString();
    public final static String Dependencies = Paths.get(SRC_MAIN_LIB, "Dependencies").toString();

    public static HashSet<String> getSootClassPath(List<String> targets) {
        HashSet<String> classpath = new HashSet<>();
        classpath.add(Paths.get(JDK, "rt.jar").toString());
        classpath.add(Paths.get(JDK, "jce.jar").toString());
        classpath.add(Paths.get(JDK, "nashorn.jar").toString());
        classpath.addAll(targets);
        return classpath;
    }

    public static void DownloadPOMDeps(String POM, String directory, boolean excludeTransitive) throws IOException, InterruptedException {
        String MvnCmd = "mvn -f " + POM + " dependency:copy-dependencies -DoutputDirectory=" + directory;
        if (excludeTransitive) {
            MvnCmd = MvnCmd + " -DexcludeTransitive=true";
        }
        Process process = Runtime.getRuntime().exec(MvnCmd);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void DownloadJars(boolean MavenFlag) throws Exception {
        // Download JDK Jars (Has already downloaded all jars of JDK 8u211)
//        System.out.println("[+] Downloading JDK jars to classpath ...");
//        JDKUtils.CopyJDKJars();

        // Download Maven Jars
        if (MavenFlag) {
            System.out.println("[+] Downloading all direct dependencies ...");
            DownloadPOMDeps(Current_POM, Dependencies, false);
        }
    }
}