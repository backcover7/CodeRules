package csv.resolver.analyzer;

import utils.ClasspathUtils;
import utils.CsvUtils;
import utils.FilesUtils;
import utils.StringsUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class AnalyzeJarFile {
    public static void getJarCSV(String jarPath, boolean print, boolean JDK) throws Exception {
        Path path = Paths.get(jarPath).getFileName();
        String filename = path.toString().replaceAll(StringsUtils.JarExtension, StringsUtils.CsvExtension);
        List<String> classNames = FilesUtils.getResourcesFromJar(jarPath, StringsUtils.ClassExtension, false);
        String classCSV = StringsUtils.empty;        // Class CSVs

        String methodFolder = FilesUtils.getFolder(JDK, false);

        if (print || FilesUtils.FileExistingPrompt(methodFolder, filename, StringsUtils.MethodCSV)) {
            for (String className : classNames) {
                if (!className.contains(StringsUtils.dot)) {
                    continue;
                }
                int lastIndex = className.lastIndexOf(StringsUtils.dot);
                String beginString = className.substring(0, lastIndex);
                String endString = className.substring(lastIndex + 1);
                classCSV += beginString + StringsUtils.colon + endString + StringsUtils.LF;

                String methodCSV = CsvUtils.getMethodCSVFromRemoteClass(className, JDK);
                FilesUtils.PrintOrWriteMethodCSV(methodCSV, print, filename, JDK);
            }
            System.out.println("[+] Method CSV of " + path + " is Done!");
        }

        String clazzFolder = FilesUtils.getFolder(JDK, true);
        if (print || FilesUtils.FileExistingPrompt(clazzFolder, filename, StringsUtils.ClassCSV)) {
            FilesUtils.PrintOrWriteClassCSV(classCSV, print, filename, JDK);
            System.out.println("[+] Class CSV of " + path + " is Done!");
        }
    }

    public static void getJarsCSV(String path, boolean print, boolean JDK) {
        try {
            // Directory containing several jars
            ArrayList<String> jarPaths = new ArrayList<>(FilesUtils.getExtensionFiles(path, StringsUtils.JarExtension, true));
            for (String jarPath : jarPaths) {
                getJarCSV(jarPath, print, JDK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
