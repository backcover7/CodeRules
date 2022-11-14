package utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class ClasspathUtils {
    /**
     *   Add the following directories to Library in Project Structure of IDEA
     *   JDK, Dependencies_LIB, TRANSITIVE_LIB, SECONDARY_TRANSITIVE_LIB, SRC_MAIN_LIB/DONT_DELETE
     */
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
            DownloadPOMDeps(ClasspathUtils.Current_POM, ClasspathUtils.Dependencies, false);
//            System.out.println("[+] Downloading all transitive dependencies ...");
//            DownloadPOMDeps(ClasspathUtils.Current_POM, ClasspathUtils.TRANSITIVE_LIB, false);
//
//            ArrayList<String> LibJarFiles = FilesUtils.getExtensionFiles(ClasspathUtils.Dependencies, StringsUtils.JarExtension, true);
//            ArrayList<String> TRANSITIVE_LIB = FilesUtils.getExtensionFiles(ClasspathUtils.TRANSITIVE_LIB, StringsUtils.JarExtension, true);
//
//            System.out.println("[+] Downloading all secondary transitive dependencies ...");
//            for (String jar : TRANSITIVE_LIB) {
//                if (!LibJarFiles.contains(jar)) {
//                    List<String> resources = FilesUtils.getResourcesFromJar(jar, StringsUtils.POM_XML, true);
//                    if (resources.size() == 0) {
//                        continue;
//                    }
//                    String TransitivePomPath = resources.get(0);
//                    try (JarFile jarFile = new JarFile(Paths.get(jar).toAbsolutePath().toString())){
//                        JarEntry entry = jarFile.getJarEntry(TransitivePomPath);
//                        InputStream inputStream = jarFile.getInputStream(entry);
//                        String CopyTransitivePomPath = Paths.get(ClasspathUtils.Dependency, StringsUtils.POM_XML).toString();
//                        File outputFile = new File(CopyTransitivePomPath);
//                        FileUtils.copyInputStreamToFile(inputStream, outputFile);
//                        ClasspathUtils.DownloadPOMDeps(CopyTransitivePomPath,
//                                Paths.get(ClasspathUtils.SECONDARY_TRANSITIVE_LIB).toAbsolutePath().toString(),
//                                true);
//                        FileUtils.delete(new File(CopyTransitivePomPath));
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
        }
    }
}